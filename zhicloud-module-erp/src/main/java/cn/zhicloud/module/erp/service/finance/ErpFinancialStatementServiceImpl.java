package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpBalanceSheetRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpCashFlowStatementRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpFinancialStatementItemVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement.ErpIncomeStatementRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpCashFlowDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpCashFlowMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.enums.finance.ErpCashFlowBizTypeEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountTypeEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ERP 单体财务报表 Service 实现类（P0-4）
 *
 * <p>基于已审核会计凭证（erp_gl_voucher_entry）按科目类型汇总，生成单体三大财务报表。
 * <ul>
 *   <li>资产负债表：资产 = 负债 + 权益</li>
 *   <li>利润表：收入 - 成本 - 费用 = 利润</li>
 *   <li>现金流量表：基于银行流水（erp_cash_flow），无流水时基于凭证中银行科目变动</li>
 * </ul>
 *
 * <p>账套视角（FMS/ERP 融合，见 {@link ErpFinancialStatementService}）：accountSetId 非空时
 * 按「账套 → 账簿 → 凭证」链路过滤（V193 同口径，凭证不落冗余列）。
 *
 * @author 智云
 */
@Service
@Validated
public class ErpFinancialStatementServiceImpl implements ErpFinancialStatementService {

    @Resource
    private ErpGlVoucherMapper voucherMapper;
    @Resource
    private ErpGlVoucherEntryMapper voucherEntryMapper;
    @Resource
    private ErpGlAccountMapper accountMapper;
    @Resource
    private ErpCashFlowMapper cashFlowMapper;
    @Resource
    private ErpAccountBookMapper accountBookMapper;

    @Override
    public ErpBalanceSheetRespVO generateBalanceSheet(Long accountSetId, LocalDate asOfDate) {
        // 1. 获取截至报表日期的已审核凭证（accountSetId 非空时按「账套 → 账簿 → 凭证」链路过滤）
        List<Long> voucherIds = getApprovedVoucherIds(accountSetId, null, asOfDate);
        // 2. 按科目汇总借贷金额
        Map<Long, AccountSummary> accountSummaries = aggregateEntriesByAccount(voucherIds);
        // 3. 加载科目类型映射
        Map<Long, ErpGlAccountDO> accountMap = loadAccountMap();

        ErpBalanceSheetRespVO respVO = new ErpBalanceSheetRespVO();
        respVO.setAsOfDate(asOfDate);
        List<ErpFinancialStatementItemVO> assetItems = new ArrayList<>();
        List<ErpFinancialStatementItemVO> liabilityItems = new ArrayList<>();
        List<ErpFinancialStatementItemVO> equityItems = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;
        // 本期损益未结转额：资产负债表只列资产/负债/权益三类，而损益类（收入 40/费用 50）
        // 科目在未做结转前不在权益里体现（§12.145：损益结转有意不生成凭证），
        // 于是只要有一张损益凭证过账，资产 ≠ 负债 + 权益。这里把累计净利润（收入净额 −
        // 费用净额，与利润表同口径）作为「本年利润/未分配利润」并入权益，令恒等式成立——
        // 这是把已算好的数字显式呈现，不发明 4103 结转凭证。
        BigDecimal currentPeriodProfit = BigDecimal.ZERO;

        for (Map.Entry<Long, AccountSummary> entry : accountSummaries.entrySet()) {
            ErpGlAccountDO account = accountMap.get(entry.getKey());
            if (account == null) {
                continue;
            }
            AccountSummary s = entry.getValue();
            BigDecimal balance;
            Integer type = account.getType();
            if (ErpGlAccountTypeEnum.ASSET.getType().equals(type)) {
                // 资产类：余额 = 借方 - 贷方
                balance = s.debit.subtract(s.credit);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    assetItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), balance));
                }
                totalAssets = totalAssets.add(balance);
            } else if (ErpGlAccountTypeEnum.LIABILITY.getType().equals(type)) {
                // 负债类：余额 = 贷方 - 借方
                balance = s.credit.subtract(s.debit);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    liabilityItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), balance));
                }
                totalLiabilities = totalLiabilities.add(balance);
            } else if (ErpGlAccountTypeEnum.EQUITY.getType().equals(type)) {
                // 权益类：余额 = 贷方 - 借方
                balance = s.credit.subtract(s.debit);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    equityItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), balance));
                }
                totalEquity = totalEquity.add(balance);
            } else if (ErpGlAccountTypeEnum.REVENUE.getType().equals(type)) {
                // 收入类贷方净额计入本期利润
                currentPeriodProfit = currentPeriodProfit.add(s.credit.subtract(s.debit));
            } else if (ErpGlAccountTypeEnum.EXPENSE.getType().equals(type)) {
                // 费用/成本类借方净额从本期利润扣减
                currentPeriodProfit = currentPeriodProfit.subtract(s.debit.subtract(s.credit));
            }
        }
        if (currentPeriodProfit.compareTo(BigDecimal.ZERO) != 0) {
            equityItems.add(new ErpFinancialStatementItemVO(null, "本年利润（未分配）", currentPeriodProfit));
            totalEquity = totalEquity.add(currentPeriodProfit);
        }
        respVO.setAssetItems(assetItems);
        respVO.setLiabilityItems(liabilityItems);
        respVO.setEquityItems(equityItems);
        respVO.setTotalAssets(totalAssets);
        respVO.setTotalLiabilities(totalLiabilities);
        respVO.setTotalEquity(totalEquity);
        return respVO;
    }

    @Override
    public ErpIncomeStatementRespVO generateIncomeStatement(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        // 1. 获取期间内已审核凭证（accountSetId 非空时按账簿链路过滤）
        List<Long> voucherIds = getApprovedVoucherIds(accountSetId, startDate, endDate);
        // 2. 按科目汇总
        Map<Long, AccountSummary> accountSummaries = aggregateEntriesByAccount(voucherIds);
        // 3. 科目类型映射
        Map<Long, ErpGlAccountDO> accountMap = loadAccountMap();

        ErpIncomeStatementRespVO respVO = new ErpIncomeStatementRespVO();
        respVO.setStartDate(startDate);
        respVO.setEndDate(endDate);
        List<ErpFinancialStatementItemVO> revenueItems = new ArrayList<>();
        List<ErpFinancialStatementItemVO> costItems = new ArrayList<>();
        List<ErpFinancialStatementItemVO> expenseItems = new ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Map.Entry<Long, AccountSummary> entry : accountSummaries.entrySet()) {
            ErpGlAccountDO account = accountMap.get(entry.getKey());
            if (account == null) {
                continue;
            }
            AccountSummary s = entry.getValue();
            Integer type = account.getType();
            if (ErpGlAccountTypeEnum.REVENUE.getType().equals(type)) {
                // 收入类：发生额 = 贷方 - 借方
                BigDecimal amount = s.credit.subtract(s.debit);
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    revenueItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), amount));
                }
                totalRevenue = totalRevenue.add(amount);
            } else if (ErpGlAccountTypeEnum.EXPENSE.getType().equals(type)) {
                // 费用/成本类：发生额 = 借方 - 贷方
                BigDecimal amount = s.debit.subtract(s.credit);
                if (amount.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                // 成本类科目编码以 5 开头（中国会计准则 5xxx 成本类），其余为费用
                if (account.getCode() != null && account.getCode().startsWith("5")) {
                    costItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), amount));
                    totalCost = totalCost.add(amount);
                } else {
                    expenseItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), amount));
                    totalExpense = totalExpense.add(amount);
                }
            }
        }
        respVO.setRevenueItems(revenueItems);
        respVO.setCostItems(costItems);
        respVO.setExpenseItems(expenseItems);
        respVO.setTotalRevenue(totalRevenue);
        respVO.setTotalCost(totalCost);
        respVO.setTotalExpense(totalExpense);
        respVO.setNetProfit(totalRevenue.subtract(totalCost).subtract(totalExpense));
        return respVO;
    }

    @Override
    public ErpCashFlowStatementRespVO generateCashFlowStatement(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        ErpCashFlowStatementRespVO respVO = new ErpCashFlowStatementRespVO();
        respVO.setStartDate(startDate);
        respVO.setEndDate(endDate);
        List<Long> accountBookIds = resolveAccountBookIds(accountSetId);

        // 1. 优先基于银行流水（erp_cash_flow）
        //    注意：erp_cash_flow 只到 bank_account_id，无账簿关联。指定账套时无法按账簿
        //    过滤流水，与既有 ERP 侧行为（租户全量流水）保持同一口径——按账套出现金流
        //    报表走下方凭证兜底分支（银行科目 1001/1002 变动经账簿链路过滤）。
        List<ErpCashFlowDO> cashFlowList = accountSetId == null
                ? cashFlowMapper.selectListByPeriod(startDate, endDate)
                : List.of();
        if (cashFlowList != null && !cashFlowList.isEmpty()) {
            respVO.setDataSource("CASH_FLOW");
            BigDecimal operating = BigDecimal.ZERO;
            BigDecimal investing = BigDecimal.ZERO;
            BigDecimal financing = BigDecimal.ZERO;
            List<ErpFinancialStatementItemVO> operatingItems = new ArrayList<>();
            List<ErpFinancialStatementItemVO> investingItems = new ArrayList<>();
            List<ErpFinancialStatementItemVO> financingItems = new ArrayList<>();
            for (ErpCashFlowDO cf : cashFlowList) {
                BigDecimal amount = cf.getAmount() == null ? BigDecimal.ZERO : cf.getAmount();
                // 收款为流入（+），付款为流出（-）
                BigDecimal signed = ErpCashFlowBizTypeEnum.RECEIPT.getType().equals(cf.getBizType())
                        ? amount : amount.negate();
                String itemName = (cf.getBizOrderType() == null ? "现金流" : cf.getBizOrderType())
                        + " " + (cf.getOccurDate() == null ? "" : cf.getOccurDate().toString());
                String activity = classifyActivity(cf.getBizOrderType());
                ErpFinancialStatementItemVO item = new ErpFinancialStatementItemVO(
                        cf.getBizOrderType() == null ? "" : cf.getBizOrderType(), itemName, signed);
                if ("INVESTING".equals(activity)) {
                    investing = investing.add(signed);
                    investingItems.add(item);
                } else if ("FINANCING".equals(activity)) {
                    financing = financing.add(signed);
                    financingItems.add(item);
                } else {
                    operating = operating.add(signed);
                    operatingItems.add(item);
                }
            }
            respVO.setNetOperatingCashFlow(operating);
            respVO.setNetInvestingCashFlow(investing);
            respVO.setNetFinancingCashFlow(financing);
            respVO.setNetCashFlow(operating.add(investing).add(financing));
            respVO.setOperatingItems(operatingItems);
            respVO.setInvestingItems(investingItems);
            respVO.setFinancingItems(financingItems);
            return respVO;
        }

        // 2. 无现金流数据（或按账套视角，见上）时，基于凭证中银行科目（1001 库存现金 / 1002 银行存款）变动
        respVO.setDataSource("VOUCHER");
        List<Long> voucherIds = getApprovedVoucherIds(accountSetId, startDate, endDate);
        Map<Long, AccountSummary> accountSummaries = aggregateEntriesByAccount(voucherIds);
        Map<Long, ErpGlAccountDO> accountMap = loadAccountMap();
        BigDecimal netChange = BigDecimal.ZERO;
        List<ErpFinancialStatementItemVO> operatingItems = new ArrayList<>();
        for (Map.Entry<Long, AccountSummary> entry : accountSummaries.entrySet()) {
            ErpGlAccountDO account = accountMap.get(entry.getKey());
            if (account == null || account.getCode() == null) {
                continue;
            }
            // 仅汇总现金及银行存款科目
            if (account.getCode().startsWith("1001") || account.getCode().startsWith("1002")) {
                AccountSummary s = entry.getValue();
                BigDecimal change = s.debit.subtract(s.credit);
                if (change.compareTo(BigDecimal.ZERO) != 0) {
                    operatingItems.add(new ErpFinancialStatementItemVO(account.getCode(), account.getName(), change));
                }
                netChange = netChange.add(change);
            }
        }
        respVO.setNetOperatingCashFlow(netChange);
        respVO.setNetInvestingCashFlow(BigDecimal.ZERO);
        respVO.setNetFinancingCashFlow(BigDecimal.ZERO);
        respVO.setNetCashFlow(netChange);
        respVO.setOperatingItems(operatingItems);
        respVO.setInvestingItems(new ArrayList<>());
        respVO.setFinancingItems(new ArrayList<>());
        return respVO;
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 获取已审核凭证编号列表
     *
     * @param accountSetId FMS 账套编号（可空；null 为租户全量）
     * @param startDate 起始日期（资产负债表传 null）
     * @param endDate   结束日期
     */
    private List<Long> getApprovedVoucherIds(Long accountSetId, LocalDate startDate, LocalDate endDate) {
        List<Long> accountBookIds = resolveAccountBookIds(accountSetId);
        // 指定账套但名下无账簿：无凭证可汇总，直接返回空（避免 IN(空集合) 语义歧义）
        if (accountBookIds != null && accountBookIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapperX<ErpGlVoucherDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpGlVoucherDO::getStatus, ErpGlVoucherStatusEnum.APPROVED.getStatus());
        wrapper.inIfPresent(ErpGlVoucherDO::getAccountBookId, accountBookIds);
        wrapper.le(ErpGlVoucherDO::getVoucherDate, endDate);
        if (startDate != null) {
            wrapper.ge(ErpGlVoucherDO::getVoucherDate, startDate);
        }
        List<ErpGlVoucherDO> vouchers = voucherMapper.selectList(wrapper);
        return vouchers.stream().map(ErpGlVoucherDO::getId).collect(Collectors.toList());
    }

    /**
     * 解析账套名下的账簿编号集合（V193 同口径：凭证只到 account_book_id，
     * 账簿再挂账套，按账套筛一律先解析账簿集合再 IN 查询，不在凭证落冗余列）
     *
     * @param accountSetId FMS 账套编号（可空）
     * @return null 表示不按账簿过滤（租户全量）；空列表表示账套下无账簿
     */
    private List<Long> resolveAccountBookIds(Long accountSetId) {
        if (accountSetId == null) {
            return null;
        }
        return accountBookMapper.selectListByAccountSetId(accountSetId).stream()
                .map(ErpAccountBookDO::getId).collect(Collectors.toList());
    }

    /**
     * 按科目汇总借贷金额
     */
    private Map<Long, AccountSummary> aggregateEntriesByAccount(List<Long> voucherIds) {
        Map<Long, AccountSummary> map = new HashMap<>();
        if (voucherIds == null || voucherIds.isEmpty()) {
            return map;
        }
        List<ErpGlVoucherEntryDO> entries = voucherEntryMapper.selectListByVoucherIds(voucherIds);
        for (ErpGlVoucherEntryDO entry : entries) {
            AccountSummary s = map.computeIfAbsent(entry.getAccountId(), k -> new AccountSummary());
            s.debit = s.debit.add(entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount());
            s.credit = s.credit.add(entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount());
        }
        return map;
    }

    /**
     * 加载全部科目，构建 id -> DO 映射
     */
    private Map<Long, ErpGlAccountDO> loadAccountMap() {
        List<ErpGlAccountDO> accounts = accountMapper.selectList(null);
        return accounts.stream().collect(Collectors.toMap(ErpGlAccountDO::getId, a -> a, (a, b) -> a));
    }

    /**
     * 根据业务单据类型分类现金流活动
     */
    private String classifyActivity(String bizOrderType) {
        if (bizOrderType == null) {
            return "OPERATING";
        }
        String upper = bizOrderType.toUpperCase();
        if (upper.contains("FIXED_ASSET") || upper.contains("INVEST") || upper.contains("投资")
                || upper.contains("资产")) {
            return "INVESTING";
        }
        if (upper.contains("LOAN") || upper.contains("FINANCE") || upper.contains("筹资")
                || upper.contains("借款") || upper.contains("融资")) {
            return "FINANCING";
        }
        return "OPERATING";
    }

    /**
     * 科目借贷汇总内部对象
     */
    private static class AccountSummary {
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal credit = BigDecimal.ZERO;
    }

}
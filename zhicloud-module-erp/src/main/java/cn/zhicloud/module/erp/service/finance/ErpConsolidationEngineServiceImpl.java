package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationEntryDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationScopeDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpConsolidationWorksheetDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpConsolidationEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpConsolidationWorksheetMapper;
import cn.zhicloud.module.erp.enums.finance.ErpConsolidationEliminationTypeEnum;
import cn.zhicloud.module.erp.enums.finance.ErpWorksheetStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 合并报表自动抵消引擎 Service 实现类（P1）
 *
 * <p>简化实现说明：
 * <ul>
 *   <li>本引擎基于已有的 {@link ErpConsolidationEntryDO}（合并抵消分录）数据做自动归集，
 *       生成抵消分录记录到 {@link ErpConsolidationWorksheetDO}（合并工作底稿），不直接生成 GL 凭证。</li>
 *   <li>合并资产负债表 / 利润表基于已审核的工作底稿金额按抵消类型聚合输出，
 *       详细科目级合并需依赖完整 GL 凭证服务（如后续扩展可直接接入）。</li>
 *   <li>本实现不修改 {@link ErpConsolidationEntryServiceImpl} 现有方法，仅作为新增引擎扩展。</li>
 * </ul>
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class ErpConsolidationEngineServiceImpl implements ErpConsolidationEngineService {

    /**
     * 默认毛利率（用于内部销售成本抵消的未实现利润计算）
     */
    private static final BigDecimal DEFAULT_GROSS_PROFIT_RATE = new BigDecimal("0.20");
    /**
     * 已审核状态
     */
    private static final Integer ENTRY_STATUS_APPROVED = 20;

    @Resource
    private ErpConsolidationScopeService consolidationScopeService;

    @Resource
    private ErpConsolidationEntryMapper consolidationEntryMapper;

    @Resource
    private ErpConsolidationWorksheetMapper consolidationWorksheetMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ErpConsolidationWorksheetDO> generateInvestmentEquityElimination(Long parentId, Long subId, String period) {
        // 校验合并范围存在
        ErpConsolidationScopeDO scope = validateScopeExists(parentId, subId);
        // 删除该母子公司在该周期下该类型的旧工作底稿
        consolidationWorksheetMapper.deleteByPeriodAndParentAndSubsidiary(period, parentId, subId);
        // 聚合已审核的投资权益抵消分录金额
        BigDecimal totalAmount = aggregateApprovedEntries(parentId, subId, period,
                ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType());
        // 按持股比例计算母公司应享有的权益份额
        BigDecimal parentShare = totalAmount.multiply(scope.getHoldingRatio());
        // 少数股东权益（差额）
        BigDecimal minorityInterest = totalAmount.subtract(parentShare);
        List<ErpConsolidationWorksheetDO> result = new ArrayList<>();
        // 借：子公司所有者权益（按持股比例）
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                parentShare, "投资权益抵消-借：子公司所有者权益（持股比例 " + scope.getHoldingRatio() + "）"));
        // 贷：长期股权投资
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                parentShare, "投资权益抵消-贷：长期股权投资"));
        // 差额：少数股东权益
        if (minorityInterest.compareTo(BigDecimal.ZERO) != 0) {
            result.add(buildWorksheet(period, parentId, subId,
                    ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                    minorityInterest, "投资权益抵消-少数股东权益"));
        }
        result.forEach(consolidationWorksheetMapper::insert);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ErpConsolidationWorksheetDO> generateIntercompanyArApElimination(Long parentId, Long subId, String period) {
        validateScopeExists(parentId, subId);
        consolidationWorksheetMapper.deleteByPeriodAndParentAndSubsidiary(period, parentId, subId);
        BigDecimal totalAmount = aggregateApprovedEntries(parentId, subId, period,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_AR_AP.getType());
        List<ErpConsolidationWorksheetDO> result = new ArrayList<>();
        // 借：内部应付（子公司对母公司）
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_AR_AP.getType(),
                totalAmount, "内部应收应付抵消-借：内部应付"));
        // 贷：内部应收（母公司对子公司）
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_AR_AP.getType(),
                totalAmount, "内部应收应付抵消-贷：内部应收"));
        result.forEach(consolidationWorksheetMapper::insert);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ErpConsolidationWorksheetDO> generateIntercompanySaleCogsElimination(Long parentId, Long subId, String period) {
        validateScopeExists(parentId, subId);
        consolidationWorksheetMapper.deleteByPeriodAndParentAndSubsidiary(period, parentId, subId);
        BigDecimal totalAmount = aggregateApprovedEntries(parentId, subId, period,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_SALE_COGS.getType());
        // 未实现利润 = 内部销售金额 × 毛利率
        BigDecimal unrealizedProfit = totalAmount.multiply(DEFAULT_GROSS_PROFIT_RATE);
        List<ErpConsolidationWorksheetDO> result = new ArrayList<>();
        // 借：销售收入（母公司对子公司销售）
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_SALE_COGS.getType(),
                totalAmount, "内部销售成本抵消-借：销售收入"));
        // 贷：销售成本（按毛利率计算未实现利润部分）
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_SALE_COGS.getType(),
                totalAmount.subtract(unrealizedProfit), "内部销售成本抵消-贷：销售成本"));
        // 贷：存货（未实现利润部分）
        if (unrealizedProfit.compareTo(BigDecimal.ZERO) != 0) {
            result.add(buildWorksheet(period, parentId, subId,
                    ErpConsolidationEliminationTypeEnum.INTERCOMPANY_SALE_COGS.getType(),
                    unrealizedProfit, "内部销售成本抵消-贷：存货（未实现利润）"));
        }
        result.forEach(consolidationWorksheetMapper::insert);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ErpConsolidationWorksheetDO> generateIntercompanyFaElimination(Long parentId, Long subId, String period) {
        validateScopeExists(parentId, subId);
        consolidationWorksheetMapper.deleteByPeriodAndParentAndSubsidiary(period, parentId, subId);
        BigDecimal totalAmount = aggregateApprovedEntries(parentId, subId, period,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_FA.getType());
        List<ErpConsolidationWorksheetDO> result = new ArrayList<>();
        // 借：固定资产原价（未实现利润部分）
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_FA.getType(),
                totalAmount, "内部固定资产抵消-借：固定资产原价（未实现利润部分）"));
        // 贷：累计折旧
        result.add(buildWorksheet(period, parentId, subId,
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_FA.getType(),
                totalAmount, "内部固定资产抵消-贷：累计折旧"));
        result.forEach(consolidationWorksheetMapper::insert);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ErpConsolidationWorksheetDO> generateAllEliminations(String period) {
        List<ErpConsolidationScopeDO> scopes = consolidationScopeService.getEnabledScopeList();
        if (scopes == null || scopes.isEmpty()) {
            throw exception(CONSOLIDATION_NO_SCOPE);
        }
        List<ErpConsolidationWorksheetDO> allWorksheets = new ArrayList<>();
        for (ErpConsolidationScopeDO scope : scopes) {
            Long parentId = scope.getParentCompanyId();
            Long subId = scope.getSubsidiaryCompanyId();
            // 依次生成 4 类抵消分录
            allWorksheets.addAll(generateInvestmentEquityElimination(parentId, subId, period));
            allWorksheets.addAll(generateIntercompanyArApElimination(parentId, subId, period));
            allWorksheets.addAll(generateIntercompanySaleCogsElimination(parentId, subId, period));
            allWorksheets.addAll(generateIntercompanyFaElimination(parentId, subId, period));
        }
        return allWorksheets;
    }

    @Override
    public Map<String, BigDecimal> generateConsolidatedBalanceSheet(String period) {
        // 资产负债表抵消类型：投资权益 + 内部应收应付 + 内部固定资产
        return aggregateWorksheetByEliminationTypes(period, new Integer[]{
                ErpConsolidationEliminationTypeEnum.INVESTMENT_EQUITY.getType(),
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_AR_AP.getType(),
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_FA.getType()
        });
    }

    @Override
    public Map<String, BigDecimal> generateConsolidatedIncomeStatement(String period) {
        // 利润表抵消类型：内部销售成本
        return aggregateWorksheetByEliminationTypes(period, new Integer[]{
                ErpConsolidationEliminationTypeEnum.INTERCOMPANY_SALE_COGS.getType()
        });
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 校验合并范围存在
     */
    private ErpConsolidationScopeDO validateScopeExists(Long parentId, Long subId) {
        List<ErpConsolidationScopeDO> scopes = consolidationScopeService.getEnabledScopeListByParent(parentId);
        if (scopes == null || scopes.isEmpty()) {
            throw exception(CONSOLIDATION_NO_SCOPE);
        }
        for (ErpConsolidationScopeDO scope : scopes) {
            if (Objects.equals(scope.getSubsidiaryCompanyId(), subId)) {
                return scope;
            }
        }
        throw exception(CONSOLIDATION_SCOPE_NOT_EXISTS);
    }

    /**
     * 聚合已审核的合并抵消分录金额
     *
     * <p>注：当前 ErpConsolidationEntryDO 没有 parentCompanyId / subsidiaryCompanyId 字段，
     * 简化实现按 periodCode + eliminationType 聚合（periodCode 与 consolidationPeriod 对应）。
     *
     * @param parentId       母公司编号（保留参数，便于后续扩展精确过滤）
     * @param subId          子公司编号（保留参数，便于后续扩展精确过滤）
     * @param period         合并周期（yyyyMM）
     * @param eliminationType 抵消类型
     * @return 聚合金额
     */
    private BigDecimal aggregateApprovedEntries(Long parentId, Long subId, String period, Integer eliminationType) {
        List<ErpConsolidationEntryDO> entries = consolidationEntryMapper.selectListByPeriodCode(period);
        if (entries == null || entries.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ErpConsolidationEntryDO entry : entries) {
            if (!Objects.equals(entry.getEliminationType(), eliminationType)) {
                continue;
            }
            // 仅聚合已审核的抵消分录
            if (!Objects.equals(entry.getStatus(), ENTRY_STATUS_APPROVED)) {
                continue;
            }
            if (entry.getEliminationAmount() != null) {
                total = total.add(entry.getEliminationAmount());
            }
        }
        return total;
    }

    /**
     * 构建工作底稿对象（不持久化）
     */
    private ErpConsolidationWorksheetDO buildWorksheet(String period, Long parentId, Long subId,
                                                       Integer eliminationType, BigDecimal amount, String description) {
        return ErpConsolidationWorksheetDO.builder()
                .consolidationPeriod(period)
                .parentCompanyId(parentId)
                .subsidiaryCompanyId(subId)
                .eliminationType(eliminationType)
                .eliminationAmount(amount == null ? BigDecimal.ZERO : amount)
                .description(description)
                .status(ErpWorksheetStatusEnum.PENDING.getStatus())
                .sort(0)
                .remark("自动抵消引擎生成")
                .build();
    }

    /**
     * 按抵消类型聚合已审核工作底稿金额
     *
     * @param period           合并周期
     * @param eliminationTypes 抵消类型数组
     * @return key=抵消类型名称，value=聚合金额
     */
    private Map<String, BigDecimal> aggregateWorksheetByEliminationTypes(String period, Integer[] eliminationTypes) {
        Map<String, BigDecimal> result = new HashMap<>();
        List<ErpConsolidationWorksheetDO> worksheets = consolidationWorksheetMapper.selectListByPeriod(period);
        if (worksheets == null || worksheets.isEmpty()) {
            return result;
        }
        for (Integer type : eliminationTypes) {
            BigDecimal total = BigDecimal.ZERO;
            for (ErpConsolidationWorksheetDO ws : worksheets) {
                if (!Objects.equals(ws.getEliminationType(), type)) {
                    continue;
                }
                // 仅聚合已审核的工作底稿
                if (!Objects.equals(ws.getStatus(), ErpWorksheetStatusEnum.APPROVED.getStatus())) {
                    continue;
                }
                if (ws.getEliminationAmount() != null) {
                    total = total.add(ws.getEliminationAmount());
                }
            }
            // 用抵消类型名称作为 key
            String typeName = "";
            for (ErpConsolidationEliminationTypeEnum e : ErpConsolidationEliminationTypeEnum.values()) {
                if (Objects.equals(e.getType(), type)) {
                    typeName = e.getName();
                    break;
                }
            }
            result.put(typeName, total);
        }
        return result;
    }

}

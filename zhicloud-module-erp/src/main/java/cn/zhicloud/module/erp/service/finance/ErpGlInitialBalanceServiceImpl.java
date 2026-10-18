package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceSaveReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpTrialBalanceRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlInitialBalanceMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountBalanceDirectionEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountTypeEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_IMPORT_AMOUNT_INVALID;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_EXISTS;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_LEAF;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_LOCKED;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_PROFIT_LOSS_YEAR_OPENING;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_SUBJECT_DUPLICATE;

/**
 * ERP 期初余额 Service 实现类（账套口径，C6③ / ADR 265）
 *
 * <p>设计要点：
 * <ul>
 *   <li>单一真相：数据落本表（账套口径）；erp_gl_account 的租户口径余额列两轨并存、暂不互写；</li>
 *   <li>护栏：账套经账簿链存在已审核凭证时禁止修改（服务端强校验）；</li>
 *   <li>科目口径：本租户科目（与三表 loadAccountMap 口径一致，全局种子科目不可见）；</li>
 *   <li>零行保留：upsert 无删除路径；「清零」以显式 0 表达。</li>
 * </ul>
 *
 * @author 智云
 */
@Slf4j
@Service
@Validated
public class ErpGlInitialBalanceServiceImpl implements ErpGlInitialBalanceService {

    /**
     * 试算平衡容差（与旧 FMS 前端 0.005 口径一致，ADR 265 §3 裁定 5）
     */
    private static final BigDecimal BALANCE_TOLERANCE = new BigDecimal("0.005");

    @Resource
    private ErpGlInitialBalanceMapper initialBalanceMapper;
    @Resource
    private ErpGlAccountMapper glAccountMapper;
    @Resource
    private ErpGlAccountService glAccountService;
    @Resource
    private ErpAccountBookMapper accountBookMapper;
    @Resource
    private ErpGlVoucherMapper glVoucherMapper;

    @Override
    public List<ErpGlInitialBalanceRespVO> getInitialBalanceList(Long accountSetId, Integer subjectType) {
        List<ErpGlAccountDO> subjects = glAccountMapper.selectListByType(subjectType);
        Map<Long, ErpGlInitialBalanceDO> balanceMap = mapByAccountId(
                initialBalanceMapper.selectListByAccountSetId(accountSetId));
        return subjects.stream()
                .map(subject -> buildRespVO(subject, balanceMap.get(subject.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveInitialBalances(ErpGlInitialBalanceSaveReqVO saveReqVO) {
        Long accountSetId = saveReqVO.getAccountSetId();
        List<ErpGlInitialBalanceSaveReqVO.Balance> balances = saveReqVO.getBalances();
        // 1. 护栏：账套经账簿链存在已审核凭证时，期初余额锁定（ADR 265 §3 裁定 6）
        validateNoApprovedVoucher(accountSetId);
        // 2. 科目重复校验（同一请求内不得重复）
        Set<Long> subjectIds = new HashSet<>();
        for (ErpGlInitialBalanceSaveReqVO.Balance item : balances) {
            if (!subjectIds.add(item.getSubjectId())) {
                throw exception(GL_INITIAL_BALANCE_SUBJECT_DUPLICATE, item.getSubjectId());
            }
        }
        // 3. 科目存在 / 末级 + 损益类年初余额校验
        for (ErpGlInitialBalanceSaveReqVO.Balance item : balances) {
            ErpGlAccountDO subject = glAccountService.validateGlAccountIsLeaf(item.getSubjectId());
            validateProfitLossYearOpening(subject, item);
        }
        // 4. 差分 upsert（唯一键 (tenant, 账套, 科目) 导幂等；零行保留、无删除路径）
        Map<Long, ErpGlInitialBalanceDO> existingMap = mapByAccountId(
                initialBalanceMapper.selectListByAccountSetId(accountSetId));
        for (ErpGlInitialBalanceSaveReqVO.Balance item : balances) {
            ErpGlInitialBalanceDO existing = existingMap.get(item.getSubjectId());
            if (existing == null) {
                initialBalanceMapper.insert(ErpGlInitialBalanceDO.builder()
                        .accountSetId(accountSetId)
                        .accountId(item.getSubjectId())
                        .openingAmount(item.getOpeningAmount())
                        .yearDebitAmount(item.getYearDebitAmount())
                        .yearCreditAmount(item.getYearCreditAmount())
                        .profitLossAmount(item.getProfitLossAmount())
                        .build());
            } else {
                existing.setOpeningAmount(item.getOpeningAmount());
                existing.setYearDebitAmount(item.getYearDebitAmount());
                existing.setYearCreditAmount(item.getYearCreditAmount());
                existing.setProfitLossAmount(item.getProfitLossAmount());
                initialBalanceMapper.updateById(existing);
            }
        }
    }

    @Override
    public ErpTrialBalanceRespVO getTrialBalance(Long accountSetId) {
        List<ErpGlInitialBalanceDO> rows = initialBalanceMapper.selectListByAccountSetId(accountSetId);
        Map<Long, ErpGlAccountDO> subjectMap = loadSubjectMap(rows);
        BigDecimal openingDebit = BigDecimal.ZERO;
        BigDecimal openingCredit = BigDecimal.ZERO;
        BigDecimal yearDebit = BigDecimal.ZERO;
        BigDecimal yearCredit = BigDecimal.ZERO;
        for (ErpGlInitialBalanceDO row : rows) {
            ErpGlAccountDO subject = subjectMap.get(row.getAccountId());
            Integer direction = subject == null ? ErpGlAccountBalanceDirectionEnum.DEBIT.getDirection()
                    : subject.getBalanceDirection();
            BigDecimal opening = nvl(row.getOpeningAmount());
            // 借向计借方；贷向计贷方；双向按符号拆分（负数等额计贷方，两列均保持非负）
            if (ErpGlAccountBalanceDirectionEnum.DEBIT.getDirection().equals(direction)) {
                openingDebit = openingDebit.add(opening);
            } else if (ErpGlAccountBalanceDirectionEnum.CREDIT.getDirection().equals(direction)) {
                openingCredit = openingCredit.add(opening);
            } else if (opening.signum() < 0) {
                openingCredit = openingCredit.add(opening.negate());
            } else {
                openingDebit = openingDebit.add(opening);
            }
            yearDebit = yearDebit.add(nvl(row.getYearDebitAmount()));
            yearCredit = yearCredit.add(nvl(row.getYearCreditAmount()));
        }
        ErpTrialBalanceRespVO respVO = new ErpTrialBalanceRespVO();
        respVO.setOpeningDebitAmount(openingDebit);
        respVO.setOpeningCreditAmount(openingCredit);
        respVO.setOpeningDifferenceAmount(openingDebit.subtract(openingCredit));
        respVO.setYearDebitAmount(yearDebit);
        respVO.setYearCreditAmount(yearCredit);
        respVO.setYearDifferenceAmount(yearDebit.subtract(yearCredit));
        respVO.setBalanced(respVO.getOpeningDifferenceAmount().abs().compareTo(BALANCE_TOLERANCE) < 0
                && respVO.getYearDifferenceAmount().abs().compareTo(BALANCE_TOLERANCE) < 0);
        return respVO;
    }

    @Override
    public List<ErpGlInitialBalanceExcelVO> getInitialBalanceExcelList(Long accountSetId) {
        List<ErpGlAccountDO> leaves = glAccountMapper.selectListByLeafOrderByCode();
        Map<Long, ErpGlInitialBalanceDO> balanceMap = mapByAccountId(
                initialBalanceMapper.selectListByAccountSetId(accountSetId));
        return leaves.stream().map(subject -> {
            ErpGlInitialBalanceDO balance = balanceMap.get(subject.getId());
            return ErpGlInitialBalanceExcelVO.builder()
                    .subjectCode(subject.getCode())
                    .subjectName(subject.getName())
                    .openingAmount(balance == null ? BigDecimal.ZERO : nvl(balance.getOpeningAmount()))
                    .yearDebitAmount(balance == null ? BigDecimal.ZERO : nvl(balance.getYearDebitAmount()))
                    .yearCreditAmount(balance == null ? BigDecimal.ZERO : nvl(balance.getYearCreditAmount()))
                    .profitLossAmount(balance == null ? BigDecimal.ZERO : nvl(balance.getProfitLossAmount()))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ErpGlInitialBalanceExcelVO> getImportTemplateList(Long accountSetId) {
        // accountSetId 目前不参与模板行构建（科目为租户级口径、余额列留空），保留参数以对齐端点契约
        return glAccountMapper.selectListByLeafOrderByCode().stream()
                .map(subject -> ErpGlInitialBalanceExcelVO.builder()
                        .subjectCode(subject.getCode())
                        .subjectName(subject.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer importInitialBalances(Long accountSetId, List<ErpGlInitialBalanceExcelVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        List<ErpGlInitialBalanceSaveReqVO.Balance> balances = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            ErpGlInitialBalanceExcelVO row = rows.get(i);
            int rowNo = i + 2; // Excel 数据行号：表头占第 1 行
            // 四金额全空 = 模板未动过的行，跳过（显式填 0 才会写入——「填 0 = 清零」）
            if (row.getOpeningAmount() == null && row.getYearDebitAmount() == null
                    && row.getYearCreditAmount() == null && row.getProfitLossAmount() == null) {
                continue;
            }
            String code = row.getSubjectCode() == null ? null : row.getSubjectCode().trim();
            if (code == null || code.isEmpty()) {
                throw exception(GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_EXISTS, rowNo,
                        String.valueOf(row.getSubjectCode()));
            }
            ErpGlAccountDO subject = glAccountMapper.selectByCode(code);
            if (subject == null) {
                throw exception(GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_EXISTS, rowNo, code);
            }
            if (!Boolean.TRUE.equals(subject.getIsLeaf())) {
                throw exception(GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_LEAF, rowNo, code);
            }
            ErpGlInitialBalanceSaveReqVO.Balance item = new ErpGlInitialBalanceSaveReqVO.Balance();
            item.setSubjectId(subject.getId());
            item.setOpeningAmount(requireNonNegative(row.getOpeningAmount(), rowNo));
            item.setYearDebitAmount(requireNonNegative(row.getYearDebitAmount(), rowNo));
            item.setYearCreditAmount(requireNonNegative(row.getYearCreditAmount(), rowNo));
            item.setProfitLossAmount(requireNonNegative(row.getProfitLossAmount(), rowNo));
            balances.add(item);
        }
        ErpGlInitialBalanceSaveReqVO saveReqVO = new ErpGlInitialBalanceSaveReqVO();
        saveReqVO.setAccountSetId(accountSetId);
        saveReqVO.setBalances(balances);
        saveInitialBalances(saveReqVO);
        return balances.size();
    }

    // ==================== 内部辅助方法 ====================

    /**
     * 护栏：账套经「账簿链」存在已审核凭证时拒绝改期初
     *
     * <p>账套未挂账簿时视为无凭证（与 V193/V194 的账套解析口径一致）。
     */
    private void validateNoApprovedVoucher(Long accountSetId) {
        List<Long> accountBookIds = accountBookMapper.selectListByAccountSetId(accountSetId).stream()
                .map(ErpAccountBookDO::getId).collect(Collectors.toList());
        if (accountBookIds.isEmpty()) {
            return;
        }
        Long count = glVoucherMapper.selectCountByAccountBookIdsAndStatus(
                accountBookIds, ErpGlVoucherStatusEnum.APPROVED.getStatus());
        if (count != null && count > 0) {
            throw exception(GL_INITIAL_BALANCE_LOCKED);
        }
    }

    /**
     * 损益类科目（收入 40 / 费用 50）的年初余额必须为 0（派生值校验，ADR 265 §3 裁定 5）
     */
    private void validateProfitLossYearOpening(ErpGlAccountDO subject, ErpGlInitialBalanceSaveReqVO.Balance item) {
        boolean profitLoss = ErpGlAccountTypeEnum.REVENUE.getType().equals(subject.getType())
                || ErpGlAccountTypeEnum.EXPENSE.getType().equals(subject.getType());
        if (!profitLoss) {
            return;
        }
        BigDecimal yearOpening = calcYearOpening(subject.getBalanceDirection(),
                item.getOpeningAmount(), item.getYearDebitAmount(), item.getYearCreditAmount());
        if (yearOpening.abs().compareTo(BALANCE_TOLERANCE) >= 0) {
            throw exception(GL_INITIAL_BALANCE_PROFIT_LOSS_YEAR_OPENING, subject.getCode());
        }
    }

    private ErpGlInitialBalanceRespVO buildRespVO(ErpGlAccountDO subject, ErpGlInitialBalanceDO balance) {
        ErpGlInitialBalanceRespVO respVO = new ErpGlInitialBalanceRespVO();
        respVO.setSubjectId(subject.getId());
        respVO.setSubjectCode(subject.getCode());
        respVO.setSubjectName(subject.getName());
        respVO.setParentId(subject.getParentId());
        respVO.setType(subject.getType());
        respVO.setTypeName(resolveTypeName(subject.getType()));
        respVO.setBalanceDirection(subject.getBalanceDirection());
        respVO.setBalanceDirectionName(resolveDirectionName(subject.getBalanceDirection()));
        respVO.setIsLeaf(subject.getIsLeaf());
        BigDecimal opening = balance == null ? BigDecimal.ZERO : nvl(balance.getOpeningAmount());
        BigDecimal yearDebit = balance == null ? BigDecimal.ZERO : nvl(balance.getYearDebitAmount());
        BigDecimal yearCredit = balance == null ? BigDecimal.ZERO : nvl(balance.getYearCreditAmount());
        respVO.setOpeningAmount(opening);
        respVO.setYearDebitAmount(yearDebit);
        respVO.setYearCreditAmount(yearCredit);
        respVO.setYearOpeningAmount(calcYearOpening(subject.getBalanceDirection(), opening, yearDebit, yearCredit));
        respVO.setProfitLossAmount(balance == null ? BigDecimal.ZERO : nvl(balance.getProfitLossAmount()));
        return respVO;
    }

    private Map<Long, ErpGlAccountDO> loadSubjectMap(List<ErpGlInitialBalanceDO> rows) {
        Set<Long> accountIds = rows.stream().map(ErpGlInitialBalanceDO::getAccountId).collect(Collectors.toSet());
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return glAccountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(ErpGlAccountDO::getId, Function.identity()));
    }

    private Map<Long, ErpGlInitialBalanceDO> mapByAccountId(List<ErpGlInitialBalanceDO> rows) {
        return rows.stream().collect(Collectors.toMap(ErpGlInitialBalanceDO::getAccountId, Function.identity()));
    }

    /**
     * 派生年初余额：借向 = opening - yearDebit + yearCredit；其余方向 = opening + yearDebit - yearCredit
     */
    private BigDecimal calcYearOpening(Integer balanceDirection, BigDecimal opening,
                                       BigDecimal yearDebit, BigDecimal yearCredit) {
        BigDecimal o = nvl(opening);
        BigDecimal d = nvl(yearDebit);
        BigDecimal c = nvl(yearCredit);
        if (ErpGlAccountBalanceDirectionEnum.DEBIT.getDirection().equals(balanceDirection)) {
            return o.subtract(d).add(c);
        }
        return o.add(d).subtract(c);
    }

    private BigDecimal requireNonNegative(BigDecimal amount, int rowNo) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.signum() < 0) {
            throw exception(GL_INITIAL_BALANCE_IMPORT_AMOUNT_INVALID, rowNo, amount);
        }
        return amount;
    }

    private String resolveTypeName(Integer type) {
        for (ErpGlAccountTypeEnum typeEnum : ErpGlAccountTypeEnum.values()) {
            if (typeEnum.getType().equals(type)) {
                return typeEnum.getName();
            }
        }
        return null;
    }

    private String resolveDirectionName(Integer direction) {
        for (ErpGlAccountBalanceDirectionEnum dirEnum : ErpGlAccountBalanceDirectionEnum.values()) {
            if (dirEnum.getDirection().equals(direction)) {
                return dirEnum.getName();
            }
        }
        return null;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

}

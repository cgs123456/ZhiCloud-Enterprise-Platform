package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPeriodStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 会计凭证 Service 实现类（P0-7）
 *
 * <p>核心实现：
 * <ul>
 *   <li>创建/更新时校验借贷平衡、分录合法性、科目末级</li>
 *   <li>审核时更新对应科目的累计发生额与期末余额</li>
 *   <li>反审核时回滚科目余额，凭证回到草稿状态</li>
 * </ul>
 *
 * <p>当前用户上下文：通过 SecurityFrameworkUtils.getLoginUserId() 获取制单人/审核人。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class ErpGlVoucherServiceImpl implements ErpGlVoucherService {

    @Resource
    private ErpGlVoucherMapper glVoucherMapper;
    @Resource
    private ErpGlVoucherEntryMapper glVoucherEntryMapper;
    @Resource
    private ErpGlAccountMapper glAccountMapper;
    @Resource
    private ErpPeriodService periodService;

    // ==================== 凭证 CRUD ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createGlVoucher(ErpGlVoucherSaveReqVO createReqVO) {
        // 校验凭证字号唯一
        if (glVoucherMapper.selectByVoucherNo(createReqVO.getVoucherNo()) != null) {
            throw exception(GL_VOUCHER_NO_EXISTS);
        }
        // 校验分录合法性 + 计算借贷合计
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (int i = 0; i < createReqVO.getEntries().size(); i++) {
            ErpGlVoucherEntryReqVO entry = createReqVO.getEntries().get(i);
            BigDecimal debit = entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount();
            BigDecimal credit = entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount();
            // 借贷不能同时有值
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw exception(GL_VOUCHER_ENTRY_DEBIT_CREDIT_BOTH, i + 1);
            }
            // 借贷不能同时为 0
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                throw exception(GL_VOUCHER_ENTRY_DEBIT_CREDIT_ZERO, i + 1);
            }
            // 校验科目存在且为末级
            ErpGlAccountDO account = glAccountMapper.selectById(entry.getAccountId());
            if (account == null) {
                throw exception(GL_ACCOUNT_NOT_EXISTS);
            }
            if (!Boolean.TRUE.equals(account.getIsLeaf())) {
                throw exception(GL_VOUCHER_ACCOUNT_NOT_LEAF, i + 1, account.getCode());
            }
            debitTotal = debitTotal.add(debit);
            creditTotal = creditTotal.add(credit);
        }
        // 校验借贷平衡
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw exception(GL_VOUCHER_NOT_BALANCE, debitTotal, creditTotal);
        }
        // 校验会计期间未关账（如指定了期间）
        Long periodId = createReqVO.getPeriodId();
        if (periodId != null) {
            ErpPeriodDO period = periodService.getPeriod(periodId);
            if (period != null && ErpPeriodStatusEnum.CLOSED.getStatus().equals(period.getStatus())) {
                throw exception(GL_VOUCHER_PERIOD_CLOSED, period.getCode());
            }
        }
        // 保存凭证主表
        ErpGlVoucherDO voucher = BeanUtils.toBean(createReqVO, ErpGlVoucherDO.class);
        voucher.setDebitTotal(debitTotal);
        voucher.setCreditTotal(creditTotal);
        voucher.setStatus(ErpGlVoucherStatusEnum.DRAFT.getStatus());
        voucher.setPreparedBy(String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        // 自动填充期间编码
        if (periodId != null) {
            ErpPeriodDO period = periodService.getPeriod(periodId);
            if (period != null) {
                voucher.setPeriodCode(period.getCode());
            }
        }
        glVoucherMapper.insert(voucher);
        // 保存分录
        saveEntries(voucher.getId(), createReqVO.getEntries());
        return voucher.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGlVoucher(ErpGlVoucherSaveReqVO updateReqVO) {
        ErpGlVoucherDO existing = validateVoucherExists(updateReqVO.getId());
        if (!ErpGlVoucherStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(GL_VOUCHER_UPDATE_FAIL_APPROVED, existing.getVoucherNo());
        }
        // 校验凭证字号唯一（排除自身）
        ErpGlVoucherDO byNo = glVoucherMapper.selectByVoucherNo(updateReqVO.getVoucherNo());
        if (byNo != null && !Objects.equals(byNo.getId(), updateReqVO.getId())) {
            throw exception(GL_VOUCHER_NO_EXISTS);
        }
        // 校验分录合法性 + 计算借贷合计
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        for (int i = 0; i < updateReqVO.getEntries().size(); i++) {
            ErpGlVoucherEntryReqVO entry = updateReqVO.getEntries().get(i);
            BigDecimal debit = entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount();
            BigDecimal credit = entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount();
            if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
                throw exception(GL_VOUCHER_ENTRY_DEBIT_CREDIT_BOTH, i + 1);
            }
            if (debit.compareTo(BigDecimal.ZERO) == 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                throw exception(GL_VOUCHER_ENTRY_DEBIT_CREDIT_ZERO, i + 1);
            }
            ErpGlAccountDO account = glAccountMapper.selectById(entry.getAccountId());
            if (account == null) {
                throw exception(GL_ACCOUNT_NOT_EXISTS);
            }
            if (!Boolean.TRUE.equals(account.getIsLeaf())) {
                throw exception(GL_VOUCHER_ACCOUNT_NOT_LEAF, i + 1, account.getCode());
            }
            debitTotal = debitTotal.add(debit);
            creditTotal = creditTotal.add(credit);
        }
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw exception(GL_VOUCHER_NOT_BALANCE, debitTotal, creditTotal);
        }
        // 更新主表
        ErpGlVoucherDO updateObj = BeanUtils.toBean(updateReqVO, ErpGlVoucherDO.class);
        updateObj.setDebitTotal(debitTotal);
        updateObj.setCreditTotal(creditTotal);
        // 自动填充期间编码
        if (updateReqVO.getPeriodId() != null) {
            ErpPeriodDO period = periodService.getPeriod(updateReqVO.getPeriodId());
            if (period != null) {
                updateObj.setPeriodCode(period.getCode());
            }
        }
        glVoucherMapper.updateById(updateObj);
        // 删除旧分录，重新插入
        glVoucherEntryMapper.deleteByVoucherId(updateReqVO.getId());
        saveEntries(updateReqVO.getId(), updateReqVO.getEntries());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGlVoucher(Long id) {
        ErpGlVoucherDO existing = validateVoucherExists(id);
        if (!ErpGlVoucherStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(GL_VOUCHER_DELETE_FAIL_APPROVED, existing.getVoucherNo());
        }
        glVoucherEntryMapper.deleteByVoucherId(id);
        glVoucherMapper.deleteById(id);
    }

    @Override
    public ErpGlVoucherDO getGlVoucher(Long id) {
        return glVoucherMapper.selectById(id);
    }

    @Override
    public List<ErpGlVoucherEntryDO> getGlVoucherEntryList(Long voucherId) {
        return glVoucherEntryMapper.selectListByVoucherId(voucherId);
    }

    @Override
    public PageResult<ErpGlVoucherDO> getGlVoucherPage(ErpGlVoucherPageReqVO pageReqVO) {
        return glVoucherMapper.selectPage(pageReqVO);
    }

    // ==================== 审核 / 反审核（含科目余额维护） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveGlVoucher(Long id) {
        ErpGlVoucherDO existing = validateVoucherExists(id);
        if (!ErpGlVoucherStatusEnum.DRAFT.getStatus().equals(existing.getStatus())) {
            throw exception(GL_VOUCHER_APPROVE_FAIL);
        }
        // 更新凭证状态
        ErpGlVoucherDO update = new ErpGlVoucherDO();
        update.setId(id);
        update.setStatus(ErpGlVoucherStatusEnum.APPROVED.getStatus());
        update.setApprovedBy(String.valueOf(SecurityFrameworkUtils.getLoginUserId()));
        update.setApprovedTime(LocalDateTime.now());
        glVoucherMapper.updateById(update);
        // 更新科目累计发生额和期末余额
        List<ErpGlVoucherEntryDO> entries = glVoucherEntryMapper.selectListByVoucherId(id);
        for (ErpGlVoucherEntryDO entry : entries) {
            updateAccountBalance(entry.getAccountId(),
                    entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount(),
                    entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount(),
                    true);
        }
        log.info("[approveGlVoucher][凭证({})审核成功，更新了 {} 个科目余额]", existing.getVoucherNo(), entries.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseApproveGlVoucher(Long id) {
        ErpGlVoucherDO existing = validateVoucherExists(id);
        if (!ErpGlVoucherStatusEnum.APPROVED.getStatus().equals(existing.getStatus())) {
            throw exception(GL_VOUCHER_PROCESS_FAIL);
        }
        // 回滚科目余额
        List<ErpGlVoucherEntryDO> entries = glVoucherEntryMapper.selectListByVoucherId(id);
        for (ErpGlVoucherEntryDO entry : entries) {
            updateAccountBalance(entry.getAccountId(),
                    entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount(),
                    entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount(),
                    false);
        }
        // 凭证回到草稿状态
        ErpGlVoucherDO update = new ErpGlVoucherDO();
        update.setId(id);
        update.setStatus(ErpGlVoucherStatusEnum.DRAFT.getStatus());
        update.setApprovedBy(null);
        update.setApprovedTime(null);
        glVoucherMapper.updateById(update);
        log.info("[reverseApproveGlVoucher][凭证({})反审核成功，回滚了 {} 个科目余额]", existing.getVoucherNo(), entries.size());
    }

    // ==================== 内部辅助方法 ====================

    private ErpGlVoucherDO validateVoucherExists(Long id) {
        if (id == null) {
            throw exception(GL_VOUCHER_NOT_EXISTS);
        }
        ErpGlVoucherDO voucher = glVoucherMapper.selectById(id);
        if (voucher == null) {
            throw exception(GL_VOUCHER_NOT_EXISTS);
        }
        return voucher;
    }

    /**
     * 保存分录列表（冗余科目编码/名称）
     */
    private void saveEntries(Long voucherId, List<ErpGlVoucherEntryReqVO> entries) {
        List<ErpGlVoucherEntryDO> entryDOs = new ArrayList<>(entries.size());
        for (int i = 0; i < entries.size(); i++) {
            ErpGlVoucherEntryReqVO reqVO = entries.get(i);
            ErpGlVoucherEntryDO entryDO = BeanUtils.toBean(reqVO, ErpGlVoucherEntryDO.class);
            entryDO.setVoucherId(voucherId);
            if (entryDO.getSort() == null) {
                entryDO.setSort(i + 1);
            }
            // 冗余科目编码与名称
            ErpGlAccountDO account = glAccountMapper.selectById(reqVO.getAccountId());
            if (account != null) {
                entryDO.setAccountCode(account.getCode());
                entryDO.setAccountName(account.getName());
            }
            entryDOs.add(entryDO);
        }
        glVoucherEntryMapper.insertBatch(entryDOs);
    }

    /**
     * 更新科目累计发生额与期末余额
     *
     * <p>规则：
     * <ul>
     *   <li>审核：currentDebit += debitAmount, currentCredit += creditAmount</li>
     *   <li>反审核：currentDebit -= debitAmount, currentCredit -= creditAmount</li>
     *   <li>期末余额：closingDebit = openingDebit + currentDebit - currentCredit（借方科目）</li>
     *   <li>期末余额：closingCredit = openingCredit + currentCredit - currentDebit（贷方科目）</li>
     * </ul>
     */
    private void updateAccountBalance(Long accountId, BigDecimal debitAmount, BigDecimal creditAmount, boolean isApprove) {
        ErpGlAccountDO account = glAccountMapper.selectById(accountId);
        if (account == null) {
            log.warn("[updateAccountBalance][科目({})不存在，跳过余额更新]", accountId);
            return;
        }
        BigDecimal currentDebit = account.getCurrentDebit() == null ? BigDecimal.ZERO : account.getCurrentDebit();
        BigDecimal currentCredit = account.getCurrentCredit() == null ? BigDecimal.ZERO : account.getCurrentCredit();
        BigDecimal openingDebit = account.getOpeningDebit() == null ? BigDecimal.ZERO : account.getOpeningDebit();
        BigDecimal openingCredit = account.getOpeningCredit() == null ? BigDecimal.ZERO : account.getOpeningCredit();

        if (isApprove) {
            currentDebit = currentDebit.add(debitAmount);
            currentCredit = currentCredit.add(creditAmount);
        } else {
            currentDebit = currentDebit.subtract(debitAmount);
            currentCredit = currentCredit.subtract(creditAmount);
        }

        // 期末余额计算（统一按借方方向计算：余额=借方累计-贷方累计）
        BigDecimal balance = openingDebit.add(currentDebit).subtract(openingCredit).subtract(currentCredit);
        BigDecimal closingDebit;
        BigDecimal closingCredit;
        if (balance.compareTo(BigDecimal.ZERO) >= 0) {
            closingDebit = balance;
            closingCredit = BigDecimal.ZERO;
        } else {
            closingDebit = BigDecimal.ZERO;
            closingCredit = balance.negate();
        }

        ErpGlAccountDO update = new ErpGlAccountDO();
        update.setId(accountId);
        update.setCurrentDebit(currentDebit);
        update.setCurrentCredit(currentCredit);
        update.setClosingDebit(closingDebit);
        update.setClosingCredit(closingCredit);
        glAccountMapper.updateById(update);
    }

}

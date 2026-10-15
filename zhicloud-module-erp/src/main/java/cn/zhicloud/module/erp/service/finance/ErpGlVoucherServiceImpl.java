package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import java.util.LinkedHashMap;
import java.util.Map;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingEventDO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingPostingDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingEventMapper;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingPostingMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingEventStatusEnum;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingPostingStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

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
 * @author 智云
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
    @Resource
    private ErpAccountingPostingMapper accountingPostingMapper;
    @Resource
    private ErpAccountingEventMapper accountingEventMapper;
    @Resource
    private ErpAccountBookMapper accountBookMapper;

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
            ErpGlAccountDO account = selectAccountForVoucher(entry.getAccountId());
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
        // 账簿归属（§12.215 修 V191 断链）：SaveReqVO 未传账簿时，取「启用的主账簿」
        // （不限会计准则，见 ErpAccountBookMapper#selectEnabledPrimary）挂上——会计事件链
        // 此前因 SaveReqVO 无 accountBookId 字段，凭证 account_book_id 恒 NULL，
        // 业务链凭证不归任何账套，账套视角报表看不到数。
        // 显式传了账簿则原样使用（但必须存在，避免挂已删/不存在账簿）；
        // 主账簿也不存在（未配账簿的租户）才留 NULL，既有手工凭证行为不变。
        if (createReqVO.getAccountBookId() == null) {
            ErpAccountBookDO primaryBook = accountBookMapper.selectEnabledPrimary();
            if (primaryBook != null) {
                createReqVO.setAccountBookId(primaryBook.getId());
            }
        } else if (accountBookMapper.selectById(createReqVO.getAccountBookId()) == null) {
            throw exception(GL_VOUCHER_ACCOUNT_BOOK_NOT_EXISTS, createReqVO.getAccountBookId());
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
            ErpGlAccountDO account = selectAccountForVoucher(entry.getAccountId());
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
        // 账簿归属（§12.215）：未传账簿时沿用凭证已有账簿（不因前端省略该字段把
        // 已挂账套的凭证退化成 NULL）；显式传则校验存在。createGlVoucher 的
        // 「推断主账簿」不适用于 update——修改动作不应悄悄改归属。
        if (updateReqVO.getAccountBookId() == null) {
            updateObj.setAccountBookId(existing.getAccountBookId());
        } else if (accountBookMapper.selectById(updateReqVO.getAccountBookId()) == null) {
            throw exception(GL_VOUCHER_ACCOUNT_BOOK_NOT_EXISTS, updateReqVO.getAccountBookId());
        }
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
        // V193：按 FMS 账套筛凭证——先把账套解析成它名下的账簿集合，再按账簿 IN 查询。
        // 凭证不落 account_set_id 冗余列（账簿改挂账套时冗余列会漂移），单一来源是账簿。
        if (pageReqVO.getAccountSetId() != null) {
            List<Long> bookIds = accountBookMapper.selectListByAccountSetId(pageReqVO.getAccountSetId()).stream()
                    .map(ErpAccountBookDO::getId).collect(Collectors.toList());
            pageReqVO.setAccountBookIds(bookIds);
            // 账套下无账簿：直接返回空页，避免 IN(空集合) 的语义歧义
            if (bookIds.isEmpty()) {
                return new PageResult<>(Collections.emptyList(), 0L);
            }
        }
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
        updateAccountBalances(entries, true);
        markAccountingResultsPosted(id, update.getApprovedTime());
        log.info("[approveGlVoucher][凭证({})审核成功，更新了 {} 个科目余额]", existing.getVoucherNo(), entries.size());
    }

    private void markAccountingResultsPosted(Long voucherId, LocalDateTime postedAt) {
        for (ErpAccountingPostingDO posting : accountingPostingMapper.selectListByVoucherId(voucherId)) {
            if (!ErpAccountingPostingStatusEnum.POSTED.getStatus().equals(posting.getStatus())) {
                posting.setStatus(ErpAccountingPostingStatusEnum.POSTED.getStatus());
                posting.setPostedAt(postedAt);
                accountingPostingMapper.updateById(posting);
            }
            ErpAccountingEventDO event = accountingEventMapper.selectByEventId(posting.getEventId());
            if (event != null && !ErpAccountingEventStatusEnum.POSTED.getStatus().equals(event.getStatus())) {
                event.setStatus(ErpAccountingEventStatusEnum.POSTED.getStatus());
                event.setPostedAt(postedAt);
                accountingEventMapper.updateById(event);
            }
        }
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
        updateAccountBalances(entries, false);
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
            ErpGlAccountDO account = selectAccountForVoucher(reqVO.getAccountId());
            if (account != null) {
                entryDO.setAccountCode(account.getCode());
                entryDO.setAccountName(account.getName());
            }
            entryDOs.add(entryDO);
        }
        glVoucherEntryMapper.insertBatch(entryDOs);
    }

    /**
     * 按科目聚合金额后逐科目更新一次余额，减少同一科目在同一次审核中的重复 SELECT/UPDATE。
     *
     * <p>§12.226：逐科目加行锁（updateAccountBalance 内 FOR UPDATE），故必须按
     * **固定的科目编号顺序**加锁——两张凭证命中同一组科目但分录顺序不同时，
     * 若按各自分录顺序加锁会形成交叉等待死锁。按 accountId 升序统一顺序。
     */
    private void updateAccountBalances(List<ErpGlVoucherEntryDO> entries, boolean isApprove) {
        Map<Long, BigDecimal[]> aggregated = new LinkedHashMap<>();
        for (ErpGlVoucherEntryDO entry : entries) {
            BigDecimal debit = entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount();
            BigDecimal credit = entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount();
            BigDecimal[] amounts = aggregated.computeIfAbsent(entry.getAccountId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            amounts[0] = amounts[0].add(debit);
            amounts[1] = amounts[1].add(credit);
        }
        // 按 accountId 升序加锁，保证并发事务的加锁顺序一致，避免死锁
        aggregated.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> updateAccountBalance(e.getKey(), e.getValue()[0], e.getValue()[1], isApprove));
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
        // 加行锁读取（§12.226）：余额是读-改-写，必须锁住同一科目的并发维护，
        // 否则并发审核两张命中同一科目的凭证时后写覆盖先写、金额少计。
        ErpGlAccountDO account = selectAccountForVoucherForUpdate(accountId);
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
        TenantUtils.execute(account.getTenantId(), () -> glAccountMapper.updateById(update));
    }

    /**
     * 加行锁查科目（余额维护专用，§12.226）
     *
     * <p>与 {@link #selectAccountForVoucher} 同一套租户解析逻辑（先当前租户、再平台租户），
     * 但查询走 {@code selectByIdForUpdate}（FOR UPDATE）。仅用于余额读-改-写路径；
     * 校验类调用仍用无锁版本，避免无谓扩大锁范围。必须在事务内调用。
     */
    private ErpGlAccountDO selectAccountForVoucherForUpdate(Long accountId) {
        ErpGlAccountDO account = glAccountMapper.selectByIdForUpdate(accountId);
        if (account != null) {
            return account;
        }
        account = TenantUtils.execute(0L, () -> glAccountMapper.selectByIdForUpdate(accountId));
        if (account != null && Long.valueOf(0L).equals(account.getTenantId())) {
            return account;
        }
        return null;
    }

    private ErpGlAccountDO selectAccountForVoucher(Long accountId) {
        ErpGlAccountDO account = glAccountMapper.selectById(accountId);
        if (account != null) {
            return account;
        }
        account = TenantUtils.execute(0L, () -> glAccountMapper.selectById(accountId));
        if (account != null && Long.valueOf(0L).equals(account.getTenantId())) {
            return account;
        }
        return null;
    }
}

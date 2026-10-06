package cn.zhicloud.module.erp.service.accounting;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingEventDO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingPostingDO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpPostingRuleDO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpPostingRuleLineDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingEventMapper;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingPostingMapper;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpPostingRuleLineMapper;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpPostingRuleMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingEventStatusEnum;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingPostingStatusEnum;
import cn.zhicloud.module.erp.enums.accounting.ErpPostingEntrySideEnum;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodStatusEnum;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.finance.ErpGlVoucherService;
import cn.zhicloud.module.erp.service.finance.ErpPeriodService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 会计事件服务实现。
 *
 * <p>当前阶段在一个数据库事务内完成事件写入、规则解析和凭证草稿生成。
 * 凭证审核后再由后续过账监听器推进事件为 POSTED，避免把草稿误标为已过账。
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class ErpAccountingEventServiceImpl implements ErpAccountingEventService {

    private static final DateTimeFormatter PERIOD_CODE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final String DEFAULT_CURRENCY = "cny";
    private static final BigDecimal DEFAULT_EXCHANGE_RATE = BigDecimal.ONE;

    @Resource
    private ErpAccountingEventMapper accountingEventMapper;
    @Resource
    private ErpAccountingPostingMapper accountingPostingMapper;
    @Resource
    private ErpPostingRuleMapper postingRuleMapper;
    @Resource
    private ErpPostingRuleLineMapper postingRuleLineMapper;
    @Resource
    private ErpGlAccountMapper glAccountMapper;
    @Resource
    private ErpGlVoucherService glVoucherService;
    @Resource
    private ErpPeriodService periodService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long postEvent(ErpAccountingEventCreateReqBO request) {
        requireTenantContext();
        validateRequest(request);
        ErpAccountingEventDO reversalOriginal = validateReversalReference(request);
        String idempotencyKey = buildIdempotencyKey(request);
        ErpAccountingEventDO existing = accountingEventMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return getExistingVoucherId(existing);
        }
        // 勾稽守卫必须放在幂等短路之后：反审核再重新审核会命中同一幂等键并直接返回原凭证，
        // 若先做累计判定，会把「原事件仍存在」误算成第二次占用额度而拒绝重新审核。
        BigDecimal docTotal = reversalOriginal == null
                ? resolveDocTotalAmount(request)
                : inheritDocTotalAmount(request, reversalOriginal);

        ErpPostingRuleDO rule = selectPostingRule(request.getEventType());
        // 分录行必须与所选规则同租户读取：规则选择会回退到平台模板（tenant_id=0），
        // 而分录行查询带着当前租户拦截器——平台模板的分录行对业务租户不可见，
        // 任何走平台模板的过账都会在这里报「过账规则没有分录行」（真机踩过）
        List<ErpPostingRuleLineDO> ruleLines = TenantUtils.execute(rule.getTenantId(),
                () -> postingRuleLineMapper.selectListByRuleId(rule.getId()));
        List<ErpGlVoucherEntryReqVO> entries = buildVoucherEntries(request, ruleLines);
        BigDecimal debitTotal = sumDebit(entries);
        BigDecimal creditTotal = sumCredit(entries);
        if (debitTotal.compareTo(creditTotal) != 0) {
            throw exception(ACCOUNTING_RULE_NOT_BALANCED, debitTotal, creditTotal);
        }
        if (debitTotal.compareTo(BigDecimal.ZERO) == 0) {
            throw exception(ACCOUNTING_RULE_LINE_INVALID, "规则生成的分录金额全部为 0");
        }

        ErpPeriodDO period = resolvePeriod(request.getAccountingDate());
        ErpAccountingEventDO event = buildEvent(request, rule, period, idempotencyKey, docTotal);
        accountingEventMapper.insert(event);
        if (event.getId() == null) {
            throw exception(ACCOUNTING_VOUCHER_CREATE_FAILED, "会计事件主键生成失败");
        }

        Long voucherId = createVoucher(request, event, period, entries);
        insertPosting(event, rule, voucherId);
        log.info("[postEvent][事件({})按规则({}/{})生成凭证草稿({})]", event.getEventId(),
                rule.getRuleCode(), rule.getVersion(), voucherId);
        return voucherId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reverseEvent(ErpAccountingEventReverseReqBO request) {
        requireTenantContext();
        validateReverseRequest(request);
        Long sourceDocLineId = defaultZero(request.getSourceDocLineId());
        Integer sourceRevision = defaultZero(request.getSourceRevision());
        Integer sequenceNo = defaultZero(request.getSequenceNo());
        ErpAccountingEventDO original = accountingEventMapper.selectBySource(request.getEventType(),
                request.getSourceDocType(), request.getSourceDocId(), sourceDocLineId, sourceRevision,
                sequenceNo, 0L);
        if (original == null || original.getId() == null) {
            throw exception(ACCOUNTING_EVENT_NOT_FOUND,
                    request.getSourceDocType() + ":" + request.getSourceDocId());
        }
        if (!ErpAccountingEventStatusEnum.POSTED.getStatus().equals(original.getStatus())) {
            throw exception(ACCOUNTING_EVENT_NOT_POSTED, original.getEventId());
        }
        Long voucherId = postEvent(ErpAccountingEventCreateReqBO.builder()
                .eventType(original.getEventType())
                .sourceDocType(original.getSourceDocType())
                .sourceDocId(original.getSourceDocId())
                .sourceDocNo(original.getSourceDocNo())
                .sourceDocLineId(original.getSourceDocLineId())
                .sourceRevision(original.getSourceRevision())
                .sequenceNo(original.getSequenceNo())
                .eventDate(request.getEventDate() == null ? request.getAccountingDate() : request.getEventDate())
                .accountingDate(request.getAccountingDate())
                .amountWithoutTax(defaultZero(original.getAmountWithoutTax()).negate())
                .taxAmount(defaultZero(original.getTaxAmount()).negate())
                .amountWithTax(defaultZero(original.getAmountWithTax()).negate())
                .costAmount(defaultZero(original.getCostAmount()).negate())
                .currency(original.getCurrency())
                .exchangeRate(original.getExchangeRate())
                .reversalOfEventId(original.getId())
                .occurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt())
                .build());
        markOriginalReversed(original);
        return voucherId;
    }

    /**
     * 将原事件标记为已冲销。
     *
     * <p>冲销不改写原事件金额与日期，仅推进状态，保证对账程序可以区分
     * 「已过账待冲销」与「已冲销」两类事件，并阻止同一原事件被重复冲销。
     */
    private void markOriginalReversed(ErpAccountingEventDO original) {
        ErpAccountingEventDO update = new ErpAccountingEventDO();
        update.setId(original.getId());
        update.setStatus(ErpAccountingEventStatusEnum.REVERSED.getStatus());
        accountingEventMapper.updateById(update);
    }

    private void validateReverseRequest(ErpAccountingEventReverseReqBO request) {
        if (request == null) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "冲销请求不能为空");
        }
        if (StrUtil.isBlank(request.getEventType())) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "事件类型不能为空");
        }
        if (StrUtil.isBlank(request.getSourceDocType())) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "来源单据类型不能为空");
        }
        if (request.getSourceDocId() == null || request.getSourceDocId() <= 0) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "来源单据编号必须大于 0");
        }
        if (request.getAccountingDate() == null) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "冲销会计日期不能为空");
        }
    }

    private void validateRequest(ErpAccountingEventCreateReqBO request) {
        if (request == null) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "请求不能为空");
        }
        if (StrUtil.isBlank(request.getEventType())) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "事件类型不能为空");
        }
        if (StrUtil.isBlank(request.getSourceDocType())) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "来源单据类型不能为空");
        }
        if (request.getSourceDocId() == null || request.getSourceDocId() <= 0) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "来源单据编号必须大于 0");
        }
        if (request.getEventDate() == null || request.getAccountingDate() == null) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "业务日期和会计日期不能为空");
        }
        BigDecimal amountWithoutTax = defaultZero(request.getAmountWithoutTax());
        BigDecimal taxAmount = defaultZero(request.getTaxAmount());
        BigDecimal amountWithTax = defaultZero(request.getAmountWithTax());
        if (amountWithoutTax.add(taxAmount).compareTo(amountWithTax) != 0) {
            throw exception(ACCOUNTING_EVENT_AMOUNT_MISMATCH, amountWithoutTax, taxAmount, amountWithTax);
        }
        if (request.getExchangeRate() != null && request.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "汇率必须大于 0");
        }
        if (request.getSequenceNo() != null && request.getSequenceNo() < 0) {
            throw exception(ACCOUNTING_EVENT_REQUEST_INVALID, "期次序号不能为负数");
        }
    }

    private void requireTenantContext() {
        if (TenantContextHolder.getTenantId() == null) {
            throw exception(ACCOUNTING_EVENT_TENANT_REQUIRED);
        }
    }

    private ErpPostingRuleDO selectPostingRule(String eventType) {
        List<ErpPostingRuleDO> effectiveRules = selectEffectiveRules(eventType,
                postingRuleMapper.selectListByEventType(eventType));
        if (effectiveRules.isEmpty() && !isPlatformTenant()) {
            // 租户没有有效自定义规则时，回退到 tenant_id=0 的平台默认模板。
            effectiveRules = TenantUtils.execute(0L, () -> selectEffectiveRules(eventType,
                    postingRuleMapper.selectListByEventType(eventType)));
        }
        if (effectiveRules.isEmpty()) {
            throw exception(ACCOUNTING_RULE_NOT_FOUND, eventType);
        }
        ErpPostingRuleDO selected = effectiveRules.get(0);
        if (StrUtil.isNotBlank(selected.getConditionJson())) {
            throw exception(ACCOUNTING_RULE_CONDITION_UNSUPPORTED, selected.getRuleCode());
        }
        boolean ambiguous = effectiveRules.stream().filter(rule -> Objects.equals(rule.getEffectiveFrom(),
                selected.getEffectiveFrom())).count() > 1;
        if (ambiguous) {
            throw exception(ACCOUNTING_RULE_AMBIGUOUS, eventType, selected.getEffectiveFrom());
        }
        return selected;
    }

    private List<ErpPostingRuleDO> selectEffectiveRules(String eventType, List<ErpPostingRuleDO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now();
        return candidates.stream()
                .filter(rule -> Boolean.TRUE.equals(rule.getEnabled()))
                .filter(rule -> rule.getEffectiveFrom() != null && !rule.getEffectiveFrom().isAfter(now))
                .filter(rule -> rule.getEffectiveTo() == null || rule.getEffectiveTo().isAfter(now))
                .sorted(Comparator.comparing(ErpPostingRuleDO::getEffectiveFrom).reversed())
                .toList();
    }

    private boolean isPlatformTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId != null && tenantId == 0L;
    }

    private List<ErpGlVoucherEntryReqVO> buildVoucherEntries(ErpAccountingEventCreateReqBO request,
                                                              List<ErpPostingRuleLineDO> ruleLines) {
        if (ruleLines == null || ruleLines.isEmpty()) {
            throw exception(ACCOUNTING_RULE_LINE_INVALID, "过账规则没有分录行");
        }
        List<ErpGlVoucherEntryReqVO> entries = new ArrayList<>();
        boolean hasDebit = false;
        boolean hasCredit = false;
        for (ErpPostingRuleLineDO ruleLine : ruleLines) {
            if (StrUtil.isNotBlank(ruleLine.getConditionJson())) {
                throw exception(ACCOUNTING_RULE_CONDITION_UNSUPPORTED, ruleLine.getRuleId());
            }
            ErpPostingEntrySideEnum entrySide = ErpPostingEntrySideEnum.valueOf(ruleLine.getEntrySide());
            if (entrySide == null) {
                throw exception(ACCOUNTING_RULE_LINE_INVALID, "分录方向无效：" + ruleLine.getEntrySide());
            }
            if (StrUtil.isBlank(ruleLine.getAccountCode()) || StrUtil.isNotBlank(ruleLine.getAccountResolver())) {
                throw exception(ACCOUNTING_RULE_LINE_INVALID, "当前阶段仅支持固定科目编码");
            }
            BigDecimal amount = resolveAmount(request, ruleLine.getAmountExpression());
            if (amount.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            ErpGlAccountDO account = glAccountMapper.selectByCode(ruleLine.getAccountCode());
            if (account == null && !isPlatformTenant()) {
                // 租户可覆盖同编码科目；未配置时使用 tenant_id=0 的平台默认科目模板。
                account = TenantUtils.execute(0L, () -> glAccountMapper.selectByCode(ruleLine.getAccountCode()));
            }
            if (account == null) {
                throw exception(ACCOUNTING_RULE_ACCOUNT_NOT_FOUND, ruleLine.getAccountCode());
            }
            if (!Boolean.TRUE.equals(account.getIsLeaf())) {
                throw exception(GL_VOUCHER_ACCOUNT_NOT_LEAF, ruleLine.getLineNo(), account.getCode());
            }
            ErpGlVoucherEntryReqVO entry = new ErpGlVoucherEntryReqVO();
            entry.setAccountId(account.getId());
            entry.setSummary(StrUtil.blankToDefault(ruleLine.getRemark(), request.getSourceDocNo()));
            entry.setSort(ruleLine.getLineNo());
            if (entrySide == ErpPostingEntrySideEnum.DEBIT) {
                if (amount.signum() < 0) {
                    entry.setDebitAmount(BigDecimal.ZERO);
                    entry.setCreditAmount(amount.abs());
                    hasCredit = true;
                } else {
                    entry.setDebitAmount(amount);
                    entry.setCreditAmount(BigDecimal.ZERO);
                    hasDebit = true;
                }
            } else {
                if (amount.signum() < 0) {
                    entry.setDebitAmount(amount.abs());
                    entry.setCreditAmount(BigDecimal.ZERO);
                    hasDebit = true;
                } else {
                    entry.setDebitAmount(BigDecimal.ZERO);
                    entry.setCreditAmount(amount);
                    hasCredit = true;
                }
            }
            entries.add(entry);
        }
        if (!hasDebit || !hasCredit || entries.size() < 2) {
            throw exception(ACCOUNTING_RULE_LINE_INVALID, "过账规则必须至少生成一条借方和一条贷方分录");
        }
        return entries;
    }

    private BigDecimal resolveAmount(ErpAccountingEventCreateReqBO request, String expression) {
        if (StrUtil.isBlank(expression)) {
            throw exception(ACCOUNTING_RULE_AMOUNT_EXPRESSION_INVALID, expression);
        }
        String normalized = expression.trim();
        boolean negative = normalized.startsWith("-");
        if (negative) {
            normalized = normalized.substring(1).trim();
        }
        BigDecimal amount = switch (normalized) {
            case "amountWithoutTax" -> defaultZero(request.getAmountWithoutTax());
            case "taxAmount" -> defaultZero(request.getTaxAmount());
            case "amountWithTax" -> defaultZero(request.getAmountWithTax());
            case "costAmount" -> defaultZero(request.getCostAmount());
            default -> throw exception(ACCOUNTING_RULE_AMOUNT_EXPRESSION_INVALID, expression);
        };
        return negative ? amount.negate() : amount;
    }

    private ErpPeriodDO resolvePeriod(java.time.LocalDate accountingDate) {
        String periodCode = accountingDate.format(PERIOD_CODE_FORMATTER);
        ErpPeriodDO period = periodService.getPeriodByCode(periodCode);
        if (period == null) {
            throw exception(PERIOD_NOT_EXISTS);
        }
        if (!ErpPeriodStatusEnum.OPEN.getStatus().equals(period.getStatus())) {
            throw exception(GL_VOUCHER_PERIOD_CLOSED, periodCode);
        }
        return period;
    }

    /**
     * 校验红冲/退货事件引用的原事件。
     *
     * <p>引用原事件必须存在且已过账，避免出现指向不存在或草稿事件的悬空引用，
     * 也避免同一原事件被重复红冲后无法被对账程序识别。
     */
    private ErpAccountingEventDO validateReversalReference(ErpAccountingEventCreateReqBO request) {
        Long reversalOfEventId = request.getReversalOfEventId();
        if (reversalOfEventId == null || reversalOfEventId <= 0) {
            return null;
        }
        ErpAccountingEventDO original = accountingEventMapper.selectById(reversalOfEventId);
        if (original == null || original.getId() == null) {
            throw exception(ACCOUNTING_EVENT_NOT_FOUND, "reversalOfEventId=" + reversalOfEventId);
        }
        if (!ErpAccountingEventStatusEnum.POSTED.getStatus().equals(original.getStatus())) {
            throw exception(ACCOUNTING_EVENT_NOT_POSTED, original.getEventId());
        }
        return original;
    }

    /**
     * 解析并校验来源单据勾稽总额（金额上界）。
     *
     * <p>规则：同一 {@code (eventType, sourceDocType, sourceDocId)} 下全部事件的
     * {@code amountWithTax} 带符号求和（含冲销事件），其绝对值不得大于勾稽总额。
     * 带符号求和让「原事件 + 冲销事件」自动相互抵消，因此已冲销的额度会被释放，
     * 不需要按状态过滤，也不会把冲销额度重复计入占用。
     *
     * <p>基线一致性也是 fail-closed：已落地基线之间、以及本次请求与已落地基线之间
     * 只要不一致就拒绝过账，不允许两条互相矛盾的上界同时进入审计记录。
     *
     * <p>采用 fail-closed：取不到勾稽总额就拒绝过账。唯一键只能防止同一来源重复落事件，
     * 不能防止同一单据的期次金额累计超额（分批开票 / 分期收款 / 分期付款正是靠期次落地）。
     * 在没有上界的情况下放行，等于把「超额过账」的风险静默转移给对账环节。
     *
     * @return 本次事件应落库的勾稽总额
     */
    private BigDecimal resolveDocTotalAmount(ErpAccountingEventCreateReqBO request) {
        List<ErpAccountingEventDO> existing = accountingEventMapper.selectListForReconciliation(
                request.getEventType(), request.getSourceDocType(), request.getSourceDocId());
        List<ErpAccountingEventDO> events = existing == null ? List.of() : existing;

        BigDecimal baselineMin = null;
        BigDecimal baselineMax = null;
        for (ErpAccountingEventDO event : events) {
            BigDecimal candidate = event.getDocTotalAmount();
            if (candidate == null) {
                continue;
            }
            if (baselineMin == null || candidate.compareTo(baselineMin) < 0) {
                baselineMin = candidate;
            }
            if (baselineMax == null || candidate.compareTo(baselineMax) > 0) {
                baselineMax = candidate;
            }
        }
        if (baselineMin != null && baselineMax != null && baselineMin.compareTo(baselineMax) != 0) {
            throw exception(ACCOUNTING_EVENT_DOC_TOTAL_CONFLICT, baselineMin, baselineMax);
        }

        BigDecimal requested = request.getDocTotalAmount();
        // 请求基线必须与已落地基线一致：不一致说明单据金额被改动，或调用方用了错误口径。
        // 立即拒绝，而不是让两条互相矛盾的上界同时留在审计记录里，把矛盾留给对账环节。
        if (requested != null && baselineMin != null && requested.compareTo(baselineMin) != 0) {
            throw exception(ACCOUNTING_EVENT_DOC_TOTAL_CONFLICT, baselineMin, requested);
        }
        BigDecimal docTotal = requested != null ? requested : baselineMin;
        if (docTotal == null) {
            throw exception(ACCOUNTING_EVENT_DOC_TOTAL_MISSING, request.getEventType(),
                    request.getSourceDocType(), request.getSourceDocId());
        }
        if (docTotal.signum() < 0) {
            throw exception(ACCOUNTING_EVENT_DOC_TOTAL_INVALID, docTotal);
        }

        BigDecimal signedSum = BigDecimal.ZERO;
        for (ErpAccountingEventDO event : events) {
            signedSum = signedSum.add(defaultZero(event.getAmountWithTax()));
        }
        BigDecimal nextAbs = signedSum.add(defaultZero(request.getAmountWithTax())).abs();
        if (nextAbs.compareTo(docTotal) > 0) {
            throw exception(ACCOUNTING_EVENT_DOC_TOTAL_EXCEEDED, nextAbs, docTotal,
                    request.getEventType(), request.getSourceDocType(), request.getSourceDocId());
        }
        return docTotal;
    }

    /**
     * 冲销事件沿用原事件的勾稽总额。
     *
     * <p>冲销事件本身是负向事件，会与原事件在带符号求和中相互抵消，因此不单独做
     * 上界判定；但仍需落库基线，便于对账程序按同一上界复算整条链路。
     */
    private BigDecimal inheritDocTotalAmount(ErpAccountingEventCreateReqBO request,
                                            ErpAccountingEventDO original) {
        BigDecimal requested = request.getDocTotalAmount();
        BigDecimal inherited = original.getDocTotalAmount();
        if (requested != null && inherited != null && requested.compareTo(inherited) != 0) {
            throw exception(ACCOUNTING_EVENT_DOC_TOTAL_CONFLICT, inherited, requested);
        }
        return requested != null ? requested : inherited;
    }

    private ErpAccountingEventDO buildEvent(ErpAccountingEventCreateReqBO request, ErpPostingRuleDO rule,
                                             ErpPeriodDO period, String idempotencyKey, BigDecimal docTotal) {
        ErpAccountingEventDO event = new ErpAccountingEventDO();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(request.getEventType());
        event.setSourceDocType(request.getSourceDocType());
        event.setSourceDocId(request.getSourceDocId());
        event.setSourceDocNo(StrUtil.blankToDefault(request.getSourceDocNo(), ""));
        event.setSourceDocLineId(defaultZero(request.getSourceDocLineId()));
        event.setSourceRevision(defaultZero(request.getSourceRevision()));
        event.setSequenceNo(defaultZero(request.getSequenceNo()));
        event.setEventDate(request.getEventDate());
        event.setAccountingDate(request.getAccountingDate());
        event.setPeriodId(period.getId());
        event.setPeriodCode(period.getCode());
        event.setAmountWithoutTax(defaultZero(request.getAmountWithoutTax()));
        event.setTaxAmount(defaultZero(request.getTaxAmount()));
        event.setAmountWithTax(defaultZero(request.getAmountWithTax()));
        event.setCostAmount(defaultZero(request.getCostAmount()));
        event.setDocTotalAmount(docTotal);
        event.setCurrency(StrUtil.blankToDefault(request.getCurrency(), DEFAULT_CURRENCY).toLowerCase());
        event.setExchangeRate(request.getExchangeRate() == null ? DEFAULT_EXCHANGE_RATE : request.getExchangeRate());
        event.setStatus(ErpAccountingEventStatusEnum.POSTING.getStatus());
        event.setIdempotencyKey(idempotencyKey);
        event.setRuleSetVersion(rule.getVersion());
        event.setPayloadHash(buildPayloadHash(request));
        event.setReversalOfEventId(defaultZero(request.getReversalOfEventId()));
        event.setRetryCount(0);
        event.setLastError("");
        event.setOccurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt());
        return event;
    }

    private Long createVoucher(ErpAccountingEventCreateReqBO request, ErpAccountingEventDO event,
                               ErpPeriodDO period, List<ErpGlVoucherEntryReqVO> entries) {
        ErpGlVoucherSaveReqVO voucher = new ErpGlVoucherSaveReqVO();
        voucher.setVoucherNo(generateVoucherNo(event));
        voucher.setVoucherDate(request.getAccountingDate());
        voucher.setPeriodId(period.getId());
        voucher.setVoucherType(40);
        voucher.setAttachmentCount(0);
        voucher.setSummary(request.getEventType() + " " + StrUtil.blankToDefault(request.getSourceDocNo(), ""));
        voucher.setSourceEventId(event.getId());
        voucher.setSourceDocType(event.getSourceDocType());
        voucher.setSourceDocId(event.getSourceDocId());
        voucher.setSourceDocNo(event.getSourceDocNo());
        voucher.setEntries(entries);
        Long voucherId = glVoucherService.createGlVoucher(voucher);
        if (voucherId == null || voucherId <= 0) {
            throw exception(ACCOUNTING_VOUCHER_CREATE_FAILED, "凭证服务未返回有效编号");
        }
        return voucherId;
    }

    private void insertPosting(ErpAccountingEventDO event, ErpPostingRuleDO rule, Long voucherId) {
        ErpAccountingPostingDO posting = new ErpAccountingPostingDO();
        posting.setEventId(event.getEventId());
        posting.setVoucherId(voucherId);
        posting.setPostingRuleId(rule.getId());
        posting.setRuleVersion(rule.getVersion());
        posting.setStatus(ErpAccountingPostingStatusEnum.POSTING.getStatus());
        posting.setAttemptNo(1);
        posting.setErrorCode("");
        posting.setErrorMessage("");
        accountingPostingMapper.insert(posting);
    }

    private Long getExistingVoucherId(ErpAccountingEventDO existing) {
        List<ErpAccountingPostingDO> postings = accountingPostingMapper.selectListByEventId(existing.getEventId());
        return postings.stream()
                .map(ErpAccountingPostingDO::getVoucherId)
                .filter(voucherId -> voucherId != null && voucherId > 0)
                .findFirst()
                .orElseThrow(() -> exception(ACCOUNTING_EVENT_ALREADY_PROCESSING, existing.getEventId()));
    }

    private String generateVoucherNo(ErpAccountingEventDO event) {
        return "记-" + event.getAccountingDate().format(PERIOD_CODE_FORMATTER) + "-"
                + event.getEventId().substring(0, 20).toUpperCase();
    }

    private String buildIdempotencyKey(ErpAccountingEventCreateReqBO request) {
        return DigestUtil.sha256Hex(String.join("|",
                request.getEventType(), request.getSourceDocType(), String.valueOf(request.getSourceDocId()),
                String.valueOf(defaultZero(request.getSourceDocLineId())),
                String.valueOf(defaultZero(request.getSourceRevision())),
                String.valueOf(defaultZero(request.getSequenceNo())),
                String.valueOf(defaultZero(request.getReversalOfEventId()))));
    }

    private String buildPayloadHash(ErpAccountingEventCreateReqBO request) {
        return DigestUtil.sha256Hex(String.join("|",
                String.valueOf(request.getEventDate()), String.valueOf(request.getAccountingDate()),
                defaultZero(request.getAmountWithoutTax()).stripTrailingZeros().toPlainString(),
                defaultZero(request.getTaxAmount()).stripTrailingZeros().toPlainString(),
                defaultZero(request.getAmountWithTax()).stripTrailingZeros().toPlainString(),
                defaultZero(request.getCostAmount()).stripTrailingZeros().toPlainString(),
                StrUtil.blankToDefault(request.getCurrency(), DEFAULT_CURRENCY).toLowerCase(),
                (request.getExchangeRate() == null ? DEFAULT_EXCHANGE_RATE : request.getExchangeRate())
                        .stripTrailingZeros().toPlainString()));
    }

    private BigDecimal sumDebit(List<ErpGlVoucherEntryReqVO> entries) {
        return entries.stream().map(entry -> defaultZero(entry.getDebitAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCredit(List<ErpGlVoucherEntryReqVO> entries) {
        return entries.stream().map(entry -> defaultZero(entry.getCreditAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private static int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

}

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
        validateRequest(request);
        String idempotencyKey = buildIdempotencyKey(request);
        ErpAccountingEventDO existing = accountingEventMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return getExistingVoucherId(existing);
        }

        ErpPostingRuleDO rule = selectPostingRule(request.getEventType());
        List<ErpPostingRuleLineDO> ruleLines = postingRuleLineMapper.selectListByRuleId(rule.getId());
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
        ErpAccountingEventDO event = buildEvent(request, rule, period, idempotencyKey);
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
        validateReverseRequest(request);
        Long sourceDocLineId = defaultZero(request.getSourceDocLineId());
        Integer sourceRevision = defaultZero(request.getSourceRevision());
        ErpAccountingEventDO original = accountingEventMapper.selectBySource(request.getEventType(),
                request.getSourceDocType(), request.getSourceDocId(), sourceDocLineId, sourceRevision, 0L);
        if (original == null || original.getId() == null) {
            throw exception(ACCOUNTING_EVENT_NOT_FOUND,
                    request.getSourceDocType() + ":" + request.getSourceDocId());
        }
        if (!ErpAccountingEventStatusEnum.POSTED.getStatus().equals(original.getStatus())) {
            throw exception(ACCOUNTING_EVENT_NOT_POSTED, original.getEventId());
        }
        return postEvent(ErpAccountingEventCreateReqBO.builder()
                .eventType(original.getEventType())
                .sourceDocType(original.getSourceDocType())
                .sourceDocId(original.getSourceDocId())
                .sourceDocNo(original.getSourceDocNo())
                .sourceDocLineId(original.getSourceDocLineId())
                .sourceRevision(original.getSourceRevision())
                .eventDate(request.getEventDate() == null ? original.getEventDate() : request.getEventDate())
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
    }

    private ErpPostingRuleDO selectPostingRule(String eventType) {
        List<ErpPostingRuleDO> effectiveRules = selectEffectiveRules(eventType,
                postingRuleMapper.selectListByEventType(eventType));
        if (effectiveRules.isEmpty() && !isDefaultTenant()) {
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

    private boolean isDefaultTenant() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null || tenantId == 0L;
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
            if (account == null && !isDefaultTenant()) {
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
        return period;
    }

    private ErpAccountingEventDO buildEvent(ErpAccountingEventCreateReqBO request, ErpPostingRuleDO rule,
                                             ErpPeriodDO period, String idempotencyKey) {
        ErpAccountingEventDO event = new ErpAccountingEventDO();
        event.setEventId(UUID.randomUUID().toString().replace("-", ""));
        event.setEventType(request.getEventType());
        event.setSourceDocType(request.getSourceDocType());
        event.setSourceDocId(request.getSourceDocId());
        event.setSourceDocNo(StrUtil.blankToDefault(request.getSourceDocNo(), ""));
        event.setSourceDocLineId(defaultZero(request.getSourceDocLineId()));
        event.setSourceRevision(defaultZero(request.getSourceRevision()));
        event.setEventDate(request.getEventDate());
        event.setAccountingDate(request.getAccountingDate());
        event.setPeriodId(period.getId());
        event.setPeriodCode(period.getCode());
        event.setAmountWithoutTax(defaultZero(request.getAmountWithoutTax()));
        event.setTaxAmount(defaultZero(request.getTaxAmount()));
        event.setAmountWithTax(defaultZero(request.getAmountWithTax()));
        event.setCostAmount(defaultZero(request.getCostAmount()));
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

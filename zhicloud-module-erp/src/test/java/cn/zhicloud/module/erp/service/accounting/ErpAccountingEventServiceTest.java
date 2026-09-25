package cn.zhicloud.module.erp.service.accounting;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingEventDO;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.erp.enums.ErrorCodeConstants;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingEventStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodStatusEnum;
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
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.finance.ErpGlVoucherService;
import cn.zhicloud.module.erp.service.finance.ErpPeriodService;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpAccountingEventServiceTest {

    @Mock
    private ErpAccountingEventMapper accountingEventMapper;
    @Mock
    private ErpAccountingPostingMapper accountingPostingMapper;
    @Mock
    private ErpPostingRuleMapper postingRuleMapper;
    @Mock
    private ErpPostingRuleLineMapper postingRuleLineMapper;
    @Mock
    private ErpGlAccountMapper glAccountMapper;
    @Mock
    private ErpGlVoucherService glVoucherService;
    @Mock
    private ErpPeriodService periodService;

    private ErpAccountingEventServiceImpl accountingEventService;

    @BeforeEach
    void setUp() {
        accountingEventService = new ErpAccountingEventServiceImpl();
        ReflectionTestUtils.setField(accountingEventService, "accountingEventMapper", accountingEventMapper);
        ReflectionTestUtils.setField(accountingEventService, "accountingPostingMapper", accountingPostingMapper);
        ReflectionTestUtils.setField(accountingEventService, "postingRuleMapper", postingRuleMapper);
        ReflectionTestUtils.setField(accountingEventService, "postingRuleLineMapper", postingRuleLineMapper);
        ReflectionTestUtils.setField(accountingEventService, "glAccountMapper", glAccountMapper);
        ReflectionTestUtils.setField(accountingEventService, "glVoucherService", glVoucherService);
        ReflectionTestUtils.setField(accountingEventService, "periodService", periodService);
    }

    @Test
    void postEventCreatesBalancedVoucherAndPosting() {
        ErpPostingRuleDO rule = new ErpPostingRuleDO().setId(10L).setRuleCode("PURCHASE_ACCRUAL")
                .setVersion("v1").setEventType("PURCHASE_RECEIPT_ACCRUAL").setEnabled(true)
                .setEffectiveFrom(LocalDateTime.now().minusDays(1));
        when(postingRuleMapper.selectListByEventType("PURCHASE_RECEIPT_ACCRUAL")).thenReturn(List.of(rule));
        when(postingRuleLineMapper.selectListByRuleId(10L)).thenReturn(List.of(
                ruleLine(1, 10, "1403", "amountWithoutTax"),
                ruleLine(2, 10, "22210101", "taxAmount"),
                ruleLine(3, 20, "2202", "amountWithTax")));
        when(glAccountMapper.selectByCode("1403")).thenReturn(account(1403L, "1403", "原材料"));
        when(glAccountMapper.selectByCode("22210101")).thenReturn(account(22210101L, "22210101", "进项税额"));
        when(glAccountMapper.selectByCode("2202")).thenReturn(account(2202L, "2202", "应付账款"));
        when(periodService.getPeriodByCode("202609")).thenReturn(new ErpPeriodDO().setId(202609L)
                .setCode("202609").setYear(2026).setMonth(9)
                .setStatus(ErpPeriodStatusEnum.OPEN.getStatus()));
        when(glVoucherService.createGlVoucher(any())).thenReturn(501L);
        doAnswer(invocation -> {
            ErpAccountingEventDO event = invocation.getArgument(0);
            event.setId(100L);
            return 1;
        }).when(accountingEventMapper).insert(any(ErpAccountingEventDO.class));

        Long voucherId = accountingEventService.postEvent(purchaseReceiptRequest());

        assertEquals(501L, voucherId);
        ArgumentCaptor<ErpGlVoucherSaveReqVO> voucherCaptor = ArgumentCaptor.forClass(ErpGlVoucherSaveReqVO.class);
        verify(glVoucherService).createGlVoucher(voucherCaptor.capture());
        ErpGlVoucherSaveReqVO voucher = voucherCaptor.getValue();
        BigDecimal debitTotal = voucher.getEntries().stream()
                .map(entry -> entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = voucher.getEntries().stream()
                .map(entry -> entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1130.0000"), debitTotal);
        assertEquals(new BigDecimal("1130.0000"), creditTotal);
        assertEquals(3, voucher.getEntries().size());
        assertEquals(202609L, voucher.getPeriodId());
        assertEquals(LocalDate.of(2026, 9, 25), voucher.getVoucherDate());
        assertEquals(100L, voucher.getSourceEventId());
        assertEquals("PURCHASE_IN", voucher.getSourceDocType());
        assertEquals(99L, voucher.getSourceDocId());
        assertNotNull(voucher.getVoucherNo());

        ArgumentCaptor<ErpAccountingPostingDO> postingCaptor = ArgumentCaptor.forClass(ErpAccountingPostingDO.class);
        verify(accountingPostingMapper).insert(postingCaptor.capture());
        assertEquals(501L, postingCaptor.getValue().getVoucherId());
        assertEquals(10, postingCaptor.getValue().getStatus());
    }

    @Test
    void postEventFallsBackToDefaultRuleAndAccountsWhenTenantHasNoCustomConfiguration() {
        TenantContextHolder.setTenantId(7L);
        try {
            ErpPostingRuleDO defaultRule = new ErpPostingRuleDO().setId(10L).setRuleCode("ERP_PURCHASE_RECEIPT_ACCRUAL")
                    .setVersion("v1").setEventType("PURCHASE_RECEIPT_ACCRUAL").setEnabled(true)
                    .setEffectiveFrom(LocalDateTime.now().minusDays(1));
            when(postingRuleMapper.selectListByEventType("PURCHASE_RECEIPT_ACCRUAL"))
                    .thenReturn(List.of()).thenReturn(List.of(defaultRule));
            when(postingRuleLineMapper.selectListByRuleId(10L)).thenReturn(List.of(
                    ruleLine(1, 10, "1403", "amountWithoutTax"),
                    ruleLine(2, 10, "22210101", "taxAmount"),
                    ruleLine(3, 20, "2202", "amountWithTax")));
            when(glAccountMapper.selectByCode("1403")).thenReturn(null)
                    .thenReturn(account(1403L, "1403", "原材料"));
            when(glAccountMapper.selectByCode("22210101")).thenReturn(null)
                    .thenReturn(account(22210101L, "22210101", "进项税额"));
            when(glAccountMapper.selectByCode("2202")).thenReturn(null)
                    .thenReturn(account(2202L, "2202", "应付账款"));
            when(periodService.getPeriodByCode("202609")).thenReturn(new ErpPeriodDO().setId(202609L)
                    .setCode("202609").setYear(2026).setMonth(9)
                .setStatus(ErpPeriodStatusEnum.OPEN.getStatus()));
            when(glVoucherService.createGlVoucher(any())).thenReturn(501L);
            doAnswer(invocation -> {
                ErpAccountingEventDO event = invocation.getArgument(0);
                event.setId(100L);
                return 1;
            }).when(accountingEventMapper).insert(any(ErpAccountingEventDO.class));

            assertEquals(501L, accountingEventService.postEvent(purchaseReceiptRequest()));

            verify(postingRuleMapper, times(2)).selectListByEventType("PURCHASE_RECEIPT_ACCRUAL");
            verify(glAccountMapper, times(2)).selectByCode("1403");
            verify(glAccountMapper, times(2)).selectByCode("22210101");
            verify(glAccountMapper, times(2)).selectByCode("2202");
        } finally {
            TenantContextHolder.clear();
        }
    }
    @Test
    void reverseEventCreatesOppositeVoucherLinkedToOriginalEvent() {
        ErpAccountingEventDO original = new ErpAccountingEventDO().setId(100L)
                .setEventId("original-event").setEventType("PURCHASE_RECEIPT_ACCRUAL")
                .setSourceDocType("PURCHASE_IN").setSourceDocId(99L).setSourceDocNo("PI20260925001")
                .setSourceDocLineId(0L).setSourceRevision(1).setStatus(20)
                .setAmountWithoutTax(new BigDecimal("1000.0000"))
                .setTaxAmount(new BigDecimal("130.0000"))
                .setAmountWithTax(new BigDecimal("1130.0000"))
                .setCurrency("cny").setExchangeRate(BigDecimal.ONE);
        when(accountingEventMapper.selectBySource("PURCHASE_RECEIPT_ACCRUAL", "PURCHASE_IN",
                99L, 0L, 1, 0L)).thenReturn(original);
        when(accountingEventMapper.selectById(100L)).thenReturn(original);
        ErpPostingRuleDO rule = new ErpPostingRuleDO().setId(10L).setRuleCode("PURCHASE_ACCRUAL")
                .setVersion("v1").setEventType("PURCHASE_RECEIPT_ACCRUAL").setEnabled(true)
                .setEffectiveFrom(LocalDateTime.now().minusDays(1));
        when(postingRuleMapper.selectListByEventType("PURCHASE_RECEIPT_ACCRUAL")).thenReturn(List.of(rule));
        when(postingRuleLineMapper.selectListByRuleId(10L)).thenReturn(List.of(
                ruleLine(1, 10, "1403", "amountWithoutTax"),
                ruleLine(2, 10, "22210101", "taxAmount"),
                ruleLine(3, 20, "2202", "amountWithTax")));
        when(glAccountMapper.selectByCode("1403")).thenReturn(account(1403L, "1403", "原材料"));
        when(glAccountMapper.selectByCode("22210101")).thenReturn(account(22210101L, "22210101", "进项税额"));
        when(glAccountMapper.selectByCode("2202")).thenReturn(account(2202L, "2202", "应付账款"));
        when(periodService.getPeriodByCode("202609")).thenReturn(new ErpPeriodDO().setId(202609L)
                .setCode("202609").setYear(2026).setMonth(9)
                .setStatus(ErpPeriodStatusEnum.OPEN.getStatus()));
        when(glVoucherService.createGlVoucher(any())).thenReturn(502L);
        doAnswer(invocation -> {
            ErpAccountingEventDO event = invocation.getArgument(0);
            event.setId(200L);
            return 1;
        }).when(accountingEventMapper).insert(any(ErpAccountingEventDO.class));

        Long voucherId = accountingEventService.reverseEvent(ErpAccountingEventReverseReqBO.builder()
                .eventType("PURCHASE_RECEIPT_ACCRUAL")
                .sourceDocType("PURCHASE_IN")
                .sourceDocId(99L)
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(LocalDate.of(2026, 9, 25))
                .accountingDate(LocalDate.of(2026, 9, 26))
                .occurredAt(LocalDateTime.of(2026, 9, 26, 9, 0))
                .build());

        assertEquals(502L, voucherId);
        ArgumentCaptor<ErpAccountingEventDO> eventCaptor = ArgumentCaptor.forClass(ErpAccountingEventDO.class);
        verify(accountingEventMapper).insert(eventCaptor.capture());
        ErpAccountingEventDO reversal = eventCaptor.getValue();
        assertEquals(new BigDecimal("-1000.0000"), reversal.getAmountWithoutTax());
        assertEquals(new BigDecimal("-130.0000"), reversal.getTaxAmount());
        assertEquals(new BigDecimal("-1130.0000"), reversal.getAmountWithTax());
        assertEquals(100L, reversal.getReversalOfEventId());
        assertEquals("cny", reversal.getCurrency());

        ArgumentCaptor<ErpGlVoucherSaveReqVO> voucherCaptor =
                ArgumentCaptor.forClass(ErpGlVoucherSaveReqVO.class);
        verify(glVoucherService).createGlVoucher(voucherCaptor.capture());
        ErpGlVoucherSaveReqVO voucher = voucherCaptor.getValue();
        assertEquals(LocalDate.of(2026, 9, 26), voucher.getVoucherDate());
        assertEquals(3, voucher.getEntries().size());
        assertEquals(new BigDecimal("1130.0000"), voucher.getEntries().stream()
                .map(entry -> entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("1130.0000"), voucher.getEntries().stream()
                .map(entry -> entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("1130.0000"), voucher.getEntries().stream()
                .filter(entry -> entry.getAccountId().equals(2202L)).findFirst().orElseThrow().getDebitAmount());
        assertEquals(new BigDecimal("1000.0000"), voucher.getEntries().stream()
                .filter(entry -> entry.getAccountId().equals(1403L)).findFirst().orElseThrow().getCreditAmount());
    }
    @Test
    void postEventSupportsCostAmountExpressionForSalesShipment() {
        ErpPostingRuleDO rule = new ErpPostingRuleDO().setId(30L).setRuleCode("ERP_SALES_SHIPMENT_POSTED")
                .setVersion("v1").setEventType("SALES_SHIPMENT_POSTED").setEnabled(true)
                .setEffectiveFrom(LocalDateTime.now().minusDays(1));
        when(postingRuleMapper.selectListByEventType("SALES_SHIPMENT_POSTED")).thenReturn(List.of(rule));
        when(postingRuleLineMapper.selectListByRuleId(30L)).thenReturn(List.of(
                ruleLine(1, 10, "1122", "amountWithTax"),
                ruleLine(2, 10, "6401", "costAmount"),
                ruleLine(3, 20, "6001", "amountWithoutTax"),
                ruleLine(4, 20, "22210102", "taxAmount"),
                ruleLine(5, 20, "1405", "costAmount")));
        when(glAccountMapper.selectByCode("1122")).thenReturn(account(1122L, "1122", "应收账款"));
        when(glAccountMapper.selectByCode("6401")).thenReturn(account(6401L, "6401", "主营业务成本"));
        when(glAccountMapper.selectByCode("6001")).thenReturn(account(6001L, "6001", "主营业务收入"));
        when(glAccountMapper.selectByCode("22210102")).thenReturn(account(22210102L, "22210102", "销项税额"));
        when(glAccountMapper.selectByCode("1405")).thenReturn(account(1405L, "1405", "库存商品"));
        when(periodService.getPeriodByCode("202609")).thenReturn(new ErpPeriodDO().setId(202609L)
                .setCode("202609").setYear(2026).setMonth(9)
                .setStatus(ErpPeriodStatusEnum.OPEN.getStatus()));
        when(glVoucherService.createGlVoucher(any())).thenReturn(601L);
        doAnswer(invocation -> {
            ErpAccountingEventDO event = invocation.getArgument(0);
            event.setId(300L);
            return 1;
        }).when(accountingEventMapper).insert(any(ErpAccountingEventDO.class));

        Long voucherId = accountingEventService.postEvent(salesShipmentRequest());

        assertEquals(601L, voucherId);
        ArgumentCaptor<ErpAccountingEventDO> eventCaptor = ArgumentCaptor.forClass(ErpAccountingEventDO.class);
        verify(accountingEventMapper).insert(eventCaptor.capture());
        assertEquals(new BigDecimal("1500.0000"), eventCaptor.getValue().getCostAmount());

        ArgumentCaptor<ErpGlVoucherSaveReqVO> voucherCaptor = ArgumentCaptor.forClass(ErpGlVoucherSaveReqVO.class);
        verify(glVoucherService).createGlVoucher(voucherCaptor.capture());
        ErpGlVoucherSaveReqVO voucher = voucherCaptor.getValue();
        assertEquals(5, voucher.getEntries().size());
        assertEquals(new BigDecimal("3760.0000"), voucher.getEntries().stream()
                .map(entry -> entry.getDebitAmount() == null ? BigDecimal.ZERO : entry.getDebitAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("3760.0000"), voucher.getEntries().stream()
                .map(entry -> entry.getCreditAmount() == null ? BigDecimal.ZERO : entry.getCreditAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        assertEquals(new BigDecimal("1500.0000"), voucher.getEntries().stream()
                .filter(entry -> entry.getAccountId().equals(6401L)).findFirst().orElseThrow().getDebitAmount());
        assertEquals(new BigDecimal("1500.0000"), voucher.getEntries().stream()
                .filter(entry -> entry.getAccountId().equals(1405L)).findFirst().orElseThrow().getCreditAmount());
    }

    @Test
    void postEventRejectsClosedAccountingPeriod() {
        stubPurchaseAccrualRuleAndAccounts();
        when(periodService.getPeriodByCode("202609")).thenReturn(
                period(202609L, "202609", 9, ErpPeriodStatusEnum.CLOSED.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> accountingEventService.postEvent(purchaseReceiptRequest()));

        assertEquals(ErrorCodeConstants.GL_VOUCHER_PERIOD_CLOSED.getCode(), exception.getCode());
        verify(accountingEventMapper, never()).insert(any(ErpAccountingEventDO.class));
        verify(glVoucherService, never()).createGlVoucher(any());
    }

    @Test
    void postEventRejectsClosingAccountingPeriod() {
        stubPurchaseAccrualRuleAndAccounts();
        when(periodService.getPeriodByCode("202609")).thenReturn(
                period(202609L, "202609", 9, ErpPeriodStatusEnum.CLOSING.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> accountingEventService.postEvent(purchaseReceiptRequest()));

        assertEquals(ErrorCodeConstants.GL_VOUCHER_PERIOD_CLOSED.getCode(), exception.getCode());
        verify(accountingEventMapper, never()).insert(any(ErpAccountingEventDO.class));
    }

    @Test
    void postEventRejectsUnknownReversalReference() {
        when(accountingEventMapper.selectById(999L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> accountingEventService.postEvent(referencedReversalRequest(999L)));

        assertEquals(ErrorCodeConstants.ACCOUNTING_EVENT_NOT_FOUND.getCode(), exception.getCode());
        verify(accountingEventMapper, never()).insert(any(ErpAccountingEventDO.class));
    }

    @Test
    void postEventRejectsReversalReferenceWhenOriginalEventIsNotPosted() {
        when(accountingEventMapper.selectById(100L)).thenReturn(new ErpAccountingEventDO().setId(100L)
                .setEventId("original-event").setStatus(ErpAccountingEventStatusEnum.POSTING.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> accountingEventService.postEvent(referencedReversalRequest(100L)));

        assertEquals(ErrorCodeConstants.ACCOUNTING_EVENT_NOT_POSTED.getCode(), exception.getCode());
        verify(accountingEventMapper, never()).insert(any(ErpAccountingEventDO.class));
    }

    @Test
    void reverseEventMarksOriginalEventAsReversed() {
        ErpAccountingEventDO original = purchaseReceiptOriginalEvent(
                ErpAccountingEventStatusEnum.POSTED.getStatus());
        when(accountingEventMapper.selectBySource("PURCHASE_RECEIPT_ACCRUAL", "PURCHASE_IN",
                99L, 0L, 1, 0L)).thenReturn(original);
        when(accountingEventMapper.selectById(100L)).thenReturn(original);
        stubPurchaseAccrualRuleAndAccounts();
        when(periodService.getPeriodByCode("202609")).thenReturn(
                period(202609L, "202609", 9, ErpPeriodStatusEnum.OPEN.getStatus()));
        when(glVoucherService.createGlVoucher(any())).thenReturn(503L);
        doAnswer(invocation -> {
            ErpAccountingEventDO event = invocation.getArgument(0);
            event.setId(200L);
            return 1;
        }).when(accountingEventMapper).insert(any(ErpAccountingEventDO.class));

        assertEquals(503L, accountingEventService.reverseEvent(ErpAccountingEventReverseReqBO.builder()
                .eventType("PURCHASE_RECEIPT_ACCRUAL")
                .sourceDocType("PURCHASE_IN")
                .sourceDocId(99L)
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .accountingDate(LocalDate.of(2026, 9, 26))
                .occurredAt(LocalDateTime.of(2026, 9, 26, 9, 0))
                .build()));

        ArgumentCaptor<ErpAccountingEventDO> updateCaptor = ArgumentCaptor.forClass(ErpAccountingEventDO.class);
        verify(accountingEventMapper).updateById(updateCaptor.capture());
        assertEquals(100L, updateCaptor.getValue().getId());
        assertEquals(ErpAccountingEventStatusEnum.REVERSED.getStatus(), updateCaptor.getValue().getStatus());
    }

    @Test
    void reverseEventRejectsAlreadyReversedOriginalEvent() {
        when(accountingEventMapper.selectBySource("PURCHASE_RECEIPT_ACCRUAL", "PURCHASE_IN",
                99L, 0L, 1, 0L)).thenReturn(purchaseReceiptOriginalEvent(
                ErpAccountingEventStatusEnum.REVERSED.getStatus()));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> accountingEventService.reverseEvent(ErpAccountingEventReverseReqBO.builder()
                        .eventType("PURCHASE_RECEIPT_ACCRUAL")
                        .sourceDocType("PURCHASE_IN")
                        .sourceDocId(99L)
                        .sourceDocLineId(0L)
                        .sourceRevision(1)
                        .accountingDate(LocalDate.of(2026, 9, 26))
                        .build()));

        assertEquals(ErrorCodeConstants.ACCOUNTING_EVENT_NOT_POSTED.getCode(), exception.getCode());
        verify(accountingEventMapper, never()).insert(any(ErpAccountingEventDO.class));
        verify(accountingEventMapper, never()).updateById(any(ErpAccountingEventDO.class));
    }

    @Test
    void reverseEventBooksCrossPeriodReversalIntoCurrentPeriodOnly() {
        ErpAccountingEventDO original = purchaseReceiptOriginalEvent(
                ErpAccountingEventStatusEnum.POSTED.getStatus());
        when(accountingEventMapper.selectBySource("PURCHASE_RECEIPT_ACCRUAL", "PURCHASE_IN",
                99L, 0L, 1, 0L)).thenReturn(original);
        when(accountingEventMapper.selectById(100L)).thenReturn(original);
        stubPurchaseAccrualRuleAndAccounts();
        when(periodService.getPeriodByCode("202610")).thenReturn(
                period(202610L, "202610", 10, ErpPeriodStatusEnum.OPEN.getStatus()));
        when(glVoucherService.createGlVoucher(any())).thenReturn(504L);
        doAnswer(invocation -> {
            ErpAccountingEventDO event = invocation.getArgument(0);
            event.setId(300L);
            return 1;
        }).when(accountingEventMapper).insert(any(ErpAccountingEventDO.class));

        accountingEventService.reverseEvent(ErpAccountingEventReverseReqBO.builder()
                .eventType("PURCHASE_RECEIPT_ACCRUAL")
                .sourceDocType("PURCHASE_IN")
                .sourceDocId(99L)
                .sourceDocLineId(0L)
                .sourceRevision(1)
                .eventDate(LocalDate.of(2026, 10, 5))
                .accountingDate(LocalDate.of(2026, 10, 5))
                .occurredAt(LocalDateTime.of(2026, 10, 5, 9, 0))
                .build());

        ArgumentCaptor<ErpAccountingEventDO> eventCaptor = ArgumentCaptor.forClass(ErpAccountingEventDO.class);
        verify(accountingEventMapper).insert(eventCaptor.capture());
        ErpAccountingEventDO reversal = eventCaptor.getValue();
        assertEquals(LocalDate.of(2026, 10, 5), reversal.getEventDate());
        assertEquals(LocalDate.of(2026, 10, 5), reversal.getAccountingDate());
        assertEquals("202610", reversal.getPeriodCode());
        assertEquals(100L, reversal.getReversalOfEventId());
        // 原事件的业务日期不得被跨期冲销改写
        assertEquals(LocalDate.of(2026, 9, 25), original.getEventDate());
    }

    private void stubPurchaseAccrualRuleAndAccounts() {
        ErpPostingRuleDO rule = new ErpPostingRuleDO().setId(10L).setRuleCode("PURCHASE_ACCRUAL")
                .setVersion("v1").setEventType("PURCHASE_RECEIPT_ACCRUAL").setEnabled(true)
                .setEffectiveFrom(LocalDateTime.now().minusDays(1));
        when(postingRuleMapper.selectListByEventType("PURCHASE_RECEIPT_ACCRUAL")).thenReturn(List.of(rule));
        when(postingRuleLineMapper.selectListByRuleId(10L)).thenReturn(List.of(
                ruleLine(1, 10, "1403", "amountWithoutTax"),
                ruleLine(2, 10, "22210101", "taxAmount"),
                ruleLine(3, 20, "2202", "amountWithTax")));
        when(glAccountMapper.selectByCode("1403")).thenReturn(account(1403L, "1403", "原材料"));
        when(glAccountMapper.selectByCode("22210101")).thenReturn(account(22210101L, "22210101", "进项税额"));
        when(glAccountMapper.selectByCode("2202")).thenReturn(account(2202L, "2202", "应付账款"));
    }

    private static ErpAccountingEventCreateReqBO referencedReversalRequest(Long reversalOfEventId) {
        return ErpAccountingEventCreateReqBO.builder()
                .eventType("RETURN_OR_RED_INVOICE_POSTED")
                .sourceDocType("PURCHASE_IN")
                .sourceDocId(99L)
                .sourceDocNo("PI20260925001")
                .sourceRevision(1)
                .eventDate(LocalDate.of(2026, 9, 25))
                .accountingDate(LocalDate.of(2026, 9, 25))
                .amountWithoutTax(new BigDecimal("-1000.0000"))
                .taxAmount(new BigDecimal("-130.0000"))
                .amountWithTax(new BigDecimal("-1130.0000"))
                .currency("cny")
                .exchangeRate(BigDecimal.ONE)
                .reversalOfEventId(reversalOfEventId)
                .occurredAt(LocalDateTime.of(2026, 9, 25, 9, 30))
                .build();
    }

    private static ErpAccountingEventDO purchaseReceiptOriginalEvent(Integer status) {
        return new ErpAccountingEventDO().setId(100L)
                .setEventId("original-event").setEventType("PURCHASE_RECEIPT_ACCRUAL")
                .setSourceDocType("PURCHASE_IN").setSourceDocId(99L).setSourceDocNo("PI20260925001")
                .setSourceDocLineId(0L).setSourceRevision(1).setStatus(status)
                .setEventDate(LocalDate.of(2026, 9, 25))
                .setAccountingDate(LocalDate.of(2026, 9, 25))
                .setAmountWithoutTax(new BigDecimal("1000.0000"))
                .setTaxAmount(new BigDecimal("130.0000"))
                .setAmountWithTax(new BigDecimal("1130.0000"))
                .setCurrency("cny").setExchangeRate(BigDecimal.ONE);
    }

    private static ErpPeriodDO period(Long id, String code, int month, Integer status) {
        return new ErpPeriodDO().setId(id).setCode(code).setYear(2026).setMonth(month).setStatus(status);
    }
    private static ErpAccountingEventCreateReqBO salesShipmentRequest() {
        return ErpAccountingEventCreateReqBO.builder()
                .eventType("SALES_SHIPMENT_POSTED")
                .sourceDocType("SALE_OUT")
                .sourceDocId(88L)
                .sourceDocNo("SO20260925001")
                .sourceRevision(1)
                .eventDate(LocalDate.of(2026, 9, 25))
                .accountingDate(LocalDate.of(2026, 9, 25))
                .amountWithoutTax(new BigDecimal("2000.0000"))
                .taxAmount(new BigDecimal("260.0000"))
                .amountWithTax(new BigDecimal("2260.0000"))
                .costAmount(new BigDecimal("1500.0000"))
                .currency("cny")
                .exchangeRate(BigDecimal.ONE)
                .occurredAt(LocalDateTime.of(2026, 9, 25, 10, 0))
                .build();
    }
    private static ErpAccountingEventCreateReqBO purchaseReceiptRequest() {
        return ErpAccountingEventCreateReqBO.builder()
                .eventType("PURCHASE_RECEIPT_ACCRUAL")
                .sourceDocType("PURCHASE_IN")
                .sourceDocId(99L)
                .sourceDocNo("PI20260925001")
                .sourceRevision(1)
                .eventDate(LocalDate.of(2026, 9, 25))
                .accountingDate(LocalDate.of(2026, 9, 25))
                .amountWithoutTax(new BigDecimal("1000.0000"))
                .taxAmount(new BigDecimal("130.0000"))
                .amountWithTax(new BigDecimal("1130.0000"))
                .currency("cny")
                .exchangeRate(BigDecimal.ONE)
                .occurredAt(LocalDateTime.of(2026, 9, 25, 9, 30))
                .build();
    }

    private static ErpPostingRuleLineDO ruleLine(int lineNo, int entrySide, String accountCode,
                                                 String amountExpression) {
        return new ErpPostingRuleLineDO().setLineNo(lineNo).setEntrySide(entrySide)
                .setAccountCode(accountCode).setAmountExpression(amountExpression);
    }

    private static ErpGlAccountDO account(Long id, String code, String name) {
        return new ErpGlAccountDO().setId(id).setCode(code).setName(name).setIsLeaf(true);
    }
}

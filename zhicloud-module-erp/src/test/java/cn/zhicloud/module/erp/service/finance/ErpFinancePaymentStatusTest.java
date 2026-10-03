package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpFinancePaymentStatusTest {

    @Mock
    private ErpFinancePaymentMapper financePaymentMapper;
    @Mock
    private ErpAccountingEventService accountingEventService;

    private ErpFinancePaymentServiceImpl financePaymentService;

    @BeforeEach
    void setUp() {
        financePaymentService = new ErpFinancePaymentServiceImpl();
        ReflectionTestUtils.setField(financePaymentService, "financePaymentMapper", financePaymentMapper);
        ReflectionTestUtils.setField(financePaymentService, "accountingEventService", accountingEventService);
    }

    @Test
    void approvalCreatesPaymentPostedEvent() {
        ErpFinancePaymentDO payment = payment(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(new BigDecimal("500.0000"));
        when(financePaymentMapper.selectById(1L)).thenReturn(payment);
        when(financePaymentMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(1);

        assertDoesNotThrow(() -> financePaymentService.updateFinancePaymentStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        ArgumentCaptor<ErpAccountingEventCreateReqBO> eventCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventCreateReqBO.class);
        verify(accountingEventService).postEvent(eventCaptor.capture());
        ErpAccountingEventCreateReqBO event = eventCaptor.getValue();
        assertEquals("PAYMENT_POSTED", event.getEventType());
        assertEquals("FINANCE_PAYMENT", event.getSourceDocType());
        assertEquals(1L, event.getSourceDocId());
        assertEquals("FP001", event.getSourceDocNo());
        assertEquals(1, event.getSourceRevision());
        assertEquals(new BigDecimal("500.0000"), event.getAmountWithoutTax());
        assertEquals(BigDecimal.ZERO, event.getTaxAmount());
        assertEquals(new BigDecimal("500.0000"), event.getAmountWithTax());
        assertEquals(LocalDate.of(2026, 9, 25), event.getEventDate());
        assertEquals(LocalDate.of(2026, 9, 25), event.getAccountingDate());
    }

    @Test
    void repeatedApprovalDoesNotPostAgain() {
        when(financePaymentMapper.selectById(1L)).thenReturn(payment(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> financePaymentService.updateFinancePaymentStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(financePaymentMapper, never()).updateByIdAndStatus(any(), any(), any());
        verifyNoInteractions(accountingEventService);
    }

    @Test
    void unknownStatusIsRejectedBeforeDatabaseAccess() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> financePaymentService.updateFinancePaymentStatus(1L, 999));

        assertTrue(exception.getMessage().contains("审核状态"));
        verifyNoInteractions(financePaymentMapper, accountingEventService);
    }

    @Test
    void cancellingApprovedPaymentCreatesReversalEvent() {
        when(financePaymentMapper.selectById(1L)).thenReturn(payment(ErpAuditStatus.APPROVE.getStatus()));
        when(financePaymentMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any())).thenReturn(1);

        assertDoesNotThrow(() -> financePaymentService.updateFinancePaymentStatus(
                1L, ErpAuditStatus.PROCESS.getStatus()));

        ArgumentCaptor<ErpAccountingEventReverseReqBO> reversalCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventReverseReqBO.class);
        verify(accountingEventService).reverseEvent(reversalCaptor.capture());
        ErpAccountingEventReverseReqBO reversal = reversalCaptor.getValue();
        assertEquals("PAYMENT_POSTED", reversal.getEventType());
        assertEquals("FINANCE_PAYMENT", reversal.getSourceDocType());
        assertEquals(1L, reversal.getSourceDocId());
        assertEquals(0L, reversal.getSourceDocLineId());
        assertEquals(1, reversal.getSourceRevision());
        assertEquals(LocalDate.of(2026, 9, 25), reversal.getEventDate());
        assertEquals(LocalDate.now(), reversal.getAccountingDate());
        verify(accountingEventService, never()).postEvent(any(ErpAccountingEventCreateReqBO.class));
    }
    private static ErpFinancePaymentDO payment(Integer status) {
        return new ErpFinancePaymentDO().setId(1L).setNo("FP001").setStatus(status)
                .setPaymentTime(LocalDateTime.of(2026, 9, 25, 10, 0))
                .setPaymentPrice(new BigDecimal("500.0000"));
    }
}

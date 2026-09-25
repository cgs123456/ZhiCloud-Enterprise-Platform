package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 收款单审核状态行为测试，重点验证收款过账事件只生成一次。
 */
@ExtendWith(MockitoExtension.class)
class ErpFinanceReceiptStatusTest {

    @Mock
    private ErpFinanceReceiptMapper financeReceiptMapper;
    @Mock
    private ErpAccountingEventService accountingEventService;

    private ErpFinanceReceiptServiceImpl receiptService;

    @BeforeEach
    void setUp() {
        receiptService = new ErpFinanceReceiptServiceImpl();
        ReflectionTestUtils.setField(receiptService, "financeReceiptMapper", financeReceiptMapper);
        ReflectionTestUtils.setField(receiptService, "accountingEventService", accountingEventService);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotPostAccountingEvent() {
        when(financeReceiptMapper.selectById(1L)).thenReturn(receipt(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> receiptService.updateFinanceReceiptStatus(1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(financeReceiptMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verifyNoInteractions(accountingEventService);
    }

    @Test
    void approvalCreatesReceiptPostedEvent() {
        when(financeReceiptMapper.selectById(1L)).thenReturn(receipt(ErpAuditStatus.PROCESS.getStatus())
                .setReceiptTime(LocalDateTime.of(2026, 9, 26, 11, 0)));
        when(financeReceiptMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);

        assertDoesNotThrow(() -> receiptService.updateFinanceReceiptStatus(1L, ErpAuditStatus.APPROVE.getStatus()));

        ArgumentCaptor<ErpAccountingEventCreateReqBO> eventCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventCreateReqBO.class);
        verify(accountingEventService).postEvent(eventCaptor.capture());
        ErpAccountingEventCreateReqBO event = eventCaptor.getValue();
        assertEquals("RECEIPT_POSTED", event.getEventType());
        assertEquals("FINANCE_RECEIPT", event.getSourceDocType());
        assertEquals(1L, event.getSourceDocId());
        assertEquals("REC001", event.getSourceDocNo());
        assertEquals(1, event.getSourceRevision());
        assertEquals(new BigDecimal("2260.0000"), event.getAmountWithoutTax());
        assertEquals(BigDecimal.ZERO, event.getTaxAmount());
        assertEquals(new BigDecimal("2260.0000"), event.getAmountWithTax());
        assertEquals(BigDecimal.ZERO, event.getCostAmount());
        assertEquals(LocalDate.of(2026, 9, 26), event.getEventDate());
        assertEquals(LocalDate.of(2026, 9, 26), event.getAccountingDate());
    }

    @Test
    void cancellingApprovedReceiptCreatesReversalEvent() {
        when(financeReceiptMapper.selectById(1L)).thenReturn(receipt(ErpAuditStatus.APPROVE.getStatus())
                .setReceiptTime(LocalDateTime.of(2026, 9, 26, 11, 0)));
        when(financeReceiptMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.APPROVE.getStatus()), any()))
                .thenReturn(1);

        assertDoesNotThrow(() -> receiptService.updateFinanceReceiptStatus(1L, ErpAuditStatus.PROCESS.getStatus()));

        ArgumentCaptor<ErpAccountingEventReverseReqBO> reversalCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventReverseReqBO.class);
        verify(accountingEventService).reverseEvent(reversalCaptor.capture());
        ErpAccountingEventReverseReqBO reversal = reversalCaptor.getValue();
        assertEquals("RECEIPT_POSTED", reversal.getEventType());
        assertEquals("FINANCE_RECEIPT", reversal.getSourceDocType());
        assertEquals(1L, reversal.getSourceDocId());
        assertEquals(0L, reversal.getSourceDocLineId());
        assertEquals(1, reversal.getSourceRevision());
        assertEquals(LocalDate.of(2026, 9, 26), reversal.getEventDate());
        assertEquals(LocalDate.now(), reversal.getAccountingDate());
        verify(accountingEventService, never()).postEvent(any(ErpAccountingEventCreateReqBO.class));
    }

    private static ErpFinanceReceiptDO receipt(Integer status) {
        return new ErpFinanceReceiptDO().setId(1L).setNo("REC001").setStatus(status)
                .setTotalPrice(new BigDecimal("2260.0000"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setReceiptPrice(new BigDecimal("2260.0000"));
    }
}
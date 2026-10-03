package cn.zhicloud.module.erp.service.purchase;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.stock.ErpStockRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 采购入库审核状态行为测试，重点验证重复请求不会重复记账。
 */
@ExtendWith(MockitoExtension.class)
class ErpPurchaseInStatusTest {

    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpAccountingEventService accountingEventService;

    private ErpPurchaseInServiceImpl purchaseInService;

    @BeforeEach
    void setUp() {
        purchaseInService = new ErpPurchaseInServiceImpl();
        ReflectionTestUtils.setField(purchaseInService, "purchaseInMapper", purchaseInMapper);
        ReflectionTestUtils.setField(purchaseInService, "purchaseInItemMapper", purchaseInItemMapper);
        ReflectionTestUtils.setField(purchaseInService, "stockRecordService", stockRecordService);
        ReflectionTestUtils.setField(purchaseInService, "accountingEventService", accountingEventService);
    }

    @Test
    void updateStatusRejectsUnknownStatusBeforeDatabaseAccess() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> purchaseInService.updatePurchaseInStatus(1L, 999));

        assertTrue(exception.getMessage().contains("审核状态"));
        verifyNoInteractions(purchaseInMapper, purchaseInItemMapper, stockRecordService, accountingEventService);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotPostStockAgain() {
        when(purchaseInMapper.selectById(1L)).thenReturn(purchaseIn(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> purchaseInService.updatePurchaseInStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseInMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any());
        verifyNoInteractions(purchaseInItemMapper, stockRecordService, accountingEventService);
    }

    @Test
    void concurrentDuplicateApprovalReturnsWithoutPostingStockAgain() {
        when(purchaseInMapper.selectById(1L))
                .thenReturn(purchaseIn(ErpAuditStatus.PROCESS.getStatus()));
        when(purchaseInMapper.selectOneForUpdate(any(), eq(1L)))
                .thenReturn(purchaseIn(ErpAuditStatus.APPROVE.getStatus()));
        when(purchaseInMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(0);

        assertDoesNotThrow(() -> purchaseInService.updatePurchaseInStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseInMapper).selectOneForUpdate(any(), eq(1L));
        verifyNoInteractions(purchaseInItemMapper, stockRecordService, accountingEventService);
    }

    @Test
    void approvalCreatesPurchaseReceiptAccrualEvent() {
        ErpPurchaseInDO purchaseIn = purchaseIn(ErpAuditStatus.PROCESS.getStatus())
                .setTotalPrice(new BigDecimal("1130.0000"))
                .setTotalProductPrice(new BigDecimal("1000.0000"))
                .setTotalTaxPrice(new BigDecimal("130.0000"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setInTime(LocalDateTime.of(2026, 9, 25, 9, 30))
                .setCurrencyId(null)
                .setExchangeRate(BigDecimal.ONE);
        when(purchaseInMapper.selectById(1L)).thenReturn(purchaseIn);
        when(purchaseInMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(1);
        when(purchaseInItemMapper.selectListByInId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> purchaseInService.updatePurchaseInStatus(
                1L, ErpAuditStatus.APPROVE.getStatus()));

        ArgumentCaptor<ErpAccountingEventCreateReqBO> eventCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventCreateReqBO.class);
        verify(accountingEventService).postEvent(eventCaptor.capture());
        ErpAccountingEventCreateReqBO event = eventCaptor.getValue();
        assertEquals("PURCHASE_RECEIPT_ACCRUAL", event.getEventType());
        assertEquals("PURCHASE_IN", event.getSourceDocType());
        assertEquals(1L, event.getSourceDocId());
        assertEquals("PI001", event.getSourceDocNo());
        assertEquals(1, event.getSourceRevision());
        assertEquals(new BigDecimal("1000.0000"), event.getAmountWithoutTax());
        assertEquals(new BigDecimal("130.0000"), event.getTaxAmount());
        assertEquals(new BigDecimal("1130.0000"), event.getAmountWithTax());
        assertEquals(java.time.LocalDate.of(2026, 9, 25), event.getEventDate());
        assertEquals(java.time.LocalDate.of(2026, 9, 25), event.getAccountingDate());
    }

    @Test
    void cancellingApprovedPurchaseInCreatesReversalEvent() {
        when(purchaseInMapper.selectById(1L)).thenReturn(purchaseIn(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 9, 25, 9, 30)));
        when(purchaseInItemMapper.selectListByInId(1L)).thenReturn(List.of());
        when(purchaseInMapper.updateByIdAndStatus(eq(1L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any())).thenReturn(1);

        assertDoesNotThrow(() -> purchaseInService.updatePurchaseInStatus(
                1L, ErpAuditStatus.PROCESS.getStatus()));

        ArgumentCaptor<ErpAccountingEventReverseReqBO> reversalCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventReverseReqBO.class);
        verify(accountingEventService).reverseEvent(reversalCaptor.capture());
        ErpAccountingEventReverseReqBO reversal = reversalCaptor.getValue();
        assertEquals("PURCHASE_RECEIPT_ACCRUAL", reversal.getEventType());
        assertEquals("PURCHASE_IN", reversal.getSourceDocType());
        assertEquals(1L, reversal.getSourceDocId());
        assertEquals(0L, reversal.getSourceDocLineId());
        assertEquals(1, reversal.getSourceRevision());
        assertEquals(LocalDate.of(2026, 9, 25), reversal.getEventDate());
        assertEquals(LocalDate.now(), reversal.getAccountingDate());
        verify(accountingEventService, never()).postEvent(any(ErpAccountingEventCreateReqBO.class));
    }
    private static ErpPurchaseInDO purchaseIn(Integer status) {
        return new ErpPurchaseInDO().setId(1L).setNo("PI001").setStatus(status)
                .setPaymentPrice(BigDecimal.ZERO);
    }
}

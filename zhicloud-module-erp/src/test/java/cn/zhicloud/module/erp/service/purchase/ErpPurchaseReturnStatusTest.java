package cn.zhicloud.module.erp.service.purchase;

import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.zhicloud.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.stock.ErpStockRecordService;
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
 * 采购退货审核状态行为测试，重点验证退货/红字发票事件只生成一次，且不与入库暂估事件互相冲销。
 */
@ExtendWith(MockitoExtension.class)
class ErpPurchaseReturnStatusTest {

    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpAccountingEventService accountingEventService;

    private ErpPurchaseReturnServiceImpl purchaseReturnService;

    @BeforeEach
    void setUp() {
        purchaseReturnService = new ErpPurchaseReturnServiceImpl();
        ReflectionTestUtils.setField(purchaseReturnService, "purchaseReturnMapper", purchaseReturnMapper);
        ReflectionTestUtils.setField(purchaseReturnService, "purchaseReturnItemMapper", purchaseReturnItemMapper);
        ReflectionTestUtils.setField(purchaseReturnService, "stockRecordService", stockRecordService);
        ReflectionTestUtils.setField(purchaseReturnService, "accountingEventService", accountingEventService);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotPostReturnEvent() {
        when(purchaseReturnMapper.selectById(1L)).thenReturn(purchaseReturn(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> purchaseReturnService.updatePurchaseReturnStatus(1L,
                ErpAuditStatus.APPROVE.getStatus()));

        verify(purchaseReturnMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verifyNoInteractions(purchaseReturnItemMapper, stockRecordService, accountingEventService);
    }

    @Test
    void approvalCreatesPurchaseReturnEventWithoutReversingReceiptEvent() {
        when(purchaseReturnMapper.selectById(1L)).thenReturn(purchaseReturn(ErpAuditStatus.PROCESS.getStatus())
                .setTotalPrice(new BigDecimal("2260.0000"))
                .setTotalProductPrice(new BigDecimal("2000.0000"))
                .setTotalTaxPrice(new BigDecimal("260.0000"))
                .setReturnTime(LocalDateTime.of(2026, 9, 25, 11, 0)));
        when(purchaseReturnMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(purchaseReturnItemMapper.selectListByReturnId(1L)).thenReturn(List.of(purchaseReturnItem()));

        assertDoesNotThrow(() -> purchaseReturnService.updatePurchaseReturnStatus(1L,
                ErpAuditStatus.APPROVE.getStatus()));

        ArgumentCaptor<ErpAccountingEventCreateReqBO> eventCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventCreateReqBO.class);
        verify(accountingEventService).postEvent(eventCaptor.capture());
        ErpAccountingEventCreateReqBO event = eventCaptor.getValue();
        assertEquals("PURCHASE_RETURN_POSTED", event.getEventType());
        assertEquals("PURCHASE_RETURN", event.getSourceDocType());
        assertEquals(1L, event.getSourceDocId());
        assertEquals("PR001", event.getSourceDocNo());
        assertEquals(0L, event.getReversalOfEventId());
        assertEquals(new BigDecimal("2000.0000"), event.getAmountWithoutTax());
        assertEquals(new BigDecimal("260.0000"), event.getTaxAmount());
        assertEquals(new BigDecimal("2260.0000"), event.getAmountWithTax());
        assertEquals(LocalDate.of(2026, 9, 25), event.getEventDate());
        assertEquals(LocalDate.of(2026, 9, 25), event.getAccountingDate());
    }

    @Test
    void cancellingApprovedPurchaseReturnCreatesReversalEvent() {
        when(purchaseReturnMapper.selectById(1L)).thenReturn(purchaseReturn(ErpAuditStatus.APPROVE.getStatus())
                .setReturnTime(LocalDateTime.of(2026, 9, 25, 11, 0)));
        when(purchaseReturnMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.APPROVE.getStatus()), any()))
                .thenReturn(1);
        when(purchaseReturnItemMapper.selectListByReturnId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> purchaseReturnService.updatePurchaseReturnStatus(1L,
                ErpAuditStatus.PROCESS.getStatus()));

        ArgumentCaptor<ErpAccountingEventReverseReqBO> reversalCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventReverseReqBO.class);
        verify(accountingEventService).reverseEvent(reversalCaptor.capture());
        ErpAccountingEventReverseReqBO reversal = reversalCaptor.getValue();
        assertEquals("PURCHASE_RETURN_POSTED", reversal.getEventType());
        assertEquals("PURCHASE_RETURN", reversal.getSourceDocType());
        assertEquals(1L, reversal.getSourceDocId());
        assertEquals(LocalDate.of(2026, 9, 25), reversal.getEventDate());
        assertEquals(LocalDate.now(), reversal.getAccountingDate());
        verify(accountingEventService, never()).postEvent(any(ErpAccountingEventCreateReqBO.class));
    }

    private static ErpPurchaseReturnDO purchaseReturn(Integer status) {
        return new ErpPurchaseReturnDO().setId(1L).setNo("PR001").setStatus(status)
                .setOrderId(9L).setOrderNo("P001")
                .setRefundPrice(BigDecimal.ZERO);
    }

    private static ErpPurchaseReturnItemDO purchaseReturnItem() {
        return new ErpPurchaseReturnItemDO().setId(11L).setReturnId(1L).setProductId(100L).setWarehouseId(200L)
                .setCount(new BigDecimal("10")).setProductPrice(new BigDecimal("200.0000"))
                .setTotalPrice(new BigDecimal("2000.0000"))
                .setTaxPercent(new BigDecimal("13")).setTaxPrice(new BigDecimal("260.0000"));
    }
}

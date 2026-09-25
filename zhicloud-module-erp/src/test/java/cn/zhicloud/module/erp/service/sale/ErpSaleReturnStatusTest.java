package cn.zhicloud.module.erp.service.sale;

import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.zhicloud.module.erp.enums.ErpAuditStatus;
import cn.zhicloud.module.erp.service.accounting.ErpAccountingEventService;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;
import cn.zhicloud.module.erp.service.product.ErpProductService;
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
 * 销售退货审核状态行为测试，重点验证退货/红字发票事件只生成一次，且不与出库事件互相冲销。
 *
 * <p>退货事件不设置 reversalOfEventId：部分退货不应把整张出库事件标记为已冲销，
 * 红字效果由独立的退货过账规则（V97）产生。</p>
 */
@ExtendWith(MockitoExtension.class)
class ErpSaleReturnStatusTest {

    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpAccountingEventService accountingEventService;

    private ErpSaleReturnServiceImpl saleReturnService;

    @BeforeEach
    void setUp() {
        saleReturnService = new ErpSaleReturnServiceImpl();
        ReflectionTestUtils.setField(saleReturnService, "saleReturnMapper", saleReturnMapper);
        ReflectionTestUtils.setField(saleReturnService, "saleReturnItemMapper", saleReturnItemMapper);
        ReflectionTestUtils.setField(saleReturnService, "stockRecordService", stockRecordService);
        ReflectionTestUtils.setField(saleReturnService, "productService", productService);
        ReflectionTestUtils.setField(saleReturnService, "accountingEventService", accountingEventService);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotPostReturnEvent() {
        when(saleReturnMapper.selectById(1L)).thenReturn(saleReturn(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> saleReturnService.updateSaleReturnStatus(1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(saleReturnMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verifyNoInteractions(saleReturnItemMapper, stockRecordService, productService, accountingEventService);
    }

    @Test
    void approvalCreatesSalesReturnEventWithoutReversingShipmentEvent() {
        when(saleReturnMapper.selectById(1L)).thenReturn(saleReturn(ErpAuditStatus.PROCESS.getStatus())
                .setTotalPrice(new BigDecimal("1130.0000"))
                .setTotalProductPrice(new BigDecimal("1000.0000"))
                .setTotalTaxPrice(new BigDecimal("130.0000"))
                .setReturnTime(LocalDateTime.of(2026, 9, 25, 11, 0)));
        when(saleReturnMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(saleReturnItemMapper.selectListByReturnId(1L)).thenReturn(List.of(saleReturnItem()));
        when(productService.getProduct(100L)).thenReturn(
                new ErpProductDO().setId(100L).setPurchasePrice(new BigDecimal("150.0000")));

        assertDoesNotThrow(() -> saleReturnService.updateSaleReturnStatus(1L, ErpAuditStatus.APPROVE.getStatus()));

        ArgumentCaptor<ErpAccountingEventCreateReqBO> eventCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventCreateReqBO.class);
        verify(accountingEventService).postEvent(eventCaptor.capture());
        ErpAccountingEventCreateReqBO event = eventCaptor.getValue();
        assertEquals("SALES_RETURN_POSTED", event.getEventType());
        assertEquals("SALE_RETURN", event.getSourceDocType());
        assertEquals(1L, event.getSourceDocId());
        assertEquals("SR001", event.getSourceDocNo());
        assertEquals(0L, event.getSourceDocLineId());
        assertEquals(1, event.getSourceRevision());
        assertEquals(0L, event.getReversalOfEventId());
        assertEquals(new BigDecimal("1000.0000"), event.getAmountWithoutTax());
        assertEquals(new BigDecimal("130.0000"), event.getTaxAmount());
        assertEquals(new BigDecimal("1130.0000"), event.getAmountWithTax());
        assertEquals(new BigDecimal("1500.0000"), event.getCostAmount());
        assertEquals(LocalDate.of(2026, 9, 25), event.getEventDate());
        assertEquals(LocalDate.of(2026, 9, 25), event.getAccountingDate());
    }

    @Test
    void cancellingApprovedSaleReturnCreatesReversalEvent() {
        when(saleReturnMapper.selectById(1L)).thenReturn(saleReturn(ErpAuditStatus.APPROVE.getStatus())
                .setReturnTime(LocalDateTime.of(2026, 9, 25, 11, 0)));
        when(saleReturnMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.APPROVE.getStatus()), any()))
                .thenReturn(1);
        when(saleReturnItemMapper.selectListByReturnId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> saleReturnService.updateSaleReturnStatus(1L, ErpAuditStatus.PROCESS.getStatus()));

        ArgumentCaptor<ErpAccountingEventReverseReqBO> reversalCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventReverseReqBO.class);
        verify(accountingEventService).reverseEvent(reversalCaptor.capture());
        ErpAccountingEventReverseReqBO reversal = reversalCaptor.getValue();
        assertEquals("SALES_RETURN_POSTED", reversal.getEventType());
        assertEquals("SALE_RETURN", reversal.getSourceDocType());
        assertEquals(1L, reversal.getSourceDocId());
        assertEquals(0L, reversal.getSourceDocLineId());
        assertEquals(1, reversal.getSourceRevision());
        assertEquals(LocalDate.of(2026, 9, 25), reversal.getEventDate());
        assertEquals(LocalDate.now(), reversal.getAccountingDate());
        verify(accountingEventService, never()).postEvent(any(ErpAccountingEventCreateReqBO.class));
    }

    private static ErpSaleReturnDO saleReturn(Integer status) {
        return new ErpSaleReturnDO().setId(1L).setNo("SR001").setStatus(status)
                .setOrderId(9L).setOrderNo("S001")
                .setRefundPrice(BigDecimal.ZERO);
    }

    private static ErpSaleReturnItemDO saleReturnItem() {
        return new ErpSaleReturnItemDO().setId(11L).setReturnId(1L).setProductId(100L).setWarehouseId(200L)
                .setCount(new BigDecimal("10")).setProductPrice(new BigDecimal("100.0000"))
                .setTotalPrice(new BigDecimal("1000.0000"))
                .setTaxPercent(new BigDecimal("13")).setTaxPrice(new BigDecimal("130.0000"));
    }
}

package cn.zhicloud.module.erp.service.sale;

import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.contract.ErpSaleOutApprovedEvent;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.zhicloud.module.erp.dal.mysql.sale.ErpSaleOutMapper;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 销售出库审核状态行为测试，重点验证收入确认与成本结转事件只生成一次，
 * 以及 V101 起「审核通过」会在同一事务内向事件发件箱登记一条跨域事件。
 */
@ExtendWith(MockitoExtension.class)
class ErpSaleOutStatusTest {

    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpAccountingEventService accountingEventService;
    @Mock
    private EventOutboxService eventOutboxService;

    private ErpSaleOutServiceImpl saleOutService;

    @BeforeEach
    void setUp() {
        saleOutService = new ErpSaleOutServiceImpl();
        ReflectionTestUtils.setField(saleOutService, "saleOutMapper", saleOutMapper);
        ReflectionTestUtils.setField(saleOutService, "saleOutItemMapper", saleOutItemMapper);
        ReflectionTestUtils.setField(saleOutService, "stockRecordService", stockRecordService);
        ReflectionTestUtils.setField(saleOutService, "productService", productService);
        ReflectionTestUtils.setField(saleOutService, "accountingEventService", accountingEventService);
        ReflectionTestUtils.setField(saleOutService, "eventOutboxService", eventOutboxService);
    }

    @Test
    void repeatedApprovalIsIdempotentAndDoesNotPostAccountingEvent() {
        when(saleOutMapper.selectById(1L)).thenReturn(saleOut(ErpAuditStatus.APPROVE.getStatus()));

        assertDoesNotThrow(() -> saleOutService.updateSaleOutStatus(1L, ErpAuditStatus.APPROVE.getStatus()));

        verify(saleOutMapper, never()).updateByIdAndStatus(anyLong(), any(), any());
        verifyNoInteractions(saleOutItemMapper, stockRecordService, productService, accountingEventService,
                eventOutboxService);
    }

    @Test
    void approvalCreatesSalesShipmentEventWithRevenueAndCost() {
        when(saleOutMapper.selectById(1L)).thenReturn(saleOut(ErpAuditStatus.PROCESS.getStatus())
                .setTotalPrice(new BigDecimal("2260.0000"))
                .setTotalProductPrice(new BigDecimal("2000.0000"))
                .setTotalTaxPrice(new BigDecimal("260.0000"))
                .setDiscountPrice(BigDecimal.ZERO)
                .setOtherPrice(BigDecimal.ZERO)
                .setOutTime(LocalDateTime.of(2026, 9, 25, 10, 0)));
        when(saleOutMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(saleOutItemMapper.selectListByOutId(1L)).thenReturn(List.of(saleOutItem()));
        when(productService.getProduct(100L)).thenReturn(
                new ErpProductDO().setId(100L).setPurchasePrice(new BigDecimal("150.0000")));

        assertDoesNotThrow(() -> saleOutService.updateSaleOutStatus(1L, ErpAuditStatus.APPROVE.getStatus()));

        ArgumentCaptor<ErpAccountingEventCreateReqBO> eventCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventCreateReqBO.class);
        verify(accountingEventService).postEvent(eventCaptor.capture());
        ErpAccountingEventCreateReqBO event = eventCaptor.getValue();
        assertEquals("SALES_SHIPMENT_POSTED", event.getEventType());
        assertEquals("SALE_OUT", event.getSourceDocType());
        assertEquals(1L, event.getSourceDocId());
        assertEquals("SO001", event.getSourceDocNo());
        assertEquals(1, event.getSourceRevision());
        assertEquals(new BigDecimal("2000.0000"), event.getAmountWithoutTax());
        assertEquals(new BigDecimal("260.0000"), event.getTaxAmount());
        assertEquals(new BigDecimal("2260.0000"), event.getAmountWithTax());
        assertEquals(new BigDecimal("1500.0000"), event.getCostAmount());
        assertEquals(LocalDate.of(2026, 9, 25), event.getEventDate());
        assertEquals(LocalDate.of(2026, 9, 25), event.getAccountingDate());

        // V101：审核通过必须在同一事务内登记「ERP 销售出库审核通过」事件，且只登记一次。
        // eventId 必须业务侧可推导（erp-sale-out-{id}-approved），否则消费侧无法靠 Inbox 唯一键去重。
        ArgumentCaptor<EventAppendReq> outboxCaptor = ArgumentCaptor.forClass(EventAppendReq.class);
        verify(eventOutboxService).append(outboxCaptor.capture());
        EventAppendReq appendReq = outboxCaptor.getValue();
        assertEquals("erp-sale-out-1-approved", appendReq.getEventId());
        assertEquals("erp.sale-out.approved.v1", appendReq.getEventType());
        assertEquals("erp_sale_out", appendReq.getAggregateType());
        assertEquals("1", appendReq.getAggregateId());
        assertEquals(0L, appendReq.getTenantId());
        assertEquals(LocalDateTime.of(2026, 9, 25, 10, 0), appendReq.getOccurredAt());
        assertNotNull(appendReq.getPayload());
        assertTrue(appendReq.getPayload().contains("SO001"));
        ErpSaleOutApprovedEvent approvedEvent = ErpSaleOutApprovedEvent.fromJson(appendReq.getPayload());
        assertEquals("SO001", approvedEvent.getNo());
        assertEquals(1L, approvedEvent.getSaleOutId());
        assertEquals(1, approvedEvent.getItems().size());
        assertEquals(100L, approvedEvent.getItems().get(0).getProductId());
        assertEquals(200L, approvedEvent.getItems().get(0).getWarehouseId());
        assertEquals(new BigDecimal("10"), approvedEvent.getItems().get(0).getCount());
    }

    @Test
    void cancellingApprovedSaleOutCreatesReversalEvent() {
        when(saleOutMapper.selectById(1L)).thenReturn(saleOut(ErpAuditStatus.APPROVE.getStatus())
                .setOutTime(LocalDateTime.of(2026, 9, 25, 10, 0)));
        when(saleOutMapper.updateByIdAndStatus(eq(1L), eq(ErpAuditStatus.APPROVE.getStatus()), any()))
                .thenReturn(1);
        when(saleOutItemMapper.selectListByOutId(1L)).thenReturn(List.of());

        assertDoesNotThrow(() -> saleOutService.updateSaleOutStatus(1L, ErpAuditStatus.PROCESS.getStatus()));

        ArgumentCaptor<ErpAccountingEventReverseReqBO> reversalCaptor =
                ArgumentCaptor.forClass(ErpAccountingEventReverseReqBO.class);
        verify(accountingEventService).reverseEvent(reversalCaptor.capture());
        ErpAccountingEventReverseReqBO reversal = reversalCaptor.getValue();
        assertEquals("SALES_SHIPMENT_POSTED", reversal.getEventType());
        assertEquals("SALE_OUT", reversal.getSourceDocType());
        assertEquals(1L, reversal.getSourceDocId());
        assertEquals(0L, reversal.getSourceDocLineId());
        assertEquals(1, reversal.getSourceRevision());
        assertEquals(LocalDate.of(2026, 9, 25), reversal.getEventDate());
        assertEquals(LocalDate.now(), reversal.getAccountingDate());
        verify(accountingEventService, never()).postEvent(any(ErpAccountingEventCreateReqBO.class));
        // V101：反审核不是「审核通过」，不能登记事件；登记了等于告诉 WMS 发生过一次出库。
        verify(eventOutboxService, never()).append(any(EventAppendReq.class));
    }

    private static ErpSaleOutDO saleOut(Integer status) {
        return new ErpSaleOutDO().setId(1L).setNo("SO001").setStatus(status)
                .setReceiptPrice(BigDecimal.ZERO);
    }

    private static ErpSaleOutItemDO saleOutItem() {
        return new ErpSaleOutItemDO().setId(11L).setOutId(1L).setProductId(100L).setWarehouseId(200L)
                .setCount(new BigDecimal("10")).setProductPrice(new BigDecimal("200.0000"))
                .setTotalPrice(new BigDecimal("2000.0000"))
                .setTaxPercent(new BigDecimal("13")).setTaxPrice(new BigDecimal("260.0000"));
    }
}
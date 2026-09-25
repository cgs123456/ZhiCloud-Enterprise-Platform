package cn.zhicloud.module.wms.service.integration;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.integration.contract.ErpSaleOutApprovedEvent;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.module.wms.controller.admin.order.shipment.vo.order.WmsShipmentOrderSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.enums.order.WmsShipmentOrderTypeEnum;
import cn.zhicloud.module.wms.service.order.shipment.WmsShipmentOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 「ERP 销售出库审核通过 -> WMS 出库单」消费者单元测试（V101）
 *
 * <p>四个用例分别对应链路里最容易退化、且退化后最难发现的四个不变量：
 * 映射正确、重复投递不建第二张单、映射缺失必须失败、可选字段缺失不阻断业务。
 */
@ExtendWith(MockitoExtension.class)
class ErpSaleOutApprovedConsumerTest {

    @Mock
    private WmsShipmentOrderService shipmentOrderService;
    @Mock
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Mock
    private MdmCrossRefService mdmCrossRefService;

    @InjectMocks
    private ErpSaleOutApprovedConsumer consumer;

    @Test
    void createsShipmentOrderWithMdmMappedIds() throws Exception {
        when(shipmentOrderMapper.selectByBizOrderNo("SO001")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "PRODUCT", "100", "WMS", "SKU")).thenReturn(1001L);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "WAREHOUSE", "200", "WMS", "WAREHOUSE"))
                .thenReturn(2001L);
        when(mdmCrossRefService.findTargetId(0L, "ERP", "CUSTOMER", "300", "WMS", "MERCHANT")).thenReturn("3001");

        consumer.handle(outboxEvent(singleItemPayload()));

        ArgumentCaptor<WmsShipmentOrderSaveReqVO> captor =
                ArgumentCaptor.forClass(WmsShipmentOrderSaveReqVO.class);
        verify(shipmentOrderService).createShipmentOrder(captor.capture());
        WmsShipmentOrderSaveReqVO req = captor.getValue();
        // 单号可推导：WMS 单号直接反查 ERP 单号，且重试时不会绕开单号唯一校验
        assertEquals("WSSO001", req.getNo());
        assertEquals(WmsShipmentOrderTypeEnum.SALE.getType(), req.getType());
        assertEquals("SO001", req.getBizOrderNo());
        assertEquals(2001L, req.getWarehouseId());
        assertEquals(3001L, req.getMerchantId());
        assertEquals(LocalDateTime.of(2026, 9, 25, 10, 0), req.getOrderTime());
        assertEquals(1, req.getDetails().size());
        assertEquals(1001L, req.getDetails().get(0).getSkuId());
        assertEquals(new BigDecimal("10"), req.getDetails().get(0).getQuantity());
        assertEquals(new BigDecimal("200.0000"), req.getDetails().get(0).getPrice());
        assertTrue(req.getRemark().contains("SO001"));
    }

    @Test
    void skipsWhenBizOrderNoAlreadyExists() throws Exception {
        when(shipmentOrderMapper.selectByBizOrderNo("SO001")).thenReturn(new WmsShipmentOrderDO().setId(9L));

        consumer.handle(outboxEvent(singleItemPayload()));

        verify(shipmentOrderService, never()).createShipmentOrder(any());
        // 幂等短路必须在最前面：连主数据映射都不该发生
        verifyNoInteractions(mdmCrossRefService);
    }

    @Test
    void failsWhenSkuMappingMissing() throws Exception {
        when(shipmentOrderMapper.selectByBizOrderNo("SO001")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "PRODUCT", "100", "WMS", "SKU"))
                .thenThrow(exception(MDM_CROSS_REF_NOT_FOUND, "ERP", "PRODUCT", "100", "WMS", "SKU"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> consumer.handle(outboxEvent(singleItemPayload())));

        assertEquals(1_099_002_003, ex.getCode());
        // 映射缺失绝不能静默建单：建到错误 SKU 上的出库单事后几乎无法发现
        verify(shipmentOrderService, never()).createShipmentOrder(any());
    }

    @Test
    void createsWithoutMerchantWhenCustomerMappingMissing() throws Exception {
        when(shipmentOrderMapper.selectByBizOrderNo("SO001")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "PRODUCT", "100", "WMS", "SKU")).thenReturn(1001L);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "WAREHOUSE", "200", "WMS", "WAREHOUSE"))
                .thenReturn(2001L);
        when(mdmCrossRefService.findTargetId(0L, "ERP", "CUSTOMER", "300", "WMS", "MERCHANT")).thenReturn(null);

        consumer.handle(outboxEvent(singleItemPayload()));

        ArgumentCaptor<WmsShipmentOrderSaveReqVO> captor =
                ArgumentCaptor.forClass(WmsShipmentOrderSaveReqVO.class);
        verify(shipmentOrderService).createShipmentOrder(captor.capture());
        assertNull(captor.getValue().getMerchantId());
    }

    @Test
    void failsWhenErpLinesMapToMultipleWarehouses() throws Exception {
        when(shipmentOrderMapper.selectByBizOrderNo("SO001")).thenReturn(null);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "PRODUCT", "100", "WMS", "SKU")).thenReturn(1001L);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "WAREHOUSE", "200", "WMS", "WAREHOUSE"))
                .thenReturn(2001L);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "PRODUCT", "101", "WMS", "SKU")).thenReturn(1002L);
        when(mdmCrossRefService.mapTargetIdAsLong(0L, "ERP", "WAREHOUSE", "201", "WMS", "WAREHOUSE"))
                .thenReturn(2002L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> consumer.handle(outboxEvent(multiWarehousePayload())));

        assertEquals(1_060_201_009, ex.getCode());
        verify(shipmentOrderService, never()).createShipmentOrder(any());
    }

    /**
     * 消费者名与事件类型是「契约」而不是实现细节：改名会让 Inbox 去重键变化，
     * 历史事件会被当成新消费者重新消费一次；事件类型写错则整条链路静默不生效。
     */
    @Test
    void declaresStableConsumerNameAndEventType() {
        assertEquals("wms-shipment-order-creator", consumer.name());
        assertTrue(consumer.supports("erp.sale-out.approved.v1"));
        assertFalse(consumer.supports("erp.sale-out.approved.v2"));
        assertFalse(consumer.supports("wms.shipment.finished.v1"));
    }
    private static EventOutboxDO outboxEvent(ErpSaleOutApprovedEvent payload) {
        return EventOutboxDO.builder()
                .id(1L)
                .eventId(ErpSaleOutApprovedEvent.eventIdOf(payload.getSaleOutId()))
                .eventType("erp.sale-out.approved.v1")
                .tenantId(0L)
                .payload(payload.toJson())
                .build();
    }

    private static ErpSaleOutApprovedEvent singleItemPayload() {
        return ErpSaleOutApprovedEvent.builder()
                .no("SO001")
                .saleOutId(1L)
                .customerId(300L)
                .outTime(LocalDateTime.of(2026, 9, 25, 10, 0))
                .totalPrice(new BigDecimal("2260.0000"))
                .items(List.of(ErpSaleOutApprovedEvent.Item.builder()
                        .productId(100L).warehouseId(200L)
                        .count(new BigDecimal("10")).price(new BigDecimal("200.0000"))
                        .build()))
                .build();
    }

    private static ErpSaleOutApprovedEvent multiWarehousePayload() {
        return ErpSaleOutApprovedEvent.builder()
                .no("SO001")
                .saleOutId(1L)
                .customerId(300L)
                .outTime(LocalDateTime.of(2026, 9, 25, 10, 0))
                .items(List.of(
                        ErpSaleOutApprovedEvent.Item.builder()
                                .productId(100L).warehouseId(200L)
                                .count(new BigDecimal("10")).price(new BigDecimal("200.0000"))
                                .build(),
                        ErpSaleOutApprovedEvent.Item.builder()
                                .productId(101L).warehouseId(201L)
                                .count(new BigDecimal("5")).price(new BigDecimal("50.0000"))
                                .build()))
                .build();
    }

}
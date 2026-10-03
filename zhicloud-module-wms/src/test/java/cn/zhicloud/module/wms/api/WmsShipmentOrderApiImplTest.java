package cn.zhicloud.module.wms.api;

import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.mysql.order.shipment.WmsShipmentOrderMapper;
import cn.zhicloud.module.wms.enums.order.WmsOrderStatusEnum;
import cn.zhicloud.module.wms.service.order.shipment.WmsShipmentOrderService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.SHIPMENT_ORDER_NOT_SHIPPED_FOR_WAYBILL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link WmsShipmentOrderApiImpl} 的单元测试：运单签收回传出库单这一段。
 *
 * @author 智云
 */
public class WmsShipmentOrderApiImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private WmsShipmentOrderApiImpl shipmentOrderApi;

    @Mock
    private WmsShipmentOrderMapper shipmentOrderMapper;
    @Mock
    private WmsShipmentOrderService shipmentOrderService;

    private WmsShipmentOrderDO order(Long id, Integer status) {
        WmsShipmentOrderDO order = new WmsShipmentOrderDO();
        order.setId(id);
        order.setStatus(status);
        return order;
    }

    @Test
    public void testValidate_missingOrder_rejected() {
        assertServiceException(() -> shipmentOrderApi.validateShippableForWaybill(9999L),
                SHIPMENT_ORDER_NOT_EXISTS);
    }

    @Test
    public void testValidate_notShipped_rejected() {
        // 已打包还没发货：不能被运单签收
        when(shipmentOrderMapper.selectById(1L)).thenReturn(order(1L, WmsOrderStatusEnum.PACKED.getStatus()));

        assertServiceException(() -> shipmentOrderApi.validateShippableForWaybill(1L),
                SHIPMENT_ORDER_NOT_SHIPPED_FOR_WAYBILL, 1L);
    }

    @Test
    public void testValidate_finished_rejected() {
        // 已完成的单子也不能被重复签收回传（幂等的第二道闸）
        when(shipmentOrderMapper.selectById(1L)).thenReturn(order(1L, WmsOrderStatusEnum.FINISHED.getStatus()));

        assertServiceException(() -> shipmentOrderApi.validateShippableForWaybill(1L),
                SHIPMENT_ORDER_NOT_SHIPPED_FOR_WAYBILL, 1L);
    }

    @Test
    public void testSign_ordersWaybillStampBeforeFinish() {
        when(shipmentOrderMapper.selectById(2L)).thenReturn(order(2L, WmsOrderStatusEnum.SHIPPED.getStatus()));

        shipmentOrderApi.signShipmentOrderByWaybill(2L, 7001L);

        // 顺序是刻意的：先登记运单号再 finish()，否则 finish 成功而登记失败会留下"已完成但查不到承运运单"
        WmsShipmentOrderDO expected = new WmsShipmentOrderDO();
        expected.setId(2L);
        expected.setWaybillId(7001L);
        InOrder inOrder = inOrder(shipmentOrderMapper, shipmentOrderService);
        inOrder.verify(shipmentOrderMapper).updateById(expected);
        inOrder.verify(shipmentOrderService).finish(2L);
    }

    @Test
    public void testSign_rejectedBeforeAnyWrite() {
        when(shipmentOrderMapper.selectById(3L)).thenReturn(order(3L, WmsOrderStatusEnum.PREPARE.getStatus()));

        assertServiceException(() -> shipmentOrderApi.signShipmentOrderByWaybill(3L, 7002L),
                SHIPMENT_ORDER_NOT_SHIPPED_FOR_WAYBILL, 3L);
        // 校验失败不能已经先把单子改掉
        verify(shipmentOrderMapper, never()).updateById(any(WmsShipmentOrderDO.class));
        verify(shipmentOrderService, never()).finish(any());
    }

}

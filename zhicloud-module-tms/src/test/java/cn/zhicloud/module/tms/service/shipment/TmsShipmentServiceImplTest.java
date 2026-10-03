package cn.zhicloud.module.tms.service.shipment;

import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.zhicloud.module.tms.dal.mysql.shipment.TmsShipmentMapper;
import cn.zhicloud.module.tms.service.driver.TmsDriverService;
import cn.zhicloud.module.tms.service.vehicle.TmsVehicleService;
import cn.zhicloud.module.wms.api.WmsShipmentOrderApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;

/**
 * {@link TmsShipmentServiceImpl} 的单元测试，聚焦「运单挂 WMS 出库单」这一段。
 *
 * <p>用纯 Mockito：这里要证明的是调用顺序与条件分支（什么时候调 WMS、什么时候不调），
 * 而不是 WMS 侧的状态机——那部分由 WmsShipmentOrderApiImplTest 覆盖。
 *
 * @author 智云
 */
public class TmsShipmentServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TmsShipmentServiceImpl shipmentService;

    @Mock
    private TmsShipmentMapper shipmentMapper;
    @Mock
    private TmsVehicleService vehicleService;
    @Mock
    private TmsDriverService driverService;
    @Mock
    private WmsShipmentOrderApi shipmentOrderApi;

    /** TMS 运单状态：30 已到达 */
    private static final int STATUS_ARRIVED = 30;
    /** TMS 运单状态：40 已签收 */
    private static final int STATUS_SIGNED = 40;

    private TmsShipmentDO shipment(Long id, Integer status, Long shipmentOrderId) {
        TmsShipmentDO shipment = new TmsShipmentDO();
        shipment.setId(id);
        shipment.setStatus(status);
        shipment.setShipmentOrderId(shipmentOrderId);
        return shipment;
    }

    @Test
    public void testCreateShipment_withShipmentOrder_validatesWmsOrder() {
        TmsShipmentSaveReqVO reqVO = new TmsShipmentSaveReqVO();
        reqVO.setNo("TN20261002001");
        reqVO.setShipmentOrderId(2001L);

        shipmentService.createShipment(reqVO);

        verify(shipmentOrderApi).validateShippableForWaybill(2001L);
        verify(shipmentMapper).insert(any(TmsShipmentDO.class));
    }

    @Test
    public void testCreateShipment_manualShipment_skipsWms() {
        TmsShipmentSaveReqVO reqVO = new TmsShipmentSaveReqVO();
        reqVO.setNo("TN20261002002");

        shipmentService.createShipment(reqVO);

        // 手工运单不走出库单联动：不能因为 WMS 侧没有对应单据就建不了单
        verify(shipmentOrderApi, never()).validateShippableForWaybill(any());
        verify(shipmentMapper).insert(any(TmsShipmentDO.class));
    }

    @Test
    public void testConfirmSign_writesBackToWms() {
        TmsShipmentDO shipment = shipment(3001L, STATUS_ARRIVED, 2001L);
        when(shipmentMapper.selectById(3001L)).thenReturn(shipment);

        shipmentService.confirmSign(3001L);

        // 状态与签收时间必须一起落库：只改状态是本次改动前的行为，签收时刻在库里不可追溯
        ArgumentCaptor<TmsShipmentDO> captor = ArgumentCaptor.forClass(TmsShipmentDO.class);
        verify(shipmentMapper).updateById(captor.capture());
        assertEquals(STATUS_SIGNED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getSignTime());
        verify(shipmentOrderApi).signShipmentOrderByWaybill(eq(2001L), eq(3001L));
    }

    @Test
    public void testConfirmSign_manualShipment_keepsWmsOutOfIt() {
        TmsShipmentDO shipment = shipment(3002L, STATUS_ARRIVED, null);
        when(shipmentMapper.selectById(3002L)).thenReturn(shipment);

        shipmentService.confirmSign(3002L);

        verify(shipmentMapper).updateById(any(TmsShipmentDO.class)); // 状态更新照常发生
        verify(shipmentOrderApi, never()).signShipmentOrderByWaybill(any(), any());
    }

    @Test
    public void testConfirmSign_notArrived_rejected() {
        when(shipmentMapper.selectById(3003L)).thenReturn(shipment(3003L, STATUS_SIGNED, 2001L));

        assertServiceException(() -> shipmentService.confirmSign(3003L), TMS_SHIPMENT_STATUS_INVALID, 3003L);
        verify(shipmentOrderApi, never()).signShipmentOrderByWaybill(any(), any());
    }



}

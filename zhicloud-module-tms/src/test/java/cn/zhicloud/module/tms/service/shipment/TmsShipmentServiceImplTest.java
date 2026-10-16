package cn.zhicloud.module.tms.service.shipment;

import cn.zhicloud.framework.test.core.ut.BaseMockitoUnitTest;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentDispatchReqVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.driver.TmsDriverDO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.dal.mysql.driver.TmsDriverMapper;
import cn.zhicloud.module.tms.dal.mysql.shipment.TmsShipmentMapper;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsVehicleMapper;
import cn.zhicloud.module.tms.service.driver.TmsDriverService;
import cn.zhicloud.module.tms.service.vehicle.TmsVehicleService;
import cn.zhicloud.module.wms.api.WmsShipmentOrderApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_CONCURRENT_DISPATCH;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_OPTIMISTIC_LOCK_FAIL;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_STATUS_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

/**
 * {@link TmsShipmentServiceImpl} 的单元测试。
 *
 * <p>用纯 Mockito：这里要证明的是调用顺序与条件分支——什么时候调 WMS、什么时候不调，
 * CAS 失败时会不会留下车辆/司机的脏状态——而不是 WMS 侧的状态机，那部分由
 * WmsShipmentOrderApiImplTest 覆盖。</p>
 *
 * <p>§12.231 起新增派车/签收的并发断言：运单状态机原本是 selectById → 内存判 status →
 * updateById 全量覆盖，并发重复派车会同时通过校验并把不同车辆/司机写进同一张运单。</p>
 *
 * @author 智云
 */
public class TmsShipmentServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private TmsShipmentServiceImpl shipmentService;

    @Mock
    private TmsShipmentMapper shipmentMapper;
    @Mock
    private TmsVehicleMapper vehicleMapper;
    @Mock
    private TmsDriverMapper driverMapper;
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
    /** TMS 运单状态：10 待发车 */
    private static final int STATUS_PENDING = 10;
    /** TMS 运单状态：20 运输中 */
    private static final int STATUS_IN_TRANSIT = 20;
    /** TMS 车辆状态：10 可用 */
    private static final int VEHICLE_STATUS_AVAILABLE = 10;
    /** TMS 车辆状态：20 运输中 */
    private static final int VEHICLE_STATUS_IN_TRANSIT = 20;

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
        when(shipmentMapper.transitStatusIfChanged(eq(3001L), eq(STATUS_ARRIVED), eq(STATUS_SIGNED), any()))
                .thenReturn(1);

        shipmentService.confirmSign(3001L);

        // 状态与签收时间必须一起落库：只改状态是本次改动前的行为，签收时刻在库里不可追溯
        ArgumentCaptor<TmsShipmentDO> captor = ArgumentCaptor.forClass(TmsShipmentDO.class);
        verify(shipmentMapper).transitStatusIfChanged(eq(3001L), eq(STATUS_ARRIVED), eq(STATUS_SIGNED),
                captor.capture());
        assertNotNull(captor.getValue().getSignTime());
        verify(shipmentOrderApi).signShipmentOrderByWaybill(eq(2001L), eq(3001L));
    }

    @Test
    public void testConfirmSign_manualShipment_keepsWmsOutOfIt() {
        TmsShipmentDO shipment = shipment(3002L, STATUS_ARRIVED, null);
        when(shipmentMapper.selectById(3002L)).thenReturn(shipment);
        when(shipmentMapper.transitStatusIfChanged(eq(3002L), eq(STATUS_ARRIVED), eq(STATUS_SIGNED), any()))
                .thenReturn(1);

        shipmentService.confirmSign(3002L);

        verify(shipmentMapper).transitStatusIfChanged(eq(3002L), eq(STATUS_ARRIVED), eq(STATUS_SIGNED), any()); // 状态推进照常发生
        verify(shipmentOrderApi, never()).signShipmentOrderByWaybill(any(), any());
    }

    @Test
    public void testConfirmSign_notArrived_rejected() {
        when(shipmentMapper.selectById(3003L)).thenReturn(shipment(3003L, STATUS_SIGNED, 2001L));

        assertServiceException(() -> shipmentService.confirmSign(3003L), TMS_SHIPMENT_STATUS_INVALID, 3003L);
        verify(shipmentOrderApi, never()).signShipmentOrderByWaybill(any(), any());
    }

    /**
     * §12.231：CAS 推进失败（有人在事务外把这单改走了）必须抛冲突，不能当成功。
     * 这正是「伪成功」：调用方会以为已签收，实际状态没动。
     */
    @Test
    public void testConfirmSign_casRejected_throwsOptimisticLock() {
        when(shipmentMapper.selectById(3004L)).thenReturn(shipment(3004L, STATUS_ARRIVED, 2001L));
        when(shipmentMapper.transitStatusIfChanged(eq(3004L), eq(STATUS_ARRIVED), eq(STATUS_SIGNED), any()))
                .thenReturn(0); // 并发改走了

        assertServiceException(() -> shipmentService.confirmSign(3004L), TMS_SHIPMENT_OPTIMISTIC_LOCK_FAIL, 3004L);
        // CAS 失败就不能回传 WMS，否则出库单被完成而运单没签收
        verify(shipmentOrderApi, never()).signShipmentOrderByWaybill(any(), any());
    }

    /**
     * §12.231 派车并发：CAS 抢不到状态位时抛并发冲突，且不许动车辆/司机。
     *
     * <p>这条锁住的是「失败方不得产生副作用」。修好前两个并发 dispatch 都能写入成功，
     * 先提交的那台车/司机的状态已被改成「运输中」，两边的状态改写还互相覆盖。</p>
     */
    @Test
    public void testDispatch_casRejected_noSideEffectOnVehicleOrDriver() {
        TmsShipmentDO pending = shipment(4001L, STATUS_PENDING, null);
        when(shipmentMapper.selectById(4001L)).thenReturn(pending);
        when(shipmentMapper.transitStatusIfChanged(eq(4001L), eq(STATUS_PENDING), eq(STATUS_IN_TRANSIT), any()))
                .thenReturn(0); // 被另一个请求先派了

        TmsShipmentDispatchReqVO reqVO = new TmsShipmentDispatchReqVO();
        reqVO.setId(4001L);
        reqVO.setVehicleId(3072L);
        reqVO.setDriverId(4096L);

        assertServiceException(() -> shipmentService.dispatch(reqVO), TMS_SHIPMENT_CONCURRENT_DISPATCH, 4001L);
        // 失败方不能碰车辆/司机的状态：那会让一台车在窗口里显示「运输中」却查不到对应运单
        verify(vehicleMapper, never()).selectByIdForUpdate(any());
        verify(driverMapper, never()).selectByIdForUpdate(any());
    }

    /**
     * §12.231 派车正常路径：CAS 成功后才改车辆/司机，且车辆/司机带版本 CAS 写。
     */
    @Test
    public void testDispatch_success_casWritesVehicleAndDriverWithVersion() {
        TmsShipmentDO pending = shipment(4002L, STATUS_PENDING, null);
        when(shipmentMapper.selectById(4002L)).thenReturn(pending);
        when(shipmentMapper.transitStatusIfChanged(eq(4002L), eq(STATUS_PENDING), eq(STATUS_IN_TRANSIT), any()))
                .thenReturn(1);

        TmsVehicleDO vehicle = new TmsVehicleDO();
        vehicle.setId(3072L);
        vehicle.setVersion(3L);
        when(vehicleMapper.selectByIdForUpdate(3072L)).thenReturn(vehicle);
        when(vehicleMapper.updateStatusWithVersion(any())).thenReturn(1);

        TmsDriverDO driver = new TmsDriverDO();
        driver.setId(4096L);
        driver.setVersion(5L);
        when(driverMapper.selectByIdForUpdate(4096L)).thenReturn(driver);
        when(driverMapper.updateStatusWithVersion(any())).thenReturn(1);

        TmsShipmentDispatchReqVO reqVO = new TmsShipmentDispatchReqVO();
        reqVO.setId(4002L);
        reqVO.setVehicleId(3072L);
        reqVO.setDriverId(4096L);

        shipmentService.dispatch(reqVO);

        // 运单车/司机必须一起写进去，否则派车后运单查不到车
        ArgumentCaptor<TmsShipmentDO> captor = ArgumentCaptor.forClass(TmsShipmentDO.class);
        verify(shipmentMapper).transitStatusIfChanged(eq(4002L), eq(STATUS_PENDING), eq(STATUS_IN_TRANSIT),
                captor.capture());
        assertEquals(3072L, captor.getValue().getVehicleId());
        assertEquals(4096L, captor.getValue().getDriverId());
        assertNotNull(captor.getValue().getDepartureTime());

        // 车辆/司机必须是「锁读 + 带版本写」：拿旧版本去 CAS 会误报冲突
        verify(vehicleMapper).selectByIdForUpdate(3072L);
        verify(driverMapper).selectByIdForUpdate(4096L);
        ArgumentCaptor<TmsVehicleDO> vCaptor = ArgumentCaptor.forClass(TmsVehicleDO.class);
        verify(vehicleMapper).updateStatusWithVersion(vCaptor.capture());
        assertEquals(VEHICLE_STATUS_IN_TRANSIT, vCaptor.getValue().getStatus());
        assertEquals(3L, vCaptor.getValue().getVersion());
    }

    /**
     * 新增/修改 VO 不得暴露 status。
     *
     * <p>§12.231：dispatch 的 CAS 前提是状态单调不回退，VO 上一个可编辑的 status 等于让调用方
     * 把运单改回「待发车」再重复派车。乐观锁只解决「同时改」，不解决「改错值」，所以这个口子
     * 必须在入口收掉，并由测试守住——否则后人加回字段时编译照过、功能照坏。</p>
     */
    @Test
    public void testSaveReqVoDoesNotExposeStatus() {
        List<String> fields = Arrays.stream(TmsShipmentSaveReqVO.class.getDeclaredFields())
                .map(Field::getName).toList();
        assertFalse(fields.contains("status"),
                "TmsShipmentSaveReqVO 暴露了 status：表单可把运单改回待发车，绕过 §12.231 的并派车互斥");
    }

    /**
     * 新增时状态由服务端强制置为「待发车」，不依赖调用方是否传值。
     */
    @Test
    public void testCreateShipment_statusIsServerAssigned() {
        TmsShipmentSaveReqVO reqVO = new TmsShipmentSaveReqVO();
        reqVO.setNo("TMSCASE01");

        ArgumentCaptor<TmsShipmentDO> captor = ArgumentCaptor.forClass(TmsShipmentDO.class);
        when(shipmentMapper.insert(captor.capture())).thenReturn(1);

        shipmentService.createShipment(reqVO);

        assertEquals(10, captor.getValue().getStatus(), "新运单必须落库为「待发车」");
    }
}

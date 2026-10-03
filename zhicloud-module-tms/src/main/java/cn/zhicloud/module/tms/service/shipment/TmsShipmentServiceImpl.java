package cn.zhicloud.module.tms.service.shipment;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentDispatchReqVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentPageReqVO;
import cn.zhicloud.module.tms.controller.admin.shipment.vo.TmsShipmentSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentDO;
import cn.zhicloud.module.tms.dal.dataobject.driver.TmsDriverDO;
import cn.zhicloud.module.tms.dal.dataobject.shipment.TmsShipmentStopDO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.dal.mysql.driver.TmsDriverMapper;
import cn.zhicloud.module.tms.dal.mysql.shipment.TmsShipmentMapper;
import cn.zhicloud.module.tms.dal.mysql.shipment.TmsShipmentStopMapper;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsVehicleMapper;
import cn.zhicloud.module.tms.service.driver.TmsDriverService;
import cn.zhicloud.module.tms.service.vehicle.TmsVehicleService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_DISPATCH_FAIL_DRIVER;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_DISPATCH_FAIL_VEHICLE;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_NOT_EXISTS;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_STATUS_INVALID;
import cn.zhicloud.module.wms.api.WmsShipmentOrderApi;

/**
 * TMS 运单 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class TmsShipmentServiceImpl implements TmsShipmentService {

    /**
     * 运单状态：待发车
     */
    private static final int STATUS_PENDING = 10;
    /**
     * 运单状态：运输中
     */
    private static final int STATUS_IN_TRANSIT = 20;
    /**
     * 运单状态：已到达
     */
    private static final int STATUS_ARRIVED = 30;
    /**
     * 运单状态：已签收
     */
    private static final int STATUS_SIGNED = 40;
    /**
     * 车辆状态：运输中
     */
    private static final int VEHICLE_STATUS_IN_TRANSIT = 20;
    /**
     * 司机状态：运输中
     */
    private static final int DRIVER_STATUS_IN_TRANSIT = 20;

    @Resource
    private TmsShipmentMapper shipmentMapper;
    @Resource
    private TmsShipmentStopMapper shipmentStopMapper;
    @Resource
    private TmsVehicleService vehicleService;
    @Resource
    private TmsDriverService driverService;
    @Resource
    private TmsVehicleMapper vehicleMapper;
    @Resource
    private TmsDriverMapper driverMapper;
    /**
     * WMS 出库单 API：运单从已发货的出库单发起，签收后回传把出库单推进到已完成。
     * 仅依赖 api 包，禁止 import WMS 的 dal/service（P1-3 模块边界治理）。
     */
    @Resource
    private WmsShipmentOrderApi shipmentOrderApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createShipment(TmsShipmentSaveReqVO createReqVO) {
        TmsShipmentDO shipment = BeanUtils.toBean(createReqVO, TmsShipmentDO.class);
        if (shipment.getStatus() == null) {
            shipment.setStatus(STATUS_PENDING);
        }
        // 挂了出库单的运单必须能在 WMS 里找到「已发货」的单子，否则签收时无处回传
        if (shipment.getShipmentOrderId() != null) {
            shipmentOrderApi.validateShippableForWaybill(shipment.getShipmentOrderId());
        }
        shipmentMapper.insert(shipment);
        return shipment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateShipment(TmsShipmentSaveReqVO updateReqVO) {
        validateShipmentExists(updateReqVO.getId());
        TmsShipmentDO updateObj = BeanUtils.toBean(updateReqVO, TmsShipmentDO.class);
        shipmentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShipment(Long id) {
        validateShipmentExists(id);
        shipmentStopMapper.deleteByShipmentId(id);
        shipmentMapper.deleteById(id);
    }

    @Override
    public TmsShipmentDO getShipment(Long id) {
        return shipmentMapper.selectById(id);
    }

    @Override
    public PageResult<TmsShipmentDO> getShipmentPage(TmsShipmentPageReqVO pageReqVO) {
        return shipmentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<TmsShipmentStopDO> getShipmentStopList(Long shipmentId) {
        return shipmentStopMapper.selectListByShipmentId(shipmentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(TmsShipmentDispatchReqVO dispatchReqVO) {
        // 1. 校验运单存在
        TmsShipmentDO shipment = shipmentMapper.selectById(dispatchReqVO.getId());
        if (shipment == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }
        // 2. 校验运单状态：只有待发车状态可调度
        if (!Integer.valueOf(STATUS_PENDING).equals(shipment.getStatus())) {
            throw exception(TMS_SHIPMENT_STATUS_INVALID, shipment.getId());
        }
        // 3. 校验车辆可用性（传入车辆编号时）
        Long vehicleId = dispatchReqVO.getVehicleId();
        if (vehicleId == null && shipment.getVehicleId() != null) {
            vehicleId = shipment.getVehicleId();
        }
        if (vehicleId == null) {
            throw exception(TMS_SHIPMENT_DISPATCH_FAIL_VEHICLE, shipment.getId());
        }
        vehicleService.validateVehicleAvailable(vehicleId);
        // 4. 校验司机可用性（传入司机编号时）
        Long driverId = dispatchReqVO.getDriverId();
        if (driverId == null && shipment.getDriverId() != null) {
            driverId = shipment.getDriverId();
        }
        if (driverId == null) {
            throw exception(TMS_SHIPMENT_DISPATCH_FAIL_DRIVER, shipment.getId());
        }
        driverService.validateDriverAvailable(driverId);
        // 5. 更新运单：绑定车辆/司机、状态置为运输中、记录发车时间
        TmsShipmentDO updateObj = new TmsShipmentDO();
        updateObj.setId(shipment.getId());
        updateObj.setVehicleId(vehicleId);
        updateObj.setDriverId(driverId);
        updateObj.setStatus(STATUS_IN_TRANSIT);
        updateObj.setDepartureTime(LocalDateTime.now());
        shipmentMapper.updateById(updateObj);
        // 6. 更新车辆/司机状态为运输中（冗余更新，避免重复调度）
        updateVehicleStatus(vehicleId, VEHICLE_STATUS_IN_TRANSIT);
        updateDriverStatus(driverId, DRIVER_STATUS_IN_TRANSIT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmArrival(Long id) {
        TmsShipmentDO shipment = shipmentMapper.selectById(id);
        if (shipment == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }
        if (!Integer.valueOf(STATUS_IN_TRANSIT).equals(shipment.getStatus())) {
            throw exception(TMS_SHIPMENT_STATUS_INVALID, id);
        }
        TmsShipmentDO updateObj = new TmsShipmentDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_ARRIVED);
        updateObj.setActualArrivalTime(LocalDateTime.now());
        shipmentMapper.updateById(updateObj);
        // 车辆/司机恢复可用
        if (shipment.getVehicleId() != null) {
            updateVehicleStatus(shipment.getVehicleId(), 10);
        }
        if (shipment.getDriverId() != null) {
            updateDriverStatus(shipment.getDriverId(), 10);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmSign(Long id) {
        TmsShipmentDO shipment = shipmentMapper.selectById(id);
        if (shipment == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }
        if (!Integer.valueOf(STATUS_ARRIVED).equals(shipment.getStatus())) {
            throw exception(TMS_SHIPMENT_STATUS_INVALID, id);
        }
        TmsShipmentDO updateObj = new TmsShipmentDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_SIGNED);
        updateObj.setSignTime(LocalDateTime.now());
        shipmentMapper.updateById(updateObj);
        // 回传 WMS：出库单推进到已完成（含库存扣减）。放在状态落库之后，
        // 回传失败时本方法的事务一起回滚，不会出现「WMS 完成而运单还是已到达」的半签收。
        if (shipment.getShipmentOrderId() != null) {
            shipmentOrderApi.signShipmentOrderByWaybill(shipment.getShipmentOrderId(), id);
        }
    }

    private void validateShipmentExists(Long id) {
        if (shipmentMapper.selectById(id) == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }
    }

    private void updateVehicleStatus(Long vehicleId, Integer status) {
        TmsVehicleDO updateObj = new TmsVehicleDO();
        updateObj.setId(vehicleId);
        updateObj.setStatus(status);
        vehicleMapper.updateById(updateObj);
    }

    private void updateDriverStatus(Long driverId, Integer status) {
        TmsDriverDO updateObj = new TmsDriverDO();
        updateObj.setId(driverId);
        updateObj.setStatus(status);
        driverMapper.updateById(updateObj);
    }

}

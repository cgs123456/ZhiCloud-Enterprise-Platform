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
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_DRIVER_NOT_EXISTS;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_DRIVER_OPTIMISTIC_LOCK_FAIL;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_CONCURRENT_DISPATCH;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_DISPATCH_FAIL_DRIVER;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_DISPATCH_FAIL_VEHICLE;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_NOT_EXISTS;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_OPTIMISTIC_LOCK_FAIL;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_SHIPMENT_STATUS_INVALID;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_NOT_EXISTS;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_OPTIMISTIC_LOCK_FAIL;
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
     * 车辆状态：可用
     */
    private static final int VEHICLE_STATUS_AVAILABLE = 10;
    /**
     * 司机状态：运输中
     */
    private static final int DRIVER_STATUS_IN_TRANSIT = 20;
    /**
     * 司机状态：可用
     */
    private static final int DRIVER_STATUS_AVAILABLE = 10;

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
        // 新单一律从「待发车」起步。VO 已不再暴露 status，这里是显式的值来源——
        // 不依赖「调用方不传就不会落库」这种隐式约定。
        shipment.setStatus(STATUS_PENDING);
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
        // §12.231：先读是为了拿到 version 同时确认存在——少了这一步 updateById 会因
        // version 为空而退化成全量覆盖，等于没加锁。
        TmsShipmentDO old = shipmentMapper.selectById(updateReqVO.getId());
        if (old == null) {
            throw exception(TMS_SHIPMENT_NOT_EXISTS);
        }
        // status 不在 VO 里，因此 BeanUtils 映射过来的 updateObj.status 是 null，
        // MyBatis-Plus 的 updateById 只 SET 非空字段——所以这里不会碰到状态列。
        //
        // 不要把这句读成「有两道防线」：真正挡住 status 的是 VO 缺这个字段（Jackson 直接
        // 丢弃未知字段），updateById 的字段策略只是让 null 不产生 SET 子句。
        // 谁要是把 status 加回 VO，BeanUtils 会把它映射进 DO 并被原样 SET，乐观锁一声不响
        // 就放行了——E2E check 16a/16b 正是为此而设（那边同一行 remark/version 照旧更新，
        // 证明这个接口是活的，不是挂掉的假通过）。
        TmsShipmentDO updateObj = BeanUtils.toBean(updateReqVO, TmsShipmentDO.class);
        updateObj.setVersion(old.getVersion());
        if (shipmentMapper.updateByIdWithVersion(updateObj) == 0) {
            throw exception(TMS_SHIPMENT_OPTIMISTIC_LOCK_FAIL, updateReqVO.getId());
        }
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
        // 5. §12.231 CAS 推进：一条 UPDATE 同时完成「校验仍是待发车」与「写入新状态/车辆/司机/发车时间」。
        //
        // 修好前的写法是 updateById 全量覆盖（读改写）：两次并发 dispatch 都能通过第 2 步的
        // status=10 校验，都写入成功——运单最终落在后提交的那台车/那个司机上，而先提交的
        // 车辆/司机状态也已被改成「运输中」，两边的第 6 步还会互相覆盖。
        // 现在第 2 步的校验只是快速失败（早于副作用），真正的互斥由这条 CAS 完成：
        // 状态机单调推进且没有任何路径把运单改回待发车，所以 status=待发车 本身就是排他的派车资格。
        //
        // 顺序不能反：必须先 CAS 再动第 6 步的车辆/司机。反过来的话失败方已经把车辆/司机的
        // 状态改成「运输中」了，那台车在窗口里是脏的。
        // 注意 status 由 mapper 的 wrapper 显式 set，不放在 entity 上，避免两处写入互相覆盖。
        TmsShipmentDO updateObj = new TmsShipmentDO();
        updateObj.setVehicleId(vehicleId);
        updateObj.setDriverId(driverId);
        updateObj.setDepartureTime(LocalDateTime.now());
        int rows = shipmentMapper.transitStatusIfChanged(shipment.getId(), STATUS_PENDING,
                STATUS_IN_TRANSIT, updateObj);
        if (rows == 0) {
            throw exception(TMS_SHIPMENT_CONCURRENT_DISPATCH, shipment.getId());
        }
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
        updateObj.setActualArrivalTime(LocalDateTime.now());
        if (shipmentMapper.transitStatusIfChanged(id, STATUS_IN_TRANSIT, STATUS_ARRIVED, updateObj) == 0) {
            throw exception(TMS_SHIPMENT_OPTIMISTIC_LOCK_FAIL, id);
        }
        // 车辆/司机恢复可用。注意这两个 helper 也是读改写：先锁读再 CAS 写，
        // 否则并发签收/取消会把「运输中」写回一辆已经派给别的单子的车。
        if (shipment.getVehicleId() != null) {
            updateVehicleStatus(shipment.getVehicleId(), VEHICLE_STATUS_AVAILABLE);
        }
        if (shipment.getDriverId() != null) {
            updateDriverStatus(shipment.getDriverId(), DRIVER_STATUS_AVAILABLE);
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
        updateObj.setSignTime(LocalDateTime.now());
        if (shipmentMapper.transitStatusIfChanged(id, STATUS_ARRIVED, STATUS_SIGNED, updateObj) == 0) {
            throw exception(TMS_SHIPMENT_OPTIMISTIC_LOCK_FAIL, id);
        }
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

    /**
     * 改写车辆状态：锁读 + CAS 写。
     *
     * <p>锁读是为了拿到 version——CAS 需要「我认为它现在是什么版本」这个前提。
     * 不加锁的 selectById 在 RR 隔离下可能读到旧快照，用它去 CAS 会在没有真实冲突时误报
     * 版本不符（§12.230.2 记的同一现象：幂等键回查也要锁读）。</p>
     */
    private void updateVehicleStatus(Long vehicleId, Integer status) {
        TmsVehicleDO vehicle = vehicleMapper.selectByIdForUpdate(vehicleId);
        if (vehicle == null) {
            throw exception(TMS_VEHICLE_NOT_EXISTS);
        }
        TmsVehicleDO updateObj = new TmsVehicleDO();
        updateObj.setId(vehicleId);
        updateObj.setStatus(status);
        updateObj.setVersion(vehicle.getVersion());
        if (vehicleMapper.updateStatusWithVersion(updateObj) == 0) {
            throw exception(TMS_VEHICLE_OPTIMISTIC_LOCK_FAIL, vehicleId);
        }
    }

    /**
     * 改写司机状态，同 {@link #updateVehicleStatus}。
     */
    private void updateDriverStatus(Long driverId, Integer status) {
        TmsDriverDO driver = driverMapper.selectByIdForUpdate(driverId);
        if (driver == null) {
            throw exception(TMS_DRIVER_NOT_EXISTS);
        }
        TmsDriverDO updateObj = new TmsDriverDO();
        updateObj.setId(driverId);
        updateObj.setStatus(status);
        updateObj.setVersion(driver.getVersion());
        if (driverMapper.updateStatusWithVersion(updateObj) == 0) {
            throw exception(TMS_DRIVER_OPTIMISTIC_LOCK_FAIL, driverId);
        }
    }

}

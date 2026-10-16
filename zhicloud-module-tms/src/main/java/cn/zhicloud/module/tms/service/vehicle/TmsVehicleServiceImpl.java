package cn.zhicloud.module.tms.service.vehicle;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehiclePageReqVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehicleSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsVehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_NOT_AVAILABLE;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_NOT_EXISTS;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_OPTIMISTIC_LOCK_FAIL;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_PLATE_NO_DUPLICATE;

/**
 * TMS 车辆 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Validated
public class TmsVehicleServiceImpl implements TmsVehicleService {

    /**
     * 车辆状态：可用
     */
    private static final int STATUS_AVAILABLE = 10;

    @Resource
    private TmsVehicleMapper vehicleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVehicle(TmsVehicleSaveReqVO createReqVO) {
        validatePlateNoUnique(null, createReqVO.getPlateNo());
        TmsVehicleDO vehicle = BeanUtils.toBean(createReqVO, TmsVehicleDO.class);
        vehicleMapper.insert(vehicle);
        return vehicle.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVehicle(TmsVehicleSaveReqVO updateReqVO) {
        validateVehicleExists(updateReqVO.getId());
        validatePlateNoUnique(updateReqVO.getId(), updateReqVO.getPlateNo());
        // §12.231/§12.233：先读是为了拿 version 并确认存在——没有这一步 updateById 会因
        // version 为空退化成无条件全量覆盖，等于把刚加的乐观锁变成了装饰。
        // VO 不暴露 status，所以 BeanUtils 映射过来的 status 是 null，updateById 不 SET 它。
        TmsVehicleDO old = vehicleMapper.selectById(updateReqVO.getId());
        if (old == null) {
            throw exception(TMS_VEHICLE_NOT_EXISTS, updateReqVO.getId());
        }
        TmsVehicleDO updateObj = BeanUtils.toBean(updateReqVO, TmsVehicleDO.class);
        updateObj.setVersion(old.getVersion());
        if (vehicleMapper.updateById(updateObj) == 0) {
            throw exception(TMS_VEHICLE_OPTIMISTIC_LOCK_FAIL, updateReqVO.getId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVehicle(Long id) {
        validateVehicleExists(id);
        vehicleMapper.deleteById(id);
    }

    @Override
    public TmsVehicleDO getVehicle(Long id) {
        return vehicleMapper.selectById(id);
    }

    @Override
    public PageResult<TmsVehicleDO> getVehiclePage(TmsVehiclePageReqVO pageReqVO) {
        return vehicleMapper.selectPage(pageReqVO);
    }

    @Override
    public void validateVehicleAvailable(Long id) {
        TmsVehicleDO vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw exception(TMS_VEHICLE_NOT_EXISTS);
        }
        if (!Integer.valueOf(STATUS_AVAILABLE).equals(vehicle.getStatus())) {
            throw exception(TMS_VEHICLE_NOT_AVAILABLE, id);
        }
    }

    private void validateVehicleExists(Long id) {
        if (vehicleMapper.selectById(id) == null) {
            throw exception(TMS_VEHICLE_NOT_EXISTS);
        }
    }

    private void validatePlateNoUnique(Long id, String plateNo) {
        if (plateNo == null) {
            return;
        }
        TmsVehicleDO vehicle = vehicleMapper.selectByPlateNo(plateNo);
        if (vehicle == null) {
            return;
        }
        if (id == null || !vehicle.getId().equals(id)) {
            throw exception(TMS_VEHICLE_PLATE_NO_DUPLICATE);
        }
    }

}

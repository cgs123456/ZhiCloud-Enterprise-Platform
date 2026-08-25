package cn.zhicloud.module.tms.service.vehicle;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehiclePageReqVO;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehicleSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import jakarta.validation.Valid;

/**
 * TMS 车辆 Service 接口
 *
 * @author zhicloud
 */
public interface TmsVehicleService {

    Long createVehicle(@Valid TmsVehicleSaveReqVO createReqVO);

    void updateVehicle(@Valid TmsVehicleSaveReqVO updateReqVO);

    void deleteVehicle(Long id);

    TmsVehicleDO getVehicle(Long id);

    PageResult<TmsVehicleDO> getVehiclePage(TmsVehiclePageReqVO pageReqVO);

    /**
     * 校验车辆是否可用
     *
     * @param id 车辆编号
     */
    void validateVehicleAvailable(Long id);

}

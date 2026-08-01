package cn.iocoder.yudao.module.tms.service.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo.TmsFleetOperationPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo.TmsFleetOperationSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.vehicle.TmsFleetOperationDO;
import jakarta.validation.Valid;

/**
 * TMS 车队运营 Service 接口
 *
 * @author 芋道源码
 */
public interface TmsFleetOperationService {

    /**
     * 创建车队运营记录
     */
    Long createFleetOperation(@Valid TmsFleetOperationSaveReqVO createReqVO);

    /**
     * 更新车队运营记录
     */
    void updateFleetOperation(@Valid TmsFleetOperationSaveReqVO updateReqVO);

    /**
     * 删除车队运营记录
     */
    void deleteFleetOperation(Long id);

    /**
     * 获取车队运营记录
     */
    TmsFleetOperationDO getFleetOperation(Long id);

    /**
     * 获取车队运营分页
     */
    PageResult<TmsFleetOperationDO> getFleetOperationPage(TmsFleetOperationPageReqVO pageReqVO);

}

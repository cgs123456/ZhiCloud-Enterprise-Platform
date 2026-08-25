package cn.zhicloud.module.tms.service.driver;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.tms.controller.admin.driver.vo.TmsDriverPageReqVO;
import cn.zhicloud.module.tms.controller.admin.driver.vo.TmsDriverSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.driver.TmsDriverDO;
import jakarta.validation.Valid;

/**
 * TMS 司机 Service 接口
 *
 * @author zhicloud
 */
public interface TmsDriverService {

    Long createDriver(@Valid TmsDriverSaveReqVO createReqVO);

    void updateDriver(@Valid TmsDriverSaveReqVO updateReqVO);

    void deleteDriver(Long id);

    TmsDriverDO getDriver(Long id);

    PageResult<TmsDriverDO> getDriverPage(TmsDriverPageReqVO pageReqVO);

    /**
     * 校验司机是否可用
     *
     * @param id 司机编号
     */
    void validateDriverAvailable(Long id);

}

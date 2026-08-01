package cn.iocoder.yudao.module.tms.service.driver;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.driver.vo.TmsDriverPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.driver.vo.TmsDriverSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.driver.TmsDriverDO;
import cn.iocoder.yudao.module.tms.dal.mysql.driver.TmsDriverMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.TMS_DRIVER_LICENSE_NO_DUPLICATE;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.TMS_DRIVER_NOT_AVAILABLE;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.TMS_DRIVER_NOT_EXISTS;

/**
 * TMS 司机 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class TmsDriverServiceImpl implements TmsDriverService {

    /**
     * 司机状态：可用
     */
    private static final int STATUS_AVAILABLE = 10;

    @Resource
    private TmsDriverMapper driverMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDriver(TmsDriverSaveReqVO createReqVO) {
        validateLicenseNoUnique(null, createReqVO.getLicenseNo());
        TmsDriverDO driver = BeanUtils.toBean(createReqVO, TmsDriverDO.class);
        driverMapper.insert(driver);
        return driver.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDriver(TmsDriverSaveReqVO updateReqVO) {
        validateDriverExists(updateReqVO.getId());
        validateLicenseNoUnique(updateReqVO.getId(), updateReqVO.getLicenseNo());
        TmsDriverDO updateObj = BeanUtils.toBean(updateReqVO, TmsDriverDO.class);
        driverMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDriver(Long id) {
        validateDriverExists(id);
        driverMapper.deleteById(id);
    }

    @Override
    public TmsDriverDO getDriver(Long id) {
        return driverMapper.selectById(id);
    }

    @Override
    public PageResult<TmsDriverDO> getDriverPage(TmsDriverPageReqVO pageReqVO) {
        return driverMapper.selectPage(pageReqVO);
    }

    @Override
    public void validateDriverAvailable(Long id) {
        TmsDriverDO driver = driverMapper.selectById(id);
        if (driver == null) {
            throw exception(TMS_DRIVER_NOT_EXISTS);
        }
        if (!Integer.valueOf(STATUS_AVAILABLE).equals(driver.getStatus())) {
            throw exception(TMS_DRIVER_NOT_AVAILABLE, id);
        }
    }

    private void validateDriverExists(Long id) {
        if (driverMapper.selectById(id) == null) {
            throw exception(TMS_DRIVER_NOT_EXISTS);
        }
    }

    private void validateLicenseNoUnique(Long id, String licenseNo) {
        if (licenseNo == null) {
            return;
        }
        TmsDriverDO driver = driverMapper.selectByLicenseNo(licenseNo);
        if (driver == null) {
            return;
        }
        if (id == null || !driver.getId().equals(id)) {
            throw exception(TMS_DRIVER_LICENSE_NO_DUPLICATE);
        }
    }

}

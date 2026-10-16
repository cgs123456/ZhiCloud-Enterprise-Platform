package cn.zhicloud.module.tms.dal.mysql.driver;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.driver.vo.TmsDriverPageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.driver.TmsDriverDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 司机 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TmsDriverMapper extends BaseMapperX<TmsDriverDO> {

    default PageResult<TmsDriverDO> selectPage(TmsDriverPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsDriverDO>()
                .likeIfPresent(TmsDriverDO::getName, reqVO.getName())
                .eqIfPresent(TmsDriverDO::getPhone, reqVO.getPhone())
                .eqIfPresent(TmsDriverDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsDriverDO::getStatus, reqVO.getStatus())
                .orderByDesc(TmsDriverDO::getId));
    }

    default TmsDriverDO selectByLicenseNo(String licenseNo) {
        return selectOne(TmsDriverDO::getLicenseNo, licenseNo);
    }

    default List<TmsDriverDO> selectListByStatus(Integer status) {
        return selectList(TmsDriverDO::getStatus, status);
    }

    /**
     * 锁读司机，同 {@code TmsVehicleMapper#selectByIdForUpdate}：CAS 前取版本号必须是锁读。
     */
    default TmsDriverDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<TmsDriverDO>()
                .eq(TmsDriverDO::getId, id)
                .last("FOR UPDATE"));
    }

    /**
     * 按已知版本 CAS 改写司机状态，同 {@code TmsVehicleMapper#updateStatusWithVersion}。
     */
    default int updateStatusWithVersion(TmsDriverDO updateObj) {
        return updateById(updateObj);
    }

}

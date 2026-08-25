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

}

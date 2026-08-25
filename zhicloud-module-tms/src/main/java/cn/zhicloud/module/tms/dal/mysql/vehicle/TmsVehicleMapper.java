package cn.zhicloud.module.tms.dal.mysql.vehicle;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsVehiclePageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 车辆 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TmsVehicleMapper extends BaseMapperX<TmsVehicleDO> {

    default PageResult<TmsVehicleDO> selectPage(TmsVehiclePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsVehicleDO>()
                .likeIfPresent(TmsVehicleDO::getPlateNo, reqVO.getPlateNo())
                .eqIfPresent(TmsVehicleDO::getCarrierId, reqVO.getCarrierId())
                .eqIfPresent(TmsVehicleDO::getStatus, reqVO.getStatus())
                .orderByDesc(TmsVehicleDO::getId));
    }

    default TmsVehicleDO selectByPlateNo(String plateNo) {
        return selectOne(TmsVehicleDO::getPlateNo, plateNo);
    }

    default List<TmsVehicleDO> selectListByStatus(Integer status) {
        return selectList(TmsVehicleDO::getStatus, status);
    }

}

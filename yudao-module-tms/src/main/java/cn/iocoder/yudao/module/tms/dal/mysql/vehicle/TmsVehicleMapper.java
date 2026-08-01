package cn.iocoder.yudao.module.tms.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo.TmsVehiclePageReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 车辆 Mapper
 *
 * @author yudao
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

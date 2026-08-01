package cn.iocoder.yudao.module.tms.dal.mysql.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo.TmsFleetOperationPageReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.vehicle.TmsFleetOperationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 车队运营 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface TmsFleetOperationMapper extends BaseMapperX<TmsFleetOperationDO> {

    default PageResult<TmsFleetOperationDO> selectPage(TmsFleetOperationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<TmsFleetOperationDO>()
                .eqIfPresent(TmsFleetOperationDO::getVehicleId, reqVO.getVehicleId())
                .betweenIfPresent(TmsFleetOperationDO::getOperationDate, reqVO.getOperationDate())
                .orderByDesc(TmsFleetOperationDO::getOperationDate));
    }

}

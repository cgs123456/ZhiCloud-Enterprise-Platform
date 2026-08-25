package cn.zhicloud.module.tms.dal.mysql.vehicle;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.controller.admin.vehicle.vo.TmsFleetOperationPageReqVO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsFleetOperationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * TMS 车队运营 Mapper
 *
 * @author 智云
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

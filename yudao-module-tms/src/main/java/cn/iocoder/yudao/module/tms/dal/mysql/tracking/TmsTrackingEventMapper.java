package cn.iocoder.yudao.module.tms.dal.mysql.tracking;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.dal.dataobject.tracking.TmsTrackingEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 跟踪事件 Mapper
 *
 * @author yudao
 */
@Mapper
public interface TmsTrackingEventMapper extends BaseMapperX<TmsTrackingEventDO> {

    default List<TmsTrackingEventDO> selectListByShipmentId(Long shipmentId) {
        return selectList(new LambdaQueryWrapperX<TmsTrackingEventDO>()
                .eq(TmsTrackingEventDO::getShipmentId, shipmentId)
                .orderByAsc(TmsTrackingEventDO::getEventTime));
    }

}

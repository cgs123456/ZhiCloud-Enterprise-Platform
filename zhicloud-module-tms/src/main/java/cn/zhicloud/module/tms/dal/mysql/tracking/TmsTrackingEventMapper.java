package cn.zhicloud.module.tms.dal.mysql.tracking;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.tms.dal.dataobject.tracking.TmsTrackingEventDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS 跟踪事件 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface TmsTrackingEventMapper extends BaseMapperX<TmsTrackingEventDO> {

    default List<TmsTrackingEventDO> selectListByShipmentId(Long shipmentId) {
        return selectList(new LambdaQueryWrapperX<TmsTrackingEventDO>()
                .eq(TmsTrackingEventDO::getShipmentId, shipmentId)
                .orderByAsc(TmsTrackingEventDO::getEventTime));
    }

}

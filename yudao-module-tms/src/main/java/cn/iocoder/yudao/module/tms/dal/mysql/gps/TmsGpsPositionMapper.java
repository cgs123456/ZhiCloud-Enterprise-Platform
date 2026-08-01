package cn.iocoder.yudao.module.tms.dal.mysql.gps;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.tms.dal.dataobject.gps.TmsGpsPositionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * TMS GPS 定位记录 Mapper
 *
 * @author yudao
 */
@Mapper
public interface TmsGpsPositionMapper extends BaseMapperX<TmsGpsPositionDO> {

    /**
     * 获取车辆最新一条 GPS 定位
     */
    default TmsGpsPositionDO selectLatestByVehicleId(Long vehicleId) {
        return selectOne(new LambdaQueryWrapperX<TmsGpsPositionDO>()
                .eq(TmsGpsPositionDO::getVehicleId, vehicleId)
                .orderByDesc(TmsGpsPositionDO::getReportTime)
                .last("LIMIT 1"));
    }

    /**
     * 获取运单的 GPS 轨迹（按时间正序）
     */
    default List<TmsGpsPositionDO> selectTrackByShipmentId(Long shipmentId) {
        return selectList(new LambdaQueryWrapperX<TmsGpsPositionDO>()
                .eq(TmsGpsPositionDO::getShipmentId, shipmentId)
                .orderByAsc(TmsGpsPositionDO::getReportTime));
    }

    /**
     * 获取车辆在指定时间范围内的 GPS 轨迹
     */
    default List<TmsGpsPositionDO> selectTrackByVehicleIdAndTimeRange(Long vehicleId,
                                                                       java.time.LocalDateTime startTime,
                                                                       java.time.LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<TmsGpsPositionDO>()
                .eq(TmsGpsPositionDO::getVehicleId, vehicleId)
                .ge(TmsGpsPositionDO::getReportTime, startTime)
                .le(TmsGpsPositionDO::getReportTime, endTime)
                .orderByAsc(TmsGpsPositionDO::getReportTime));
    }

}

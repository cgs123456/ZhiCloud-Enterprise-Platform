package cn.iocoder.yudao.module.tms.service.gps;

import cn.iocoder.yudao.module.tms.controller.admin.gps.vo.TmsGpsPositionSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.gps.TmsGpsPositionDO;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * TMS GPS 定位 Service 接口
 *
 * @author yudao
 */
public interface TmsGpsService {

    /**
     * 上报 GPS 定位
     *
     * @param saveReqVO 上报信息
     * @return 编号
     */
    Long reportPosition(@Valid TmsGpsPositionSaveReqVO saveReqVO);

    /**
     * 获取车辆最新位置
     *
     * @param vehicleId 车辆编号
     * @return 最新定位
     */
    TmsGpsPositionDO getLatestPosition(Long vehicleId);

    /**
     * 获取运单的 GPS 轨迹
     *
     * @param shipmentId 运单编号
     * @return 轨迹列表（按时间正序）
     */
    List<TmsGpsPositionDO> getShipmentTrack(Long shipmentId);

    /**
     * 获取车辆在指定时间范围内的 GPS 轨迹
     *
     * @param vehicleId 车辆编号
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 轨迹列表
     */
    List<TmsGpsPositionDO> getVehicleTrack(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime);

}

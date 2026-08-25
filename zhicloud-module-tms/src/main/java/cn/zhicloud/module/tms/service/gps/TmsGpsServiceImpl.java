package cn.zhicloud.module.tms.service.gps;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.tms.controller.admin.gps.vo.TmsGpsPositionSaveReqVO;
import cn.zhicloud.module.tms.dal.dataobject.gps.TmsGpsPositionDO;
import cn.zhicloud.module.tms.dal.dataobject.vehicle.TmsVehicleDO;
import cn.zhicloud.module.tms.dal.mysql.gps.TmsGpsPositionMapper;
import cn.zhicloud.module.tms.dal.mysql.vehicle.TmsVehicleMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.tms.enums.ErrorCodeConstants.TMS_VEHICLE_NOT_EXISTS;

/**
 * TMS GPS 定位 Service 实现类
 *
 * <p>
 * 设计说明：
 * GPS 数据来源为外部 GPS 设备/平台通过 API 上报，本模块负责接收、存储和查询。
 * 实际 GPS 硬件对接（如北斗/GPS 定位终端）通过 MQTT/HTTP 推送至本接口，
 * 本系统不直接与硬件通信，只提供标准化接收和查询能力。
 *
 * @author zhicloud
 */
@Service
@Validated
public class TmsGpsServiceImpl implements TmsGpsService {

    @Resource
    private TmsGpsPositionMapper gpsPositionMapper;
    @Resource
    private TmsVehicleMapper vehicleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportPosition(TmsGpsPositionSaveReqVO saveReqVO) {
        // 校验车辆存在
        TmsVehicleDO vehicle = vehicleMapper.selectById(saveReqVO.getVehicleId());
        if (vehicle == null) {
            throw exception(TMS_VEHICLE_NOT_EXISTS);
        }
        TmsGpsPositionDO position = BeanUtils.toBean(saveReqVO, TmsGpsPositionDO.class);
        // 上报时间为空时取当前时间
        if (position.getReportTime() == null) {
            position.setReportTime(LocalDateTime.now());
        }
        gpsPositionMapper.insert(position);
        return position.getId();
    }

    @Override
    public TmsGpsPositionDO getLatestPosition(Long vehicleId) {
        return gpsPositionMapper.selectLatestByVehicleId(vehicleId);
    }

    @Override
    public List<TmsGpsPositionDO> getShipmentTrack(Long shipmentId) {
        return gpsPositionMapper.selectTrackByShipmentId(shipmentId);
    }

    @Override
    public List<TmsGpsPositionDO> getVehicleTrack(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        return gpsPositionMapper.selectTrackByVehicleIdAndTimeRange(vehicleId, startTime, endTime);
    }

}

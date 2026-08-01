package cn.iocoder.yudao.module.tms.controller.admin.gps;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.gps.vo.TmsGpsPositionRespVO;
import cn.iocoder.yudao.module.tms.controller.admin.gps.vo.TmsGpsPositionSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.gps.TmsGpsPositionDO;
import cn.iocoder.yudao.module.tms.service.gps.TmsGpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - TMS GPS 定位")
@RestController
@RequestMapping("/tms/gps")
@Validated
public class TmsGpsController {

    @Resource
    private TmsGpsService gpsService;

    @PostMapping("/report")
    @Operation(summary = "上报 GPS 定位")
    @PreAuthorize("@ss.hasPermission('tms:gps:report')")
    public CommonResult<Long> reportPosition(@Valid @RequestBody TmsGpsPositionSaveReqVO saveReqVO) {
        return success(gpsService.reportPosition(saveReqVO));
    }

    @GetMapping("/latest-position")
    @Operation(summary = "获取车辆最新位置")
    @Parameter(name = "vehicleId", description = "车辆编号", required = true, example = "3072")
    @PreAuthorize("@ss.hasPermission('tms:gps:query')")
    public CommonResult<TmsGpsPositionRespVO> getLatestPosition(@RequestParam("vehicleId") Long vehicleId) {
        TmsGpsPositionDO position = gpsService.getLatestPosition(vehicleId);
        return success(BeanUtils.toBean(position, TmsGpsPositionRespVO.class));
    }

    @GetMapping("/shipment-track")
    @Operation(summary = "获取运单 GPS 轨迹")
    @Parameter(name = "shipmentId", description = "运单编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('tms:gps:query')")
    public CommonResult<List<TmsGpsPositionRespVO>> getShipmentTrack(@RequestParam("shipmentId") Long shipmentId) {
        List<TmsGpsPositionDO> list = gpsService.getShipmentTrack(shipmentId);
        return success(BeanUtils.toBean(list, TmsGpsPositionRespVO.class));
    }

    @GetMapping("/vehicle-track")
    @Operation(summary = "获取车辆时间范围内的 GPS 轨迹")
    @Parameter(name = "vehicleId", description = "车辆编号", required = true, example = "3072")
    @Parameter(name = "startTime", description = "开始时间", required = true, example = "2024-01-01T00:00:00")
    @Parameter(name = "endTime", description = "结束时间", required = true, example = "2024-01-02T00:00:00")
    @PreAuthorize("@ss.hasPermission('tms:gps:query')")
    public CommonResult<List<TmsGpsPositionRespVO>> getVehicleTrack(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endTime) {
        List<TmsGpsPositionDO> list = gpsService.getVehicleTrack(vehicleId, startTime, endTime);
        return success(BeanUtils.toBean(list, TmsGpsPositionRespVO.class));
    }

}

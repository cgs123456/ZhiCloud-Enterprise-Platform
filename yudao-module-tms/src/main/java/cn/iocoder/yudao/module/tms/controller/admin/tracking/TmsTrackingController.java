package cn.iocoder.yudao.module.tms.controller.admin.tracking;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.tracking.vo.TmsTrackingEventRespVO;
import cn.iocoder.yudao.module.tms.controller.admin.tracking.vo.TmsTrackingEventSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.tracking.TmsTrackingEventDO;
import cn.iocoder.yudao.module.tms.service.tracking.TmsTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - TMS 跟踪事件")
@RestController
@RequestMapping("/tms/tracking")
@Validated
public class TmsTrackingController {

    @Resource
    private TmsTrackingService trackingService;

    @PostMapping("/report")
    @Operation(summary = "上报跟踪事件")
    @PreAuthorize("@ss.hasPermission('tms:tracking:create')")
    public CommonResult<Long> reportEvent(@Valid @RequestBody TmsTrackingEventSaveReqVO saveReqVO) {
        return success(trackingService.reportEvent(saveReqVO));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除跟踪事件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('tms:tracking:delete')")
    public CommonResult<Boolean> deleteTrackingEvent(@RequestParam("id") Long id) {
        trackingService.deleteTrackingEvent(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得跟踪事件")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:tracking:query')")
    public CommonResult<TmsTrackingEventRespVO> getTrackingEvent(@RequestParam("id") Long id) {
        TmsTrackingEventDO event = trackingService.getTrackingEvent(id);
        return success(BeanUtils.toBean(event, TmsTrackingEventRespVO.class));
    }

    @GetMapping("/list-by-shipment")
    @Operation(summary = "获得运单的跟踪事件列表")
    @Parameter(name = "shipmentId", description = "运单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('tms:tracking:query')")
    public CommonResult<List<TmsTrackingEventRespVO>> getTrackingEventListByShipment(
            @RequestParam("shipmentId") Long shipmentId) {
        List<TmsTrackingEventDO> list = trackingService.getTrackingEventListByShipmentId(shipmentId);
        return success(BeanUtils.toBean(list, TmsTrackingEventRespVO.class));
    }

}

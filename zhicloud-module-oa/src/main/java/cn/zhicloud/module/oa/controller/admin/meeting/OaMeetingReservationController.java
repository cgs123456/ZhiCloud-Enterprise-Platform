package cn.zhicloud.module.oa.controller.admin.meeting;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingReservationPageReqVO;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingReservationRespVO;
import cn.zhicloud.module.oa.controller.admin.meeting.vo.OaMeetingReservationSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.meeting.OaMeetingReservationDO;
import cn.zhicloud.module.oa.service.meeting.OaMeetingReservationService;
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

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 会议室预约")
@RestController
@RequestMapping("/oa/meeting-reservation")
@Validated
public class OaMeetingReservationController {

    @Resource
    private OaMeetingReservationService meetingReservationService;

    @PostMapping("/create")
    @Operation(summary = "创建会议室预约")
    @PreAuthorize("@ss.hasPermission('oa:meeting-reservation:create')")
    public CommonResult<Long> createReservation(@Valid @RequestBody OaMeetingReservationSaveReqVO createReqVO) {
        return success(meetingReservationService.createReservation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新会议室预约")
    @PreAuthorize("@ss.hasPermission('oa:meeting-reservation:update')")
    public CommonResult<Boolean> updateReservation(@Valid @RequestBody OaMeetingReservationSaveReqVO updateReqVO) {
        meetingReservationService.updateReservation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除会议室预约")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:meeting-reservation:delete')")
    public CommonResult<Boolean> deleteReservation(@RequestParam("id") Long id) {
        meetingReservationService.deleteReservation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得会议室预约")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:meeting-reservation:query')")
    public CommonResult<OaMeetingReservationRespVO> getReservation(@RequestParam("id") Long id) {
        OaMeetingReservationDO reservation = meetingReservationService.getReservation(id);
        return success(BeanUtils.toBean(reservation, OaMeetingReservationRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得会议室预约分页")
    @PreAuthorize("@ss.hasPermission('oa:meeting-reservation:query')")
    public CommonResult<PageResult<OaMeetingReservationRespVO>> getReservationPage(
            @Valid OaMeetingReservationPageReqVO pageReqVO) {
        PageResult<OaMeetingReservationDO> pageResult = meetingReservationService.getReservationPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaMeetingReservationRespVO.class));
    }

    @GetMapping("/check-conflict")
    @Operation(summary = "校验会议室时段冲突")
    @PreAuthorize("@ss.hasPermission('oa:meeting-reservation:query')")
    public CommonResult<Boolean> checkTimeConflict(@RequestParam("roomId") Long roomId,
                                                   @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
                                                   @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
                                                   @RequestParam(value = "excludeReservationId", required = false) Long excludeReservationId) {
        return success(meetingReservationService.checkTimeConflict(roomId, startTime, endTime, excludeReservationId));
    }

}

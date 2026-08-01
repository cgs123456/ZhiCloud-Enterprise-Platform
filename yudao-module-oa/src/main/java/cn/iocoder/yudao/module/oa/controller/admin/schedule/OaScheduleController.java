package cn.iocoder.yudao.module.oa.controller.admin.schedule;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaSchedulePageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaScheduleRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.schedule.vo.OaScheduleSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.schedule.OaScheduleDO;
import cn.iocoder.yudao.module.oa.service.schedule.OaScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * OA 日程 Controller
 *
 * @author 芋道源码
 */
@Tag(name = "管理后台 - OA 日程")
@RestController
@RequestMapping("/oa/schedule")
public class OaScheduleController {

    @Resource
    private OaScheduleService scheduleService;

    @PostMapping("/create")
    @Operation(summary = "创建日程")
    @PreAuthorize("@ss.hasPermission('oa:schedule:create')")
    public CommonResult<Long> createSchedule(@Valid @RequestBody OaScheduleSaveReqVO createReqVO) {
        return success(scheduleService.createSchedule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新日程")
    @PreAuthorize("@ss.hasPermission('oa:schedule:update')")
    public CommonResult<Boolean> updateSchedule(@Valid @RequestBody OaScheduleSaveReqVO updateReqVO) {
        scheduleService.updateSchedule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除日程")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:schedule:delete')")
    public CommonResult<Boolean> deleteSchedule(@RequestParam("id") Long id) {
        scheduleService.deleteSchedule(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取日程")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:schedule:query')")
    public CommonResult<OaScheduleRespVO> getSchedule(@RequestParam("id") Long id) {
        OaScheduleDO schedule = scheduleService.getSchedule(id);
        return success(BeanUtils.toBean(schedule, OaScheduleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取日程分页")
    @PreAuthorize("@ss.hasPermission('oa:schedule:query')")
    public CommonResult<PageResult<OaScheduleRespVO>> getSchedulePage(@Valid OaSchedulePageReqVO pageReqVO) {
        PageResult<OaScheduleDO> pageResult = scheduleService.getSchedulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaScheduleRespVO.class));
    }

    @GetMapping("/list-by-user")
    @Operation(summary = "获取用户指定时间范围内的日程")
    @PreAuthorize("@ss.hasPermission('oa:schedule:query')")
    public CommonResult<List<OaScheduleRespVO>> getScheduleListByUser(
            @RequestParam("userId") Long userId,
            @RequestParam("startTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<OaScheduleDO> list = scheduleService.getScheduleListByUser(userId, startTime, endTime);
        return success(BeanUtils.toBean(list, OaScheduleRespVO.class));
    }

    @PutMapping("/complete")
    @Operation(summary = "完成日程")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:schedule:update')")
    public CommonResult<Boolean> completeSchedule(@RequestParam("id") Long id) {
        scheduleService.completeSchedule(id);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消日程")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:schedule:update')")
    public CommonResult<Boolean> cancelSchedule(@RequestParam("id") Long id) {
        scheduleService.cancelSchedule(id);
        return success(true);
    }

}

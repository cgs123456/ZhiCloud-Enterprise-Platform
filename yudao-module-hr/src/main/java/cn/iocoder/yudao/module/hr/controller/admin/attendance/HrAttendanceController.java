package cn.iocoder.yudao.module.hr.controller.admin.attendance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendanceMonthlySummaryRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendanceRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.attendance.vo.HrAttendanceSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.attendance.HrAttendanceDO;
import cn.iocoder.yudao.module.hr.service.attendance.HrAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 考勤记录")
@RestController
@RequestMapping("/hr/attendance")
@Validated
public class HrAttendanceController {

    @Resource
    private HrAttendanceService attendanceService;

    @PostMapping("/create")
    @Operation(summary = "创建考勤记录")
    @PreAuthorize("@ss.hasPermission('hr:attendance:create')")
    public CommonResult<Long> createAttendance(@Valid @RequestBody HrAttendanceSaveReqVO createReqVO) {
        return success(attendanceService.createAttendance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新考勤记录")
    @PreAuthorize("@ss.hasPermission('hr:attendance:update')")
    public CommonResult<Boolean> updateAttendance(@Valid @RequestBody HrAttendanceSaveReqVO updateReqVO) {
        attendanceService.updateAttendance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除考勤记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:attendance:delete')")
    public CommonResult<Boolean> deleteAttendance(@RequestParam("id") Long id) {
        attendanceService.deleteAttendance(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得考勤记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:attendance:query')")
    public CommonResult<HrAttendanceRespVO> getAttendance(@RequestParam("id") Long id) {
        HrAttendanceDO attendance = attendanceService.getAttendance(id);
        return success(BeanUtils.toBean(attendance, HrAttendanceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得考勤记录分页")
    @PreAuthorize("@ss.hasPermission('hr:attendance:query')")
    public CommonResult<PageResult<HrAttendanceRespVO>> getAttendancePage(@Valid HrAttendancePageReqVO pageReqVO) {
        PageResult<HrAttendanceDO> pageResult = attendanceService.getAttendancePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrAttendanceRespVO.class));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "获得员工月度考勤汇总")
    @Parameter(name = "employeeId", description = "员工编号", required = true, example = "2048")
    @Parameter(name = "month", description = "月份（yyyyMM）", required = true, example = "202401")
    @PreAuthorize("@ss.hasPermission('hr:attendance:query')")
    public CommonResult<HrAttendanceMonthlySummaryRespVO> getMonthlySummary(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("month") String month) {
        return success(attendanceService.getMonthlySummary(employeeId, month));
    }

}
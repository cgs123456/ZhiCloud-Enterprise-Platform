package cn.zhicloud.module.hr.controller.admin.hrm.attendance;

import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo.*;
import cn.zhicloud.module.hr.service.hrm.attendance.HrmAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 考勤 Controller（前端 views/hrm/attendance 契约）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 考勤")
@RestController
@RequestMapping("/hrm/attendance")
@Validated
public class HrmAttendanceController {

    @Resource
    private HrmAttendanceService attendanceService;

    // ========== 打卡 ==========

    @GetMapping("/clock/page")
    @Operation(summary = "获得考勤打卡分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:query')")
    public CommonResult<PageResult<HrmAttendanceClockRespVO>> getClockPage(@Valid HrmAttendanceClockPageReqVO reqVO) {
        return success(attendanceService.getClockPage(reqVO));
    }

    @GetMapping("/clock/get")
    @Operation(summary = "获得考勤打卡详情")
    @Parameter(name = "id", description = "打卡记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:query')")
    public CommonResult<HrmAttendanceClockRespVO> getClock(@RequestParam("id") Long id) {
        return success(attendanceService.getClock(id));
    }

    @GetMapping("/clock/get-shift")
    @Operation(summary = "获得员工实际班次和允许打卡时间")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @Parameter(name = "attendanceTime", description = "应打卡日期（yyyy-MM-dd）", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:query')")
    public CommonResult<HrmAttendanceClockShiftRespVO> getClockShift(@RequestParam("employeeId") Long employeeId,
                                                                    @RequestParam("attendanceTime") String attendanceTime) {
        return success(attendanceService.getClockShift(employeeId, attendanceTime));
    }

    @PostMapping("/clock/create")
    @Operation(summary = "新增考勤打卡")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:create')")
    public CommonResult<Long> createClock(@Valid @RequestBody HrmAttendanceClockSaveReqVO reqVO) {
        return success(attendanceService.createClock(reqVO));
    }

    @PutMapping("/clock/update")
    @Operation(summary = "修改考勤打卡")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:update')")
    public CommonResult<Boolean> updateClock(@Valid @RequestBody HrmAttendanceClockSaveReqVO reqVO) {
        attendanceService.updateClock(reqVO);
        return success(true);
    }

    @DeleteMapping("/clock/delete")
    @Operation(summary = "删除考勤打卡")
    @Parameter(name = "id", description = "打卡记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:delete')")
    public CommonResult<Boolean> deleteClock(@RequestParam("id") Long id) {
        attendanceService.deleteClock(id);
        return success(true);
    }

    @DeleteMapping("/clock/delete-list")
    @Operation(summary = "批量删除考勤打卡")
    @Parameter(name = "ids", description = "打卡记录编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:delete')")
    public CommonResult<Boolean> deleteClockList(@RequestParam("ids") String ids) {
        attendanceService.deleteClockList(splitIds(ids));
        return success(true);
    }

    @GetMapping("/clock/export-excel")
    @Operation(summary = "导出考勤打卡 Excel")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportClockExcel(@Valid HrmAttendanceClockPageReqVO reqVO, HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "考勤打卡.xls", "数据",
                HrmAttendanceClockExportExcelVO.class, attendanceService.getClockExportList(reqVO));
    }

    // ========== 考勤组 ==========

    @GetMapping("/group/page")
    @Operation(summary = "获得考勤组分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:group:query')")
    public CommonResult<PageResult<HrmAttendanceGroupRespVO>> getGroupPage(@Valid PageParam pageParam,
                                                                          @RequestParam(value = "name", required = false) String name) {
        return success(attendanceService.getGroupPage(pageParam.getPageNo(), pageParam.getPageSize(), name));
    }

    @GetMapping("/group/get")
    @Operation(summary = "获得考勤组详情")
    @Parameter(name = "id", description = "考勤组编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:group:query')")
    public CommonResult<HrmAttendanceGroupRespVO> getGroup(@RequestParam("id") Long id) {
        return success(attendanceService.getGroup(id));
    }

    @GetMapping("/group/my")
    @Operation(summary = "获得员工所在考勤组")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:group:query')")
    public CommonResult<HrmAttendanceGroupRespVO> getMyGroup(@RequestParam("employeeId") Long employeeId) {
        return success(attendanceService.getMyGroup(employeeId));
    }

    @PostMapping("/group/create")
    @Operation(summary = "创建考勤组")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:group:create')")
    public CommonResult<Long> createGroup(@Valid @RequestBody HrmAttendanceGroupSaveReqVO reqVO) {
        return success(attendanceService.createGroup(reqVO));
    }

    @PutMapping("/group/update")
    @Operation(summary = "修改考勤组")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:group:update')")
    public CommonResult<Boolean> updateGroup(@Valid @RequestBody HrmAttendanceGroupSaveReqVO reqVO) {
        attendanceService.updateGroup(reqVO);
        return success(true);
    }

    @DeleteMapping("/group/delete")
    @Operation(summary = "删除考勤组")
    @Parameter(name = "id", description = "考勤组编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:group:delete')")
    public CommonResult<Boolean> deleteGroup(@RequestParam("id") Long id) {
        attendanceService.deleteGroup(id);
        return success(true);
    }

    // ========== 节假日 ==========

    @GetMapping("/holiday/page")
    @Operation(summary = "获得考勤节假日分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:holiday:query')")
    public CommonResult<PageResult<HrmAttendanceHolidayRespVO>> getHolidayPage(@Valid HrmAttendanceHolidayPageReqVO reqVO) {
        return success(attendanceService.getHolidayPage(reqVO));
    }

    @GetMapping("/holiday/get")
    @Operation(summary = "获得考勤节假日详情")
    @Parameter(name = "id", description = "节假日编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:holiday:query')")
    public CommonResult<HrmAttendanceHolidayRespVO> getHoliday(@RequestParam("id") Long id) {
        return success(attendanceService.getHoliday(id));
    }

    @PostMapping("/holiday/create")
    @Operation(summary = "创建考勤节假日")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:holiday:create')")
    public CommonResult<Long> createHoliday(@Valid @RequestBody HrmAttendanceHolidaySaveReqVO reqVO) {
        return success(attendanceService.createHoliday(reqVO));
    }

    @PutMapping("/holiday/update")
    @Operation(summary = "修改考勤节假日")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:holiday:update')")
    public CommonResult<Boolean> updateHoliday(@Valid @RequestBody HrmAttendanceHolidaySaveReqVO reqVO) {
        attendanceService.updateHoliday(reqVO);
        return success(true);
    }

    @DeleteMapping("/holiday/delete")
    @Operation(summary = "删除考勤节假日")
    @Parameter(name = "id", description = "节假日编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:holiday:delete')")
    public CommonResult<Boolean> deleteHoliday(@RequestParam("id") Long id) {
        attendanceService.deleteHoliday(id);
        return success(true);
    }

    // ========== 请假（读侧） ==========

    @GetMapping("/leave/page")
    @Operation(summary = "获得请假分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:leave:query')")
    public CommonResult<PageResult<HrmAttendanceLeaveRespVO>> getLeavePage(@Valid HrmAttendanceLeavePageReqVO reqVO) {
        return success(attendanceService.getLeavePage(reqVO));
    }

    @GetMapping("/leave/get")
    @Operation(summary = "获得请假详情")
    @Parameter(name = "id", description = "请假记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:leave:query')")
    public CommonResult<HrmAttendanceLeaveRespVO> getLeave(@RequestParam("id") Long id) {
        return success(attendanceService.getLeave(id));
    }

    @GetMapping("/leave/export-excel")
    @Operation(summary = "导出请假 Excel")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:leave:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportLeaveExcel(@Valid HrmAttendanceLeavePageReqVO reqVO, HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "请假记录.xls", "数据",
                HrmAttendanceLeaveExportExcelVO.class, attendanceService.getLeaveExportList(reqVO));
    }

    // ========== 统计 ==========

    @GetMapping("/statistics/month-record-page")
    @Operation(summary = "获得月度考勤汇总分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:statistics:query')")
    public CommonResult<PageResult<HrmAttendanceMonthRecordRespVO>> getMonthRecordPage(
            @Valid HrmAttendanceMonthRecordPageReqVO reqVO) {
        return success(attendanceService.getMonthRecordPage(reqVO));
    }

    @GetMapping("/statistics/month-daily-page")
    @Operation(summary = "获得月度打卡概况分页")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:statistics:query')")
    public CommonResult<PageResult<HrmAttendanceMonthDailyOverviewRespVO>> getMonthDailyPage(
            @Valid HrmAttendanceMonthRecordPageReqVO reqVO) {
        return success(attendanceService.getMonthDailyPage(reqVO));
    }

    @GetMapping("/statistics/month-detail")
    @Operation(summary = "获得月度考勤详情")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:statistics:query')")
    public CommonResult<HrmAttendanceMonthDetailRespVO> getMonthDetail(@RequestParam("employeeId") Long employeeId,
                                                                       @RequestParam("year") Integer year,
                                                                       @RequestParam("month") Integer month) {
        return success(attendanceService.getMonthDetail(employeeId, year, month));
    }

    @GetMapping("/statistics/daily-detail")
    @Operation(summary = "获得每日考勤明细")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @Parameter(name = "date", description = "日期（yyyy-MM-dd）", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:attendance:statistics:query')")
    public CommonResult<HrmAttendanceDailyDetailRespVO> getDailyDetail(@RequestParam("employeeId") Long employeeId,
                                                                       @RequestParam("date") String date) {
        return success(attendanceService.getDailyDetail(employeeId, date));
    }

    @GetMapping("/statistics/month-record-export-excel")
    @Operation(summary = "导出月度考勤汇总 Excel")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:statistics:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMonthRecordExcel(@Valid HrmAttendanceMonthRecordPageReqVO reqVO, HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "员工月度考勤汇总.xls", "数据",
                HrmAttendanceMonthRecordExportExcelVO.class, attendanceService.getMonthRecordExportList(reqVO));
    }

    @GetMapping("/statistics/month-daily-export-excel")
    @Operation(summary = "导出月度打卡概况 Excel")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:statistics:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportMonthDailyExcel(@Valid HrmAttendanceMonthRecordPageReqVO reqVO, HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "月度打卡概况.xls", "数据",
                HrmAttendanceMonthDailyExportExcelVO.class, attendanceService.getMonthDailyExportList(reqVO));
    }

    private List<Long> splitIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

}

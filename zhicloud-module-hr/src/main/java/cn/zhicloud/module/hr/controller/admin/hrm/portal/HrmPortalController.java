package cn.zhicloud.module.hr.controller.admin.hrm.portal;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.portal.vo.*;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;
import cn.zhicloud.module.hr.service.hrm.portal.HrmPortalService;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 员工自助端（portal）Controller
 *
 * 员工自助入口：员工档案、考勤、社保、绩效、工资条、首页日历。
 * 所有接口以「当前登录用户绑定的员工」为数据边界。
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 员工自助端")
@RestController
@RequestMapping("/hrm/portal")
@Validated
public class HrmPortalController {

    @Resource
    private HrmPortalService portalService;

    // ========== 员工档案 ==========

    @GetMapping("/employee/get-bind-status")
    @Operation(summary = "获得当前账号的员工绑定状态")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<Boolean> getBindStatus() {
        return success(portalService.getBindStatus());
    }

    @GetMapping("/employee/get")
    @Operation(summary = "获得当前员工档案")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmPortalEmployeeVO> getEmployee() {
        return success(portalService.getEmployee());
    }

    @PutMapping("/employee/update")
    @Operation(summary = "修改当前员工档案")
    @PreAuthorize("@ss.hasPermission('hrm:portal:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody HrmPortalEmployeeUpdateReqVO reqVO) {
        portalService.updateEmployee(reqVO);
        return success(true);
    }

    @GetMapping("/employee/certificate/list")
    @Operation(summary = "获得当前员工证书列表")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeCertificateVO>> getCertificateList() {
        return success(portalService.getCertificateList());
    }

    @GetMapping("/employee/contact/list")
    @Operation(summary = "获得当前员工紧急联系人列表")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeContactVO>> getContactList() {
        return success(portalService.getContactList());
    }

    @GetMapping("/employee/education-experience/list")
    @Operation(summary = "获得当前员工教育经历列表")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeEducationExperienceVO>> getEducationExperienceList() {
        return success(portalService.getEducationExperienceList());
    }

    @GetMapping("/employee/work-experience/list")
    @Operation(summary = "获得当前员工工作经历列表")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeWorkExperienceVO>> getWorkExperienceList() {
        return success(portalService.getWorkExperienceList());
    }

    @GetMapping("/employee/training-experience/list")
    @Operation(summary = "获得当前员工培训经历列表")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeTrainingExperienceVO>> getTrainingExperienceList() {
        return success(portalService.getTrainingExperienceList());
    }

    @GetMapping("/employee/quit-info/get")
    @Operation(summary = "获得当前员工离职信息")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmEmployeeQuitInfoRespVO> getQuitInfo() {
        return success(portalService.getQuitInfo());
    }

    @GetMapping("/employee/field-config/list")
    @Operation(summary = "获得当前员工档案字段配置")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeFieldConfigRespVO>> getFieldConfigList() {
        return success(portalService.getFieldConfigList());
    }

    // ========== 考勤 ==========

    @GetMapping("/attendance/clock/list")
    @Operation(summary = "获得当前员工打卡记录列表")
    @Parameter(name = "year", description = "年份")
    @Parameter(name = "month", description = "月份")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:query')")
    public CommonResult<List<HrmPortalAttendanceClockVO>> getClockList(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        LocalDate base = (year != null && month != null)
                ? LocalDate.of(year, month, 1) : LocalDate.now().withDayOfMonth(1);
        LocalDate start = base.withDayOfMonth(1);
        LocalDate end = base.withDayOfMonth(base.lengthOfMonth());
        return success(portalService.getClockList(start.toString(), end.toString()));
    }

    @GetMapping("/attendance/leave/list")
    @Operation(summary = "获得当前员工请假申请列表")
    @Parameter(name = "year", description = "年份")
    @Parameter(name = "month", description = "月份")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:leave:query')")
    public CommonResult<List<HrmPortalAttendanceLeaveVO>> getLeaveList(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        return success(portalService.getLeaveList(year, month));
    }

    @PostMapping("/attendance/leave/create")
    @Operation(summary = "创建当前员工请假申请")
    @PreAuthorize("@ss.hasPermission('hrm:portal:attendance:leave')")
    public CommonResult<Long> createLeave(@Valid @RequestBody HrmPortalAttendanceLeaveCreateReqVO reqVO) {
        return success(portalService.createLeave(reqVO));
    }

    @PutMapping("/attendance/leave/cancel")
    @Operation(summary = "取消当前员工请假申请")
    @PreAuthorize("@ss.hasPermission('hrm:portal:attendance:leave')")
    public CommonResult<Boolean> cancelLeave(@Valid @RequestBody HrmPortalAttendanceLeaveCancelReqVO reqVO) {
        portalService.cancelLeave(reqVO);
        return success(true);
    }

    @GetMapping("/attendance/statistics/month-detail")
    @Operation(summary = "获得当前员工月度考勤详情")
    @Parameter(name = "year", description = "年份")
    @Parameter(name = "month", description = "月份")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:query')")
    public CommonResult<Map<String, Object>> getAttendanceMonthDetail(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month) {
        LocalDate now = LocalDate.now();
        return success(portalService.getAttendanceMonthDetail(
                year != null ? year : now.getYear(),
                month != null ? month : now.getMonthValue()));
    }

    @GetMapping("/attendance/statistics/export-excel")
    @Operation(summary = "导出当前员工月度考勤")
    @Parameter(name = "year", description = "年份")
    @Parameter(name = "month", description = "月份")
    @PreAuthorize("@ss.hasPermission('hrm:attendance:clock:query')")
    public void exportAttendanceMonthDetail(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            HttpServletResponse response) throws IOException {
        LocalDate now = LocalDate.now();
        List<HrmPortalAttendanceMonthExportExcelVO> list = portalService.getAttendanceMonthExportList(
                year != null ? year : now.getYear(),
                month != null ? month : now.getMonthValue());
        ExcelUtils.write(response, "考勤统计.xls", "数据", HrmPortalAttendanceMonthExportExcelVO.class, list);
    }

    // ========== 社保 ==========

    @GetMapping("/insurance/record/list")
    @Operation(summary = "获得当前员工社保参保记录列表")
    @Parameter(name = "year", description = "年份")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmPortalInsuranceRecordVO>> getInsuranceRecordList(
            @RequestParam(value = "year", required = false) Integer year) {
        return success(portalService.getInsuranceRecordList(year));
    }

    @GetMapping("/insurance/record/get")
    @Operation(summary = "获得当前员工社保参保记录详情")
    @Parameter(name = "id", description = "记录编号（year*100+month）", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmPortalInsuranceRecordVO> getInsuranceRecord(@RequestParam("id") Long id) {
        return success(portalService.getInsuranceRecord((int) (id / 100), (int) (id % 100)));
    }

    // ========== 绩效 ==========

    @GetMapping("/performance/assessment/page")
    @Operation(summary = "获得当前员工绩效考核分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getAssessmentPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getAssessmentPage(reqVO));
    }

    @GetMapping("/performance/assessment/task-count")
    @Operation(summary = "获得当前员工绩效任务数量")
    @Parameter(name = "search", description = "关键字（考核名称）")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<HrmPortalPerformanceTaskCountVO> getAssessmentTaskCount(
            @RequestParam(value = "search", required = false) String search) {
        return success(portalService.getAssessmentTaskCount(search));
    }

    @GetMapping("/performance/assessment/get")
    @Operation(summary = "获得当前员工绩效考核详情")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @Parameter(name = "stageId", description = "运行阶段编号")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<HrmPortalPerformanceAssessmentVO> getAssessment(
            @RequestParam("id") Long id,
            @RequestParam(value = "stageId", required = false) Long stageId) {
        return success(portalService.getAssessment(id, stageId));
    }

    @GetMapping("/performance/assessment/process-record-list")
    @Operation(summary = "获得当前员工绩效流程记录列表")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @Parameter(name = "stageId", description = "运行阶段编号")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPortalPerformanceProcessRecordVO>> getProcessRecordList(
            @RequestParam("id") Long id,
            @RequestParam(value = "stageId", required = false) Long stageId) {
        return success(portalService.getProcessRecordList(id, stageId));
    }

    @GetMapping("/performance/assessment/fill-quota-task-page")
    @Operation(summary = "获得指标填写任务分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getFillQuotaTaskPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getFillQuotaTaskPage(reqVO));
    }

    @GetMapping("/performance/assessment/target-confirmation-task-page")
    @Operation(summary = "获得目标确认任务分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getTargetConfirmationTaskPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getTargetConfirmationTaskPage(reqVO));
    }

    @GetMapping("/performance/assessment/review-task-page")
    @Operation(summary = "获得评分任务分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getReviewTaskPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getReviewTaskPage(reqVO));
    }

    @GetMapping("/performance/assessment/result-audit-task-page")
    @Operation(summary = "获得结果审核任务分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getResultAuditTaskPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getResultAuditTaskPage(reqVO));
    }

    @GetMapping("/performance/assessment/result-confirmation-task-page")
    @Operation(summary = "获得结果确认任务分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getResultConfirmationTaskPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getResultConfirmationTaskPage(reqVO));
    }

    @GetMapping("/performance/assessment/appeal-task-page")
    @Operation(summary = "获得申诉处理任务分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPortalPerformanceAssessmentVO>> getAppealTaskPage(
            @Valid HrmPortalPerformanceAssessmentPageReqVO reqVO) {
        return success(portalService.getAppealTaskPage(reqVO));
    }

    @PutMapping("/performance/assessment/fill-quota")
    @Operation(summary = "填写绩效指标")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> fillQuota(@Valid @RequestBody HrmPortalPerformanceQuotaFillReqVO reqVO) {
        portalService.fillQuota(reqVO);
        return success(true);
    }

    @PutMapping("/performance/assessment/confirm-target")
    @Operation(summary = "确认绩效目标")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> confirmTarget(@Valid @RequestBody HrmPortalPerformanceConfirmReqVO reqVO) {
        portalService.confirmTarget(reqVO);
        return success(true);
    }

    @PostMapping("/performance/assessment/score-preview")
    @Operation(summary = "预览绩效评分结果")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Object> previewScore(@Valid @RequestBody HrmPortalPerformanceScoreReqVO reqVO) {
        return success(portalService.previewScore(reqVO));
    }

    @PutMapping("/performance/assessment/score")
    @Operation(summary = "提交绩效评分")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> score(@Valid @RequestBody HrmPortalPerformanceScoreReqVO reqVO) {
        portalService.score(reqVO);
        return success(true);
    }

    @PutMapping("/performance/assessment/reject-review-stage")
    @Operation(summary = "驳回绩效评分阶段")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> rejectReviewStage(@Valid @RequestBody HrmPortalPerformanceRejectReqVO reqVO) {
        portalService.rejectReviewStage(reqVO);
        return success(true);
    }

    @PutMapping("/performance/assessment/handle-result-audit")
    @Operation(summary = "处理绩效结果审核")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> handleResultAudit(@Valid @RequestBody HrmPortalPerformanceHandleStageReqVO reqVO) {
        portalService.handleResultAudit(reqVO);
        return success(true);
    }

    @PutMapping("/performance/assessment/confirm-result")
    @Operation(summary = "确认绩效结果")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> confirmResult(@Valid @RequestBody HrmPortalPerformanceConfirmReqVO reqVO) {
        portalService.confirmResult(reqVO);
        return success(true);
    }

    @PutMapping("/performance/assessment/submit-appeal")
    @Operation(summary = "提交绩效申诉")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> submitAppeal(@Valid @RequestBody HrmPortalPerformanceAppealReqVO reqVO) {
        portalService.submitAppeal(reqVO);
        return success(true);
    }

    @PutMapping("/performance/assessment/handle-appeal")
    @Operation(summary = "处理绩效申诉")
    @PreAuthorize("@ss.hasPermission('hrm:portal:performance:action')")
    public CommonResult<Boolean> handleAppeal(@Valid @RequestBody HrmPortalPerformanceHandleStageReqVO reqVO) {
        portalService.handleAppeal(reqVO);
        return success(true);
    }

    // ========== 工资条 ==========

    @GetMapping("/salary/slip/list")
    @Operation(summary = "获得当前员工工资条列表")
    @Parameter(name = "startMonth", description = "开始月份（yyyy-MM）")
    @Parameter(name = "endMonth", description = "结束月份（yyyy-MM）")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<List<HrmPortalSalarySlipVO>> getSalarySlipList(
            @RequestParam(value = "startMonth", required = false) String startMonth,
            @RequestParam(value = "endMonth", required = false) String endMonth) {
        return success(portalService.getSalarySlipPage(1, cn.zhicloud.framework.common.pojo.PageParam.PAGE_SIZE_NONE,
                startMonth, endMonth).getList());
    }

    @GetMapping("/salary/slip/unread-summary")
    @Operation(summary = "获得当前员工未读工资条概况")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<HrmPortalSalarySlipUnreadSummaryVO> getSalarySlipUnreadSummary() {
        return success(portalService.getSalarySlipUnreadSummary());
    }

    @PutMapping("/salary/slip/read")
    @Operation(summary = "标记当前员工工资条已读")
    @Parameter(name = "ids", description = "工资条编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:update')")
    public CommonResult<Boolean> readSalarySlip(@RequestParam("ids") String ids) {
        Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).forEach(portalService::readSalarySlip);
        return success(true);
    }

    // ========== 首页日历 ==========

    @GetMapping("/home/calendar")
    @Operation(summary = "获得当前员工首页日历")
    @Parameter(name = "startDate", description = "开始日期（yyyy-MM-dd）", required = true)
    @Parameter(name = "endDate", description = "结束日期（yyyy-MM-dd）", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmPortalCalendarItemVO>> getHomeCalendar(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        return success(portalService.getHomeCalendar(startDate, endDate));
    }

}

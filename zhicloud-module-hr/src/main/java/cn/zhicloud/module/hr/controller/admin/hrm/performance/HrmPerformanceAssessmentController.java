package cn.zhicloud.module.hr.controller.admin.hrm.performance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.performance.vo.*;
import cn.zhicloud.module.hr.service.hrm.performance.HrmPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 员工绩效考核 Controller（考核 + 归档 + 流程记录）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 员工绩效考核")
@RestController
@RequestMapping("/hrm/performance/assessment")
@Validated
public class HrmPerformanceAssessmentController {

    @Resource
    private HrmPerformanceService performanceService;

    @GetMapping("/page")
    @Operation(summary = "获得员工绩效考核分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPerformanceAssessmentVO>> getAssessmentPage(
            @Valid HrmPerformanceAssessmentPageReqVO reqVO) {
        return success(performanceService.getAssessmentPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得员工绩效考核详情")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<HrmPerformanceAssessmentVO> getAssessment(@RequestParam("id") Long id) {
        return success(performanceService.getAssessment(id));
    }

    @GetMapping("/unassigned-employee-id-list")
    @Operation(summary = "获得未分配员工编号列表")
    @Parameter(name = "planId", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<Long>> getUnassignedEmployeeIdList(@RequestParam("planId") Long planId) {
        return success(performanceService.getUnassignedEmployeeIdList(planId));
    }

    @PostMapping("/create-list")
    @Operation(summary = "批量新增员工绩效考核")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Long> createAssessmentList(@Valid @RequestBody HrmPerformanceAssessmentCreateListReqVO reqVO) {
        return success(performanceService.createAssessmentList(reqVO));
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除员工绩效考核")
    @Parameter(name = "ids", description = "考核编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> deleteAssessmentList(@RequestParam("ids") String ids) {
        performanceService.deleteAssessmentList(Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList());
        return success(true);
    }

    @GetMapping("/process-record-list")
    @Operation(summary = "获得绩效流程记录列表")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPerformanceProcessRecordVO>> getProcessRecordList(@RequestParam("id") Long id) {
        return success(performanceService.getProcessRecordList(id));
    }

    // ========== 归档 ==========

    @GetMapping("/archive-page")
    @Operation(summary = "获得绩效归档分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPerformanceAssessmentVO>> getArchivePage(
            @Valid HrmPerformanceAssessmentPageReqVO reqVO) {
        return success(performanceService.getArchivePage(reqVO));
    }

    @GetMapping("/archive-get")
    @Operation(summary = "获得绩效归档详情")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<HrmPerformanceAssessmentVO> getArchive(@RequestParam("id") Long id) {
        return success(performanceService.getArchive(id));
    }

    @GetMapping("/archive-plan-simple-list")
    @Operation(summary = "获得绩效归档计划精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPerformancePlanVO>> getArchivePlanSimpleList() {
        return success(performanceService.getArchivePlanSimpleList());
    }

    @DeleteMapping("/archive-delete")
    @Operation(summary = "删除绩效归档")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:archive:delete')")
    public CommonResult<Boolean> deleteArchive(@RequestParam("id") Long id) {
        performanceService.deleteArchive(id);
        return success(true);
    }

    @DeleteMapping("/archive-employee-delete")
    @Operation(summary = "删除绩效归档员工")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:archive:delete')")
    public CommonResult<Boolean> deleteArchiveEmployee(@RequestParam("id") Long id) {
        performanceService.deleteArchiveEmployee(id);
        return success(true);
    }

    @GetMapping("/archive-process-record-list")
    @Operation(summary = "获得绩效归档流程记录列表")
    @Parameter(name = "id", description = "员工绩效考核编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPerformanceProcessRecordVO>> getArchiveProcessRecordList(@RequestParam("id") Long id) {
        return success(performanceService.getArchiveProcessRecordList(id));
    }

}

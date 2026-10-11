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

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 绩效计划 Controller（计划 CRUD + 生命周期 + 计数）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 绩效计划")
@RestController
@RequestMapping("/hrm/performance/plan")
@Validated
public class HrmPerformancePlanController {

    @Resource
    private HrmPerformanceService performanceService;

    @GetMapping("/page")
    @Operation(summary = "获得绩效计划分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<PageResult<HrmPerformancePlanVO>> getPlanPage(@Valid HrmPerformancePlanPageReqVO reqVO) {
        return success(performanceService.getPlanPage(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得绩效计划详情")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<HrmPerformancePlanVO> getPlan(@RequestParam("id") Long id) {
        return success(performanceService.getPlan(id));
    }

    @GetMapping("/status-count")
    @Operation(summary = "获得绩效计划状态数量")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPerformancePlanStatusCountRespVO>> getPlanStatusCount() {
        return success(performanceService.getPlanStatusCount());
    }

    @GetMapping("/stage-count")
    @Operation(summary = "获得绩效计划阶段数量")
    @Parameter(name = "planId", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPerformancePlanStageCountRespVO>> getPlanStageCount(@RequestParam("planId") Long planId) {
        return success(performanceService.getPlanStageCount(planId));
    }

    @GetMapping("/level-count")
    @Operation(summary = "获得绩效计划等级数量")
    @Parameter(name = "planId", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:query')")
    public CommonResult<List<HrmPerformancePlanLevelCountRespVO>> getPlanLevelCount(@RequestParam("planId") Long planId) {
        return success(performanceService.getPlanLevelCount(planId));
    }

    @PostMapping("/create")
    @Operation(summary = "创建绩效计划")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:create')")
    public CommonResult<Long> createPlan(@Valid @RequestBody HrmPerformancePlanVO reqVO) {
        return success(performanceService.createPlan(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改绩效计划")
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> updatePlan(@Valid @RequestBody HrmPerformancePlanVO reqVO) {
        performanceService.updatePlan(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除绩效计划")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:delete')")
    public CommonResult<Boolean> deletePlan(@RequestParam("id") Long id) {
        performanceService.deletePlan(id);
        return success(true);
    }

    @PutMapping("/start")
    @Operation(summary = "开始绩效计划")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> startPlan(@RequestParam("id") Long id) {
        performanceService.startPlan(id);
        return success(true);
    }

    @PutMapping("/open-scoring")
    @Operation(summary = "开启评分")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> openScoring(@RequestParam("id") Long id) {
        performanceService.openScoring(id);
        return success(true);
    }

    @PutMapping("/start-interview")
    @Operation(summary = "发起面谈")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> startInterview(@RequestParam("id") Long id) {
        performanceService.startInterview(id);
        return success(true);
    }

    @PutMapping("/archive")
    @Operation(summary = "归档绩效计划")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> archivePlan(@RequestParam("id") Long id) {
        performanceService.archivePlan(id);
        return success(true);
    }

    @PutMapping("/terminate")
    @Operation(summary = "终止绩效计划")
    @Parameter(name = "id", description = "绩效计划编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:plan:update')")
    public CommonResult<Boolean> terminatePlan(@RequestParam("id") Long id) {
        performanceService.terminatePlan(id);
        return success(true);
    }

}

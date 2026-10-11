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
 * HRM 绩效模板 Controller（考核模板 + 结果模板）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 绩效模板")
@RestController
@RequestMapping("/hrm/performance")
@Validated
public class HrmPerformanceTemplateController {

    @Resource
    private HrmPerformanceService performanceService;

    // ========== 考核模板 ==========

    @GetMapping("/assessment-template/page")
    @Operation(summary = "获得考核模板分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:query')")
    public CommonResult<PageResult<HrmPerformanceAssessmentTemplateVO>> getAssessmentTemplatePage(
            @Valid HrmPerformanceTemplatePageReqVO reqVO) {
        return success(performanceService.getAssessmentTemplatePage(reqVO));
    }

    @GetMapping("/assessment-template/get")
    @Operation(summary = "获得考核模板详情")
    @Parameter(name = "id", description = "模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:query')")
    public CommonResult<HrmPerformanceAssessmentTemplateVO> getAssessmentTemplate(@RequestParam("id") Long id) {
        return success(performanceService.getAssessmentTemplate(id));
    }

    @GetMapping("/assessment-template/simple-list")
    @Operation(summary = "获得考核模板精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:query')")
    public CommonResult<List<HrmPerformanceAssessmentTemplateVO>> getAssessmentTemplateSimpleList() {
        return success(performanceService.getAssessmentTemplateSimpleList());
    }

    @PostMapping("/assessment-template/create")
    @Operation(summary = "创建考核模板")
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:create')")
    public CommonResult<Long> createAssessmentTemplate(@Valid @RequestBody HrmPerformanceAssessmentTemplateVO reqVO) {
        return success(performanceService.createAssessmentTemplate(reqVO));
    }

    @PutMapping("/assessment-template/update")
    @Operation(summary = "修改考核模板")
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:update')")
    public CommonResult<Boolean> updateAssessmentTemplate(@Valid @RequestBody HrmPerformanceAssessmentTemplateVO reqVO) {
        performanceService.updateAssessmentTemplate(reqVO);
        return success(true);
    }

    @DeleteMapping("/assessment-template/delete")
    @Operation(summary = "删除考核模板")
    @Parameter(name = "id", description = "模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:delete')")
    public CommonResult<Boolean> deleteAssessmentTemplate(@RequestParam("id") Long id) {
        performanceService.deleteAssessmentTemplate(id);
        return success(true);
    }

    @DeleteMapping("/assessment-template/delete-list")
    @Operation(summary = "批量删除考核模板")
    @Parameter(name = "ids", description = "模板编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:assessment-template:delete')")
    public CommonResult<Boolean> deleteAssessmentTemplateList(@RequestParam("ids") String ids) {
        performanceService.deleteAssessmentTemplateList(splitIds(ids));
        return success(true);
    }

    // ========== 结果模板 ==========

    @GetMapping("/result-template/page")
    @Operation(summary = "获得结果模板分页")
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:query')")
    public CommonResult<PageResult<HrmPerformanceResultTemplateVO>> getResultTemplatePage(
            @Valid HrmPerformanceTemplatePageReqVO reqVO) {
        return success(performanceService.getResultTemplatePage(reqVO));
    }

    @GetMapping("/result-template/get")
    @Operation(summary = "获得结果模板详情")
    @Parameter(name = "id", description = "结果模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:query')")
    public CommonResult<HrmPerformanceResultTemplateVO> getResultTemplate(@RequestParam("id") Long id) {
        return success(performanceService.getResultTemplate(id));
    }

    @GetMapping("/result-template/simple-list")
    @Operation(summary = "获得结果模板精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:query')")
    public CommonResult<List<HrmPerformanceResultTemplateVO>> getResultTemplateSimpleList() {
        return success(performanceService.getResultTemplateSimpleList());
    }

    @PostMapping("/result-template/create")
    @Operation(summary = "创建结果模板")
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:create')")
    public CommonResult<Long> createResultTemplate(@Valid @RequestBody HrmPerformanceResultTemplateVO reqVO) {
        return success(performanceService.createResultTemplate(reqVO));
    }

    @PutMapping("/result-template/update")
    @Operation(summary = "修改结果模板")
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:update')")
    public CommonResult<Boolean> updateResultTemplate(@Valid @RequestBody HrmPerformanceResultTemplateVO reqVO) {
        performanceService.updateResultTemplate(reqVO);
        return success(true);
    }

    @DeleteMapping("/result-template/delete")
    @Operation(summary = "删除结果模板")
    @Parameter(name = "id", description = "结果模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:delete')")
    public CommonResult<Boolean> deleteResultTemplate(@RequestParam("id") Long id) {
        performanceService.deleteResultTemplate(id);
        return success(true);
    }

    @DeleteMapping("/result-template/delete-list")
    @Operation(summary = "批量删除结果模板")
    @Parameter(name = "ids", description = "结果模板编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:performance:result-template:delete')")
    public CommonResult<Boolean> deleteResultTemplateList(@RequestParam("ids") String ids) {
        performanceService.deleteResultTemplateList(splitIds(ids));
        return success(true);
    }

    private List<Long> splitIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
    }

}

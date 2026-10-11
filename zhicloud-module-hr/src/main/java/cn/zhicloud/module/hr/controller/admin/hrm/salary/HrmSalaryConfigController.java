package cn.zhicloud.module.hr.controller.admin.hrm.salary;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;
import cn.zhicloud.module.hr.service.hrm.salary.HrmSalaryConfigService;
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
 * HRM 薪资配置 Controller（薪资组/薪资项/计税规则/调薪模板/薪资配置）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 薪资配置")
@RestController
@RequestMapping("/hrm/salary")
@Validated
public class HrmSalaryConfigController {

    @Resource
    private HrmSalaryConfigService salaryConfigService;

    // ========== 薪资组 ==========

    @GetMapping("/group/page")
    @Operation(summary = "获得薪资组分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:group:query')")
    public CommonResult<PageResult<HrmSalaryGroupVO>> getGroupPage(@Valid HrmSalaryGroupPageReqVO reqVO) {
        return success(salaryConfigService.getGroupPage(reqVO));
    }

    @GetMapping("/group/get")
    @Operation(summary = "获得薪资组详情")
    @Parameter(name = "id", description = "薪资组编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:group:query')")
    public CommonResult<HrmSalaryGroupVO> getGroup(@RequestParam("id") Long id) {
        return success(salaryConfigService.getGroup(id));
    }

    @GetMapping("/group/simple-list")
    @Operation(summary = "获得薪资组精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:group:query')")
    public CommonResult<List<HrmSalaryGroupVO>> getGroupSimpleList() {
        return success(salaryConfigService.getGroupSimpleList());
    }

    @PostMapping("/group/create")
    @Operation(summary = "创建薪资组")
    @PreAuthorize("@ss.hasPermission('hrm:salary:group:create')")
    public CommonResult<Long> createGroup(@Valid @RequestBody HrmSalaryGroupVO reqVO) {
        return success(salaryConfigService.createGroup(reqVO));
    }

    @PutMapping("/group/update")
    @Operation(summary = "修改薪资组")
    @PreAuthorize("@ss.hasPermission('hrm:salary:group:update')")
    public CommonResult<Boolean> updateGroup(@Valid @RequestBody HrmSalaryGroupVO reqVO) {
        salaryConfigService.updateGroup(reqVO);
        return success(true);
    }

    @DeleteMapping("/group/delete")
    @Operation(summary = "删除薪资组")
    @Parameter(name = "id", description = "薪资组编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:group:delete')")
    public CommonResult<Boolean> deleteGroup(@RequestParam("id") Long id) {
        salaryConfigService.deleteGroup(id);
        return success(true);
    }

    // ========== 薪资项 ==========

    @GetMapping("/option/list")
    @Operation(summary = "获得薪资项列表（树形）")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:query')")
    public CommonResult<List<HrmSalaryOptionVO>> getOptionList() {
        return success(salaryConfigService.getOptionList());
    }

    @GetMapping("/option/simple-list")
    @Operation(summary = "获得薪资项精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:query')")
    public CommonResult<List<HrmSalaryOptionVO>> getOptionSimpleList() {
        return success(salaryConfigService.getOptionSimpleList());
    }

    @PostMapping("/option/create")
    @Operation(summary = "创建薪资项")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:create')")
    public CommonResult<Long> createOption(@Valid @RequestBody HrmSalaryOptionVO reqVO) {
        return success(salaryConfigService.createOption(reqVO));
    }

    @PutMapping("/option/update")
    @Operation(summary = "修改薪资项")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:update')")
    public CommonResult<Boolean> updateOption(@Valid @RequestBody HrmSalaryOptionVO reqVO) {
        salaryConfigService.updateOption(reqVO);
        return success(true);
    }

    @DeleteMapping("/option/delete")
    @Operation(summary = "删除薪资项")
    @Parameter(name = "id", description = "薪资项编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:delete')")
    public CommonResult<Boolean> deleteOption(@RequestParam("id") Long id) {
        salaryConfigService.deleteOption(id);
        return success(true);
    }

    @PutMapping("/option/update-enabled")
    @Operation(summary = "更新薪资项启用状态")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:update')")
    public CommonResult<Boolean> updateOptionEnabled(@Valid @RequestBody HrmSalaryOptionEnabledReqVO reqVO) {
        salaryConfigService.updateOptionEnabled(reqVO);
        return success(true);
    }

    @PutMapping("/option/update-visible")
    @Operation(summary = "更新薪资项显示状态")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:update')")
    public CommonResult<Boolean> updateOptionVisible(@Valid @RequestBody HrmSalaryOptionVisibleReqVO reqVO) {
        salaryConfigService.updateOptionVisible(reqVO);
        return success(true);
    }

    @PutMapping("/option/sync")
    @Operation(summary = "同步标准薪资项")
    @PreAuthorize("@ss.hasPermission('hrm:salary:option:update')")
    public CommonResult<Boolean> syncOption() {
        salaryConfigService.syncOption();
        return success(true);
    }

    // ========== 计税规则 ==========

    @GetMapping("/tax-rule/list")
    @Operation(summary = "获得计税规则列表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:tax-rule:query')")
    public CommonResult<List<HrmSalaryTaxRuleVO>> getTaxRuleList() {
        return success(salaryConfigService.getTaxRuleList());
    }

    @GetMapping("/tax-rule/get")
    @Operation(summary = "获得计税规则详情")
    @Parameter(name = "id", description = "计税规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:tax-rule:query')")
    public CommonResult<HrmSalaryTaxRuleVO> getTaxRule(@RequestParam("id") Long id) {
        return success(salaryConfigService.getTaxRule(id));
    }

    @PostMapping("/tax-rule/create")
    @Operation(summary = "创建计税规则")
    @PreAuthorize("@ss.hasPermission('hrm:salary:tax-rule:create')")
    public CommonResult<Long> createTaxRule(@Valid @RequestBody HrmSalaryTaxRuleVO reqVO) {
        return success(salaryConfigService.createTaxRule(reqVO));
    }

    @PutMapping("/tax-rule/update")
    @Operation(summary = "修改计税规则")
    @PreAuthorize("@ss.hasPermission('hrm:salary:tax-rule:update')")
    public CommonResult<Boolean> updateTaxRule(@Valid @RequestBody HrmSalaryTaxRuleVO reqVO) {
        salaryConfigService.updateTaxRule(reqVO);
        return success(true);
    }

    @DeleteMapping("/tax-rule/delete")
    @Operation(summary = "删除计税规则")
    @Parameter(name = "id", description = "计税规则编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:tax-rule:delete')")
    public CommonResult<Boolean> deleteTaxRule(@RequestParam("id") Long id) {
        salaryConfigService.deleteTaxRule(id);
        return success(true);
    }

    // ========== 调薪模板 ==========

    @GetMapping("/change-template/list")
    @Operation(summary = "获得调薪模板列表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:change-template:query')")
    public CommonResult<List<HrmSalaryChangeTemplateVO>> getChangeTemplateList() {
        return success(salaryConfigService.getChangeTemplateList());
    }

    @GetMapping("/change-template/get")
    @Operation(summary = "获得调薪模板详情")
    @Parameter(name = "id", description = "调薪模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:change-template:query')")
    public CommonResult<HrmSalaryChangeTemplateVO> getChangeTemplate(@RequestParam("id") Long id) {
        return success(salaryConfigService.getChangeTemplate(id));
    }

    @PostMapping("/change-template/create")
    @Operation(summary = "创建调薪模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:change-template:create')")
    public CommonResult<Long> createChangeTemplate(@Valid @RequestBody HrmSalaryChangeTemplateVO reqVO) {
        return success(salaryConfigService.createChangeTemplate(reqVO));
    }

    @PutMapping("/change-template/update")
    @Operation(summary = "修改调薪模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:change-template:update')")
    public CommonResult<Boolean> updateChangeTemplate(@Valid @RequestBody HrmSalaryChangeTemplateVO reqVO) {
        salaryConfigService.updateChangeTemplate(reqVO);
        return success(true);
    }

    @DeleteMapping("/change-template/delete")
    @Operation(summary = "删除调薪模板")
    @Parameter(name = "id", description = "调薪模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:change-template:delete')")
    public CommonResult<Boolean> deleteChangeTemplate(@RequestParam("id") Long id) {
        salaryConfigService.deleteChangeTemplate(id);
        return success(true);
    }

    // ========== 薪资配置 ==========

    @GetMapping("/config/get")
    @Operation(summary = "获得薪资配置")
    @PreAuthorize("@ss.hasPermission('hrm:salary:config:query')")
    public CommonResult<HrmSalaryConfigVO> getConfig() {
        return success(salaryConfigService.getConfig());
    }

    @PostMapping("/config/create")
    @Operation(summary = "创建薪资配置")
    @PreAuthorize("@ss.hasPermission('hrm:salary:config:update')")
    public CommonResult<Long> createConfig(@Valid @RequestBody HrmSalaryConfigVO reqVO) {
        return success(salaryConfigService.createConfig(reqVO));
    }

    @PutMapping("/config/update")
    @Operation(summary = "修改薪资配置")
    @PreAuthorize("@ss.hasPermission('hrm:salary:config:update')")
    public CommonResult<Boolean> updateConfig(@Valid @RequestBody HrmSalaryConfigVO reqVO) {
        salaryConfigService.updateConfig(reqVO);
        return success(true);
    }

}

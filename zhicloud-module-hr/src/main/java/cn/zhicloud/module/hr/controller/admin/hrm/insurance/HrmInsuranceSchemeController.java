package cn.zhicloud.module.hr.controller.admin.hrm.insurance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.InsuranceSchemeVO;
import cn.zhicloud.module.hr.service.hrm.insurance.HrmInsuranceSchemeService;
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
 * HRM 社保方案 Controller（前端 api/hrm/insurance/scheme 契约）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 社保方案")
@RestController
@RequestMapping("/hrm/insurance/scheme")
@Validated
public class HrmInsuranceSchemeController {

    @Resource
    private HrmInsuranceSchemeService schemeService;

    @GetMapping("/list")
    @Operation(summary = "获得社保方案列表")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:scheme:query')")
    public CommonResult<List<InsuranceSchemeVO>> getSchemeList() {
        return success(schemeService.getSchemeList());
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得社保方案精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:scheme:query')")
    public CommonResult<List<InsuranceSchemeVO>> getSchemeSimpleList() {
        return success(schemeService.getSchemeSimpleList());
    }

    @GetMapping("/get")
    @Operation(summary = "获得社保方案详情")
    @Parameter(name = "id", description = "社保方案编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:scheme:query')")
    public CommonResult<InsuranceSchemeVO> getScheme(@RequestParam("id") Long id) {
        return success(schemeService.getScheme(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建社保方案")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:scheme:create')")
    public CommonResult<Long> createScheme(@Valid @RequestBody InsuranceSchemeVO reqVO) {
        return success(schemeService.createScheme(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改社保方案")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:scheme:update')")
    public CommonResult<Boolean> updateScheme(@Valid @RequestBody InsuranceSchemeVO reqVO) {
        schemeService.updateScheme(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除社保方案")
    @Parameter(name = "id", description = "社保方案编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:scheme:delete')")
    public CommonResult<Boolean> deleteScheme(@RequestParam("id") Long id) {
        schemeService.deleteScheme(id);
        return success(true);
    }

}

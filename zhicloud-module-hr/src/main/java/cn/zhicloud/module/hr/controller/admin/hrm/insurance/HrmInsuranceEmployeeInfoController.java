package cn.zhicloud.module.hr.controller.admin.hrm.insurance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeInfoVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.HrmInsuranceEmployeeUpdateSchemeReqVO;
import cn.zhicloud.module.hr.service.hrm.insurance.HrmInsuranceEmployeeInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 员工参保信息 Controller（前端 api/hrm/insurance/employee-info 契约）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 员工参保信息")
@RestController
@RequestMapping("/hrm/insurance/employee-info")
@Validated
public class HrmInsuranceEmployeeInfoController {

    @Resource
    private HrmInsuranceEmployeeInfoService employeeInfoService;

    @GetMapping("/get")
    @Operation(summary = "获得员工参保信息")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmInsuranceEmployeeInfoVO> getEmployeeInfo(@RequestParam("employeeId") Long employeeId) {
        return success(employeeInfoService.getEmployeeInfo(employeeId));
    }

    @PutMapping("/save")
    @Operation(summary = "保存员工参保信息")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:employee-info:update')")
    public CommonResult<Long> saveEmployeeInfo(@Valid @RequestBody HrmInsuranceEmployeeInfoVO reqVO) {
        return success(employeeInfoService.saveEmployeeInfo(reqVO));
    }

    @PutMapping("/update-scheme")
    @Operation(summary = "更新员工参保方案")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:employee-info:update')")
    public CommonResult<Boolean> updateEmployeeScheme(@Valid @RequestBody HrmInsuranceEmployeeUpdateSchemeReqVO reqVO) {
        employeeInfoService.updateEmployeeScheme(reqVO);
        return success(true);
    }

}

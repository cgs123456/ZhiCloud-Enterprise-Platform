package cn.zhicloud.module.hr.controller.admin.hrm.insurance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.InsuranceStandardTypeVO;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.InsuranceStandardProjectVO;
import cn.zhicloud.module.hr.service.hrm.insurance.HrmInsuranceStandardService;
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
 * HRM 保险域标准参保数据 Controller（前端 views/hrm/insurance/scheme 页面）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 保险域标准参保")
@RestController
@RequestMapping("/hrm/insurance/standard")
@Validated
public class HrmInsuranceStandardController {

    @Resource
    private HrmInsuranceStandardService insuranceStandardService;

    @GetMapping("/type-list")
    @Operation(summary = "查询标准参保类型列表")
    @Parameter(name = "areaId", description = "参保地区编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:standard:type-list')")
    public CommonResult<List<InsuranceStandardTypeVO>> getStandardTypeList(@RequestParam("areaId") Long areaId) {
        return success(insuranceStandardService.getStandardTypeList(areaId));
    }

    @GetMapping("/project-list")
    @Operation(summary = "查询标准参保项目列表")
    @Parameter(name = "areaId", description = "参保地区编号", required = true)
    @Parameter(name = "typeCode", description = "参保方案编码", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:standard:project-list')")
    public CommonResult<List<InsuranceStandardProjectVO>> getStandardProjectList(@RequestParam("areaId") Long areaId,
                                                                              @RequestParam("typeCode") String typeCode) {
        return success(insuranceStandardService.getStandardProjectList(areaId, typeCode));
    }

}

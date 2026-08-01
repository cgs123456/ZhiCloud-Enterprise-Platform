package cn.iocoder.yudao.module.hr.controller.admin.socialinsurance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsurancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsuranceRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.HrSocialInsuranceSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.socialinsurance.vo.SocialInsuranceDetailRespVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.socialinsurance.HrSocialInsuranceBaseDO;
import cn.iocoder.yudao.module.hr.service.socialinsurance.HrSocialInsuranceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 社保申报")
@RestController
@RequestMapping("/hr/social-insurance")
@Validated
public class HrSocialInsuranceController {

    @Resource
    private HrSocialInsuranceService socialInsuranceService;

    @PostMapping("/create")
    @Operation(summary = "创建社保基数")
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:create')")
    public CommonResult<Long> createSocialInsurance(@Valid @RequestBody HrSocialInsuranceSaveReqVO createReqVO) {
        return success(socialInsuranceService.createSocialInsurance(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新社保基数")
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:update')")
    public CommonResult<Boolean> updateSocialInsurance(@Valid @RequestBody HrSocialInsuranceSaveReqVO updateReqVO) {
        socialInsuranceService.updateSocialInsurance(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除社保基数")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:delete')")
    public CommonResult<Boolean> deleteSocialInsurance(@RequestParam("id") Long id) {
        socialInsuranceService.deleteSocialInsurance(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得社保基数")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:query')")
    public CommonResult<HrSocialInsuranceRespVO> getSocialInsurance(@RequestParam("id") Long id) {
        HrSocialInsuranceBaseDO socialInsurance = socialInsuranceService.getSocialInsurance(id);
        return success(BeanUtils.toBean(socialInsurance, HrSocialInsuranceRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得社保分页")
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:query')")
    public CommonResult<PageResult<HrSocialInsuranceRespVO>> getSocialInsurancePage(@Valid HrSocialInsurancePageReqVO pageReqVO) {
        PageResult<HrSocialInsuranceBaseDO> pageResult = socialInsuranceService.getSocialInsurancePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrSocialInsuranceRespVO.class));
    }

    @PutMapping("/adjust")
    @Operation(summary = "基数调整")
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:update')")
    public CommonResult<Long> adjustBase(@Valid @RequestBody HrSocialInsuranceSaveReqVO reqVO) {
        return success(socialInsuranceService.adjustBase(reqVO));
    }

    @GetMapping("/calculate-monthly")
    @Operation(summary = "计算月度社保")
    @Parameter(name = "employeeId", description = "员工 ID", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('hr:social-insurance:query')")
    public CommonResult<SocialInsuranceDetailRespVO> calculateMonthly(@RequestParam("employeeId") Long employeeId) {
        return success(socialInsuranceService.calculateMonthly(employeeId));
    }

}
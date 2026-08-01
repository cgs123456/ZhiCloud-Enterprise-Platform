package cn.iocoder.yudao.module.hr.controller.admin.recruitment;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeSaveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo.HrResumeScreenReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.recruitment.HrResumeDO;
import cn.iocoder.yudao.module.hr.service.recruitment.HrResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 简历")
@RestController
@RequestMapping("/hr/resume")
@Validated
public class HrResumeController {

    @Resource
    private HrResumeService resumeService;

    @PostMapping("/create")
    @Operation(summary = "创建简历")
    @PreAuthorize("@ss.hasPermission('hr:resume:create')")
    public CommonResult<Long> createResume(@Valid @RequestBody HrResumeSaveReqVO createReqVO) {
        return success(resumeService.createResume(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新简历")
    @PreAuthorize("@ss.hasPermission('hr:resume:update')")
    public CommonResult<Boolean> updateResume(@Valid @RequestBody HrResumeSaveReqVO updateReqVO) {
        resumeService.updateResume(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除简历")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:resume:delete')")
    public CommonResult<Boolean> deleteResume(@RequestParam("id") Long id) {
        resumeService.deleteResume(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得简历")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:resume:query')")
    public CommonResult<HrResumeRespVO> getResume(@RequestParam("id") Long id) {
        HrResumeDO resume = resumeService.getResume(id);
        return success(BeanUtils.toBean(resume, HrResumeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得简历分页")
    @PreAuthorize("@ss.hasPermission('hr:resume:query')")
    public CommonResult<PageResult<HrResumeRespVO>> getResumePage(@Valid HrResumePageReqVO pageReqVO) {
        PageResult<HrResumeDO> pageResult = resumeService.getResumePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrResumeRespVO.class));
    }

    @PutMapping("/screen")
    @Operation(summary = "简历筛选")
    @PreAuthorize("@ss.hasPermission('hr:resume:update')")
    public CommonResult<Boolean> screenResume(@Valid @RequestBody HrResumeScreenReqVO reqVO) {
        resumeService.screenResume(reqVO);
        return success(true);
    }

    @PutMapping("/offer")
    @Operation(summary = "录用并创建员工档案")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:resume:update')")
    public CommonResult<Long> offerResume(@RequestParam("id") Long id) {
        return success(resumeService.offerResume(id));
    }

}
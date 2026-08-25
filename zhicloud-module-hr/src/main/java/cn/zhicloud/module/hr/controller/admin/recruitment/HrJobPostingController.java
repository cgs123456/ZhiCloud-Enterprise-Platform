package cn.zhicloud.module.hr.controller.admin.recruitment;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrJobPostingPageReqVO;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrJobPostingRespVO;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrJobPostingSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruitment.HrJobPostingDO;
import cn.zhicloud.module.hr.service.recruitment.HrJobPostingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 招聘职位")
@RestController
@RequestMapping("/hr/job-posting")
@Validated
public class HrJobPostingController {

    @Resource
    private HrJobPostingService jobPostingService;

    @PostMapping("/create")
    @Operation(summary = "创建招聘职位")
    @PreAuthorize("@ss.hasPermission('hr:job-posting:create')")
    public CommonResult<Long> createJobPosting(@Valid @RequestBody HrJobPostingSaveReqVO createReqVO) {
        return success(jobPostingService.createJobPosting(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新招聘职位")
    @PreAuthorize("@ss.hasPermission('hr:job-posting:update')")
    public CommonResult<Boolean> updateJobPosting(@Valid @RequestBody HrJobPostingSaveReqVO updateReqVO) {
        jobPostingService.updateJobPosting(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除招聘职位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:job-posting:delete')")
    public CommonResult<Boolean> deleteJobPosting(@RequestParam("id") Long id) {
        jobPostingService.deleteJobPosting(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得招聘职位")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:job-posting:query')")
    public CommonResult<HrJobPostingRespVO> getJobPosting(@RequestParam("id") Long id) {
        HrJobPostingDO jobPosting = jobPostingService.getJobPosting(id);
        return success(BeanUtils.toBean(jobPosting, HrJobPostingRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得招聘职位分页")
    @PreAuthorize("@ss.hasPermission('hr:job-posting:query')")
    public CommonResult<PageResult<HrJobPostingRespVO>> getJobPostingPage(@Valid HrJobPostingPageReqVO pageReqVO) {
        PageResult<HrJobPostingDO> pageResult = jobPostingService.getJobPostingPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrJobPostingRespVO.class));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布招聘职位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:job-posting:update')")
    public CommonResult<Boolean> publishJobPosting(@RequestParam("id") Long id) {
        jobPostingService.publishJobPosting(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭招聘职位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:job-posting:update')")
    public CommonResult<Boolean> closeJobPosting(@RequestParam("id") Long id) {
        jobPostingService.closeJobPosting(id);
        return success(true);
    }

}
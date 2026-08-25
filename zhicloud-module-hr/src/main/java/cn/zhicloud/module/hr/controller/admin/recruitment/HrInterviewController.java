package cn.zhicloud.module.hr.controller.admin.recruitment;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrInterviewPageReqVO;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrInterviewRespVO;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrInterviewResultReqVO;
import cn.zhicloud.module.hr.controller.admin.recruitment.vo.HrInterviewSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.recruitment.HrInterviewDO;
import cn.zhicloud.module.hr.service.recruitment.HrInterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 面试")
@RestController
@RequestMapping("/hr/interview")
@Validated
public class HrInterviewController {

    @Resource
    private HrInterviewService interviewService;

    @PostMapping("/create")
    @Operation(summary = "创建面试")
    @PreAuthorize("@ss.hasPermission('hr:interview:create')")
    public CommonResult<Long> createInterview(@Valid @RequestBody HrInterviewSaveReqVO createReqVO) {
        return success(interviewService.createInterview(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新面试")
    @PreAuthorize("@ss.hasPermission('hr:interview:update')")
    public CommonResult<Boolean> updateInterview(@Valid @RequestBody HrInterviewSaveReqVO updateReqVO) {
        interviewService.updateInterview(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除面试")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:interview:delete')")
    public CommonResult<Boolean> deleteInterview(@RequestParam("id") Long id) {
        interviewService.deleteInterview(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得面试")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:interview:query')")
    public CommonResult<HrInterviewRespVO> getInterview(@RequestParam("id") Long id) {
        HrInterviewDO interview = interviewService.getInterview(id);
        return success(BeanUtils.toBean(interview, HrInterviewRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得面试分页")
    @PreAuthorize("@ss.hasPermission('hr:interview:query')")
    public CommonResult<PageResult<HrInterviewRespVO>> getInterviewPage(@Valid HrInterviewPageReqVO pageReqVO) {
        PageResult<HrInterviewDO> pageResult = interviewService.getInterviewPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrInterviewRespVO.class));
    }

    @PostMapping("/schedule")
    @Operation(summary = "安排面试")
    @PreAuthorize("@ss.hasPermission('hr:interview:create')")
    public CommonResult<Long> scheduleInterview(@Valid @RequestBody HrInterviewSaveReqVO reqVO) {
        return success(interviewService.scheduleInterview(reqVO));
    }

    @PutMapping("/result")
    @Operation(summary = "记录面试结果")
    @PreAuthorize("@ss.hasPermission('hr:interview:update')")
    public CommonResult<Boolean> recordResult(@Valid @RequestBody HrInterviewResultReqVO reqVO) {
        interviewService.recordResult(reqVO);
        return success(true);
    }

}
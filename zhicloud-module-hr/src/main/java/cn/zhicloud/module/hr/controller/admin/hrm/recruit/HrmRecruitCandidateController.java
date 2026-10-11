package cn.zhicloud.module.hr.controller.admin.hrm.recruit;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo.*;
import cn.zhicloud.module.hr.service.hrm.recruit.HrmRecruitService;
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
 * HRM 招聘候选人 Controller（候选人 + 面试 + 淘汰原因配置）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 招聘候选人")
@RestController
@RequestMapping("/hrm/recruit")
@Validated
public class HrmRecruitCandidateController {

    @Resource
    private HrmRecruitService recruitService;

    // ========== 候选人 ==========

    @GetMapping("/candidate/page")
    @Operation(summary = "获得候选人分页")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:query')")
    public CommonResult<PageResult<HrmRecruitCandidateRespVO>> getCandidatePage(@Valid HrmRecruitCandidatePageReqVO reqVO) {
        return success(recruitService.getCandidatePage(reqVO));
    }

    @GetMapping("/candidate/get")
    @Operation(summary = "获得候选人详情")
    @Parameter(name = "id", description = "候选人编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:query')")
    public CommonResult<HrmRecruitCandidateRespVO> getCandidate(@RequestParam("id") Long id) {
        return success(recruitService.getCandidate(id));
    }

    @GetMapping("/candidate/status-count")
    @Operation(summary = "获得候选人状态数量")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:query')")
    public CommonResult<List<HrmRecruitCandidateStatusCountRespVO>> getCandidateStatusCount() {
        return success(recruitService.getCandidateStatusCount());
    }

    @PostMapping("/candidate/create")
    @Operation(summary = "创建候选人")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:create')")
    public CommonResult<Long> createCandidate(@Valid @RequestBody HrmRecruitCandidateSaveReqVO reqVO) {
        return success(recruitService.createCandidate(reqVO));
    }

    @PutMapping("/candidate/update")
    @Operation(summary = "修改候选人")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:update')")
    public CommonResult<Boolean> updateCandidate(@Valid @RequestBody HrmRecruitCandidateSaveReqVO reqVO) {
        recruitService.updateCandidate(reqVO);
        return success(true);
    }

    @DeleteMapping("/candidate/delete")
    @Operation(summary = "删除候选人")
    @Parameter(name = "id", description = "候选人编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:delete')")
    public CommonResult<Boolean> deleteCandidate(@RequestParam("id") Long id) {
        recruitService.deleteCandidate(id);
        return success(true);
    }

    @PutMapping("/candidate/update-status")
    @Operation(summary = "修改候选人状态")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:update')")
    public CommonResult<Boolean> updateCandidateStatus(@Valid @RequestBody HrmRecruitCandidateUpdateStatusReqVO reqVO) {
        recruitService.updateCandidateStatus(reqVO);
        return success(true);
    }

    @PutMapping("/candidate/eliminate")
    @Operation(summary = "淘汰候选人")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:update')")
    public CommonResult<Boolean> eliminateCandidate(@Valid @RequestBody HrmRecruitCandidateEliminateReqVO reqVO) {
        recruitService.eliminateCandidate(reqVO);
        return success(true);
    }

    @PutMapping("/candidate/update-channel")
    @Operation(summary = "修改候选人渠道")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:update')")
    public CommonResult<Boolean> updateCandidateChannel(@Valid @RequestBody HrmRecruitCandidateUpdateChannelReqVO reqVO) {
        recruitService.updateCandidateChannel(reqVO);
        return success(true);
    }

    @PutMapping("/candidate/update-post")
    @Operation(summary = "修改候选人职位")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:update')")
    public CommonResult<Boolean> updateCandidatePost(@Valid @RequestBody HrmRecruitCandidateUpdatePostReqVO reqVO) {
        recruitService.updateCandidatePost(reqVO);
        return success(true);
    }

    @PostMapping("/candidate/convert-employee")
    @Operation(summary = "候选人转员工")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Long> convertCandidateToEmployee(@Valid @RequestBody HrmRecruitCandidateConvertEmployeeReqVO reqVO) {
        return success(recruitService.convertCandidateToEmployee(reqVO));
    }

    @DeleteMapping("/candidate/clean-ids")
    @Operation(summary = "清理候选人编号")
    @Parameter(name = "ids", description = "候选人编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:delete')")
    public CommonResult<Boolean> cleanCandidateIds(@RequestParam("ids") String ids) {
        recruitService.cleanCandidateIds(Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList());
        return success(true);
    }

    // ========== 面试 ==========

    @GetMapping("/interview/list-by-candidate")
    @Operation(summary = "获得候选人的面试列表")
    @Parameter(name = "candidateId", description = "候选人编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:query')")
    public CommonResult<List<HrmRecruitInterviewRespVO>> getInterviewListByCandidate(@RequestParam("candidateId") Long candidateId) {
        return success(recruitService.getInterviewListByCandidate(candidateId));
    }

    @GetMapping("/interview/get")
    @Operation(summary = "获得面试详情")
    @Parameter(name = "id", description = "面试编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:query')")
    public CommonResult<HrmRecruitInterviewRespVO> getInterview(@RequestParam("id") Long id) {
        return success(recruitService.getInterview(id));
    }

    @PostMapping("/interview/create")
    @Operation(summary = "创建面试")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:interview:create')")
    public CommonResult<Long> createInterview(@Valid @RequestBody HrmRecruitInterviewSaveReqVO reqVO) {
        return success(recruitService.createInterview(reqVO));
    }

    @PutMapping("/interview/update")
    @Operation(summary = "修改面试")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:interview:update')")
    public CommonResult<Boolean> updateInterview(@Valid @RequestBody HrmRecruitInterviewSaveReqVO reqVO) {
        recruitService.updateInterview(reqVO);
        return success(true);
    }

    @PutMapping("/interview/update-result")
    @Operation(summary = "修改面试结果")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:interview:update')")
    public CommonResult<Boolean> updateInterviewResult(@Valid @RequestBody HrmRecruitInterviewResultReqVO reqVO) {
        recruitService.updateInterviewResult(reqVO);
        return success(true);
    }

    @DeleteMapping("/interview/delete")
    @Operation(summary = "删除面试")
    @Parameter(name = "id", description = "面试编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:interview:update')")
    public CommonResult<Boolean> deleteInterview(@RequestParam("id") Long id) {
        recruitService.deleteInterview(id);
        return success(true);
    }

    // ========== 配置（淘汰原因） ==========

    @GetMapping("/config/eliminate-reason/list")
    @Operation(summary = "获得淘汰原因列表")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:candidate:query')")
    public CommonResult<List<String>> getEliminateReasonList() {
        return success(recruitService.getEliminateReasonList());
    }

    @PostMapping("/config/eliminate-reason/save")
    @Operation(summary = "保存淘汰原因")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:config:update')")
    public CommonResult<Boolean> saveEliminateReason(@Valid @RequestBody HrmRecruitEliminateReasonSaveReqVO reqVO) {
        recruitService.saveEliminateReason(reqVO);
        return success(true);
    }

}

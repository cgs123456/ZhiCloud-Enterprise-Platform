package cn.zhicloud.module.hr.controller.admin.hrm.recruit;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageParam;
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

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 招聘职位 Controller（职位类型 + 招聘职位）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 招聘职位")
@RestController
@RequestMapping("/hrm/recruit")
@Validated
public class HrmRecruitPostController {

    @Resource
    private HrmRecruitService recruitService;

    // ========== 职位类型 ==========

    @GetMapping("/post-type/list")
    @Operation(summary = "查询招聘职位类型列表")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:query')")
    public CommonResult<List<HrmRecruitPostTypeVO>> getPostTypeList(
            @RequestParam(value = "status", required = false) Integer status) {
        return success(recruitService.getPostTypeList(status));
    }

    // ========== 招聘职位 ==========

    @GetMapping("/post/page")
    @Operation(summary = "获得招聘职位分页")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:query')")
    public CommonResult<PageResult<HrmRecruitPostRespVO>> getPostPage(@Valid HrmRecruitPostPageReqVO reqVO) {
        return success(recruitService.getPostPage(reqVO));
    }

    @GetMapping("/post/get")
    @Operation(summary = "获得招聘职位详情")
    @Parameter(name = "id", description = "招聘职位编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:query')")
    public CommonResult<HrmRecruitPostRespVO> getPost(@RequestParam("id") Long id) {
        return success(recruitService.getPost(id));
    }

    @GetMapping("/post/simple-list")
    @Operation(summary = "获得招聘职位精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:query')")
    public CommonResult<List<HrmRecruitPostRespVO>> getPostSimpleList(
            @RequestParam(value = "status", required = false) Integer status) {
        return success(recruitService.getPostSimpleList(status));
    }

    @GetMapping("/post/status-count")
    @Operation(summary = "获得招聘职位状态数量")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:query')")
    public CommonResult<List<HrmRecruitPostStatusCountRespVO>> getPostStatusCount() {
        return success(recruitService.getPostStatusCount());
    }

    @PostMapping("/post/create")
    @Operation(summary = "创建招聘职位")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:create')")
    public CommonResult<Long> createPost(@Valid @RequestBody HrmRecruitPostSaveReqVO reqVO) {
        return success(recruitService.createPost(reqVO));
    }

    @PutMapping("/post/update")
    @Operation(summary = "修改招聘职位")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:update')")
    public CommonResult<Boolean> updatePost(@Valid @RequestBody HrmRecruitPostSaveReqVO reqVO) {
        recruitService.updatePost(reqVO);
        return success(true);
    }

    @PutMapping("/post/update-status")
    @Operation(summary = "修改招聘职位状态")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:post:update')")
    public CommonResult<Boolean> updatePostStatus(@RequestParam("id") Long id,
                                                  @RequestParam("status") Integer status,
                                                  @RequestParam(value = "stopReason", required = false) String stopReason) {
        recruitService.updatePostStatus(id, status, stopReason);
        return success(true);
    }

}

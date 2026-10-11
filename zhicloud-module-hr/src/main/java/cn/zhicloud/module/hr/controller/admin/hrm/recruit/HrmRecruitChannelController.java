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

import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 招聘渠道 Controller
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 招聘渠道")
@RestController
@RequestMapping("/hrm/recruit/channel")
@Validated
public class HrmRecruitChannelController {

    @Resource
    private HrmRecruitService recruitService;

    @GetMapping("/page")
    @Operation(summary = "获得招聘渠道分页")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:query')")
    public CommonResult<PageResult<HrmRecruitChannelVO>> getChannelPage(
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "name", required = false) String name) {
        return success(recruitService.getChannelPage(pageNo, pageSize, name));
    }

    @GetMapping("/get")
    @Operation(summary = "获得招聘渠道详情")
    @Parameter(name = "id", description = "招聘渠道编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:query')")
    public CommonResult<HrmRecruitChannelVO> getChannel(@RequestParam("id") Long id) {
        return success(recruitService.getChannel(id));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得招聘渠道精简列表")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:query')")
    public CommonResult<List<HrmRecruitChannelVO>> getChannelSimpleList() {
        return success(recruitService.getChannelSimpleList());
    }

    @PostMapping("/create")
    @Operation(summary = "创建招聘渠道")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:create')")
    public CommonResult<Long> createChannel(@Valid @RequestBody HrmRecruitChannelSaveReqVO reqVO) {
        return success(recruitService.createChannel(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改招聘渠道")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:update')")
    public CommonResult<Boolean> updateChannel(@Valid @RequestBody HrmRecruitChannelSaveReqVO reqVO) {
        recruitService.updateChannel(reqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改招聘渠道状态")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:update')")
    public CommonResult<Boolean> updateChannelStatus(@Valid @RequestBody HrmRecruitChannelUpdateStatusReqVO reqVO) {
        recruitService.updateChannelStatus(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除招聘渠道")
    @PreAuthorize("@ss.hasPermission('hrm:recruit:channel:delete')")
    public CommonResult<Boolean> deleteChannel(@Valid @RequestBody HrmRecruitChannelDeleteReqVO reqVO) {
        recruitService.deleteChannel(reqVO);
        return success(true);
    }

}

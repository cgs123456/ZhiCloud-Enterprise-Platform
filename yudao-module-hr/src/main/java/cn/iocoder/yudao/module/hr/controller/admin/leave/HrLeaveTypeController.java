package cn.iocoder.yudao.module.hr.controller.admin.leave;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveTypeRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.leave.vo.HrLeaveTypeSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.leave.HrLeaveTypeDO;
import cn.iocoder.yudao.module.hr.service.leave.HrLeaveTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 假期类型")
@RestController
@RequestMapping("/hr/leave-type")
@Validated
public class HrLeaveTypeController {

    @Resource
    private HrLeaveTypeService leaveTypeService;

    @PostMapping("/create")
    @Operation(summary = "创建假期类型")
    @PreAuthorize("@ss.hasPermission('hr:leave-type:create')")
    public CommonResult<Long> createLeaveType(@Valid @RequestBody HrLeaveTypeSaveReqVO createReqVO) {
        return success(leaveTypeService.createLeaveType(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新假期类型")
    @PreAuthorize("@ss.hasPermission('hr:leave-type:update')")
    public CommonResult<Boolean> updateLeaveType(@Valid @RequestBody HrLeaveTypeSaveReqVO updateReqVO) {
        leaveTypeService.updateLeaveType(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除假期类型")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:leave-type:delete')")
    public CommonResult<Boolean> deleteLeaveType(@RequestParam("id") Long id) {
        leaveTypeService.deleteLeaveType(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得假期类型")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:leave-type:query')")
    public CommonResult<HrLeaveTypeRespVO> getLeaveType(@RequestParam("id") Long id) {
        HrLeaveTypeDO leaveType = leaveTypeService.getLeaveType(id);
        return success(BeanUtils.toBean(leaveType, HrLeaveTypeRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得假期类型列表")
    @PreAuthorize("@ss.hasPermission('hr:leave-type:query')")
    public CommonResult<List<HrLeaveTypeRespVO>> getLeaveTypeList() {
        List<HrLeaveTypeDO> list = leaveTypeService.getLeaveTypeList();
        return success(BeanUtils.toBean(list, HrLeaveTypeRespVO.class));
    }

}
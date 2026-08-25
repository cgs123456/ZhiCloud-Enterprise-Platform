package cn.zhicloud.module.hr.controller.admin.leave;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveBalanceRespVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestApproveReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestPageReqVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestRespVO;
import cn.zhicloud.module.hr.controller.admin.leave.vo.HrLeaveRequestSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveBalanceDO;
import cn.zhicloud.module.hr.dal.dataobject.leave.HrLeaveRequestDO;
import cn.zhicloud.module.hr.service.leave.HrLeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 请假单")
@RestController
@RequestMapping("/hr/leave-request")
@Validated
public class HrLeaveRequestController {

    @Resource
    private HrLeaveRequestService leaveRequestService;

    @PostMapping("/create")
    @Operation(summary = "创建请假单")
    @PreAuthorize("@ss.hasPermission('hr:leave-request:create')")
    public CommonResult<Long> createLeaveRequest(@Valid @RequestBody HrLeaveRequestSaveReqVO createReqVO) {
        return success(leaveRequestService.createLeaveRequest(createReqVO));
    }

    @PutMapping("/approve")
    @Operation(summary = "审批请假单")
    @PreAuthorize("@ss.hasPermission('hr:leave-request:update')")
    public CommonResult<Boolean> approveLeaveRequest(@Valid @RequestBody HrLeaveRequestApproveReqVO reqVO) {
        leaveRequestService.approveLeaveRequest(reqVO);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "撤销请假单")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:leave-request:update')")
    public CommonResult<Boolean> cancelLeaveRequest(@RequestParam("id") Long id) {
        leaveRequestService.cancelLeaveRequest(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得请假单")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:leave-request:query')")
    public CommonResult<HrLeaveRequestRespVO> getLeaveRequest(@RequestParam("id") Long id) {
        HrLeaveRequestDO leaveRequest = leaveRequestService.getLeaveRequest(id);
        return success(BeanUtils.toBean(leaveRequest, HrLeaveRequestRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得请假单分页")
    @PreAuthorize("@ss.hasPermission('hr:leave-request:query')")
    public CommonResult<PageResult<HrLeaveRequestRespVO>> getLeaveRequestPage(@Valid HrLeaveRequestPageReqVO pageReqVO) {
        PageResult<HrLeaveRequestDO> pageResult = leaveRequestService.getLeaveRequestPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrLeaveRequestRespVO.class));
    }

    @GetMapping("/balance")
    @Operation(summary = "查询年度假期余额")
    @PreAuthorize("@ss.hasPermission('hr:leave-request:query')")
    public CommonResult<List<HrLeaveBalanceRespVO>> getLeaveBalance(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam(value = "year", required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        List<HrLeaveBalanceDO> list = leaveRequestService.getLeaveBalanceByYear(employeeId, y);
        return success(BeanUtils.toBean(list, HrLeaveBalanceRespVO.class));
    }

}
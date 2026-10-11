package cn.zhicloud.module.hr.controller.admin.hrm.insurance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo.*;
import cn.zhicloud.module.hr.service.hrm.insurance.HrmInsuranceMonthService;
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
 * HRM 保险域月度社保 Controller（月度社保表 + 员工月度社保记录）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 月度社保")
@RestController
@RequestMapping("/hrm/insurance")
@Validated
public class HrmInsuranceMonthController {

    @Resource
    private HrmInsuranceMonthService insuranceMonthService;

    // ========== 月度社保表 ==========

    @GetMapping("/month-record/list")
    @Operation(summary = "获得月度社保表列表")
    @Parameter(name = "year", description = "年份", required = false)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:query')")
    public CommonResult<List<InsuranceMonthRecordVO>> getMonthRecordList(
            @RequestParam(value = "year", required = false) Integer year) {
        return success(insuranceMonthService.getMonthRecordList(year));
    }

    @GetMapping("/month-record/get")
    @Operation(summary = "获得月度社保表详情")
    @Parameter(name = "id", description = "月度社保表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:query')")
    public CommonResult<InsuranceMonthRecordVO> getMonthRecord(@RequestParam("id") Long id) {
        return success(insuranceMonthService.getMonthRecord(id));
    }

    @GetMapping("/month-record/last")
    @Operation(summary = "获得最近月度社保表")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:query')")
    public CommonResult<InsuranceMonthRecordVO> getLastMonthRecord() {
        return success(insuranceMonthService.getLastMonthRecord());
    }

    @PostMapping("/month-record/create-first")
    @Operation(summary = "创建首月社保表")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:create')")
    public CommonResult<Long> createFirstMonthRecord(@Valid @RequestBody InsuranceMonthRecordCreateReqVO reqVO) {
        return success(insuranceMonthService.createFirstMonthRecord(reqVO));
    }

    @PostMapping("/month-record/create-next")
    @Operation(summary = "新建次月社保表")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:create')")
    public CommonResult<Long> createNextMonthRecord() {
        return success(insuranceMonthService.createNextMonthRecord());
    }

    @DeleteMapping("/month-record/delete")
    @Operation(summary = "删除月度社保表")
    @Parameter(name = "id", description = "月度社保表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:delete')")
    public CommonResult<Boolean> deleteMonthRecord(@RequestParam("id") Long id) {
        insuranceMonthService.deleteMonthRecord(id);
        return success(true);
    }

    // ========== 员工月度社保记录 ==========

    @GetMapping("/month-employee-record/page")
    @Operation(summary = "获得员工月度社保分页")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:query')")
    public CommonResult<PageResult<InsuranceMonthEmployeeRecordVO>> getMonthEmployeeRecordPage(
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "monthRecordId", required = false) Long monthRecordId) {
        return success(insuranceMonthService.getMonthEmployeeRecordPage(pageNo, pageSize, monthRecordId));
    }

    @GetMapping("/month-employee-record/get")
    @Operation(summary = "获得员工月度社保详情")
    @Parameter(name = "id", description = "员工月度社保记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:query')")
    public CommonResult<InsuranceMonthEmployeeRecordVO> getMonthEmployeeRecord(@RequestParam("id") Long id) {
        return success(insuranceMonthService.getMonthEmployeeRecord(id));
    }

    @PostMapping("/month-employee-record/create-list")
    @Operation(summary = "添加月度参保人员")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:employee-info:update')")
    public CommonResult<Long> createMonthEmployeeRecordList(@Valid @RequestBody InsuranceMonthEmployeeCreateListReqVO reqVO) {
        return success(insuranceMonthService.createMonthEmployeeRecordList(reqVO));
    }

    @PutMapping("/month-employee-record/update")
    @Operation(summary = "修改员工月度参保项目")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:employee-info:update')")
    public CommonResult<Boolean> updateMonthEmployeeRecord(@Valid @RequestBody InsuranceMonthEmployeeUpdateReqVO reqVO) {
        insuranceMonthService.updateMonthEmployeeRecord(reqVO);
        return success(true);
    }

    @PutMapping("/month-employee-record/stop-list")
    @Operation(summary = "停止员工月度参保")
    @PreAuthorize("@ss.hasPermission('hrm:insurance:employee-info:update')")
    public CommonResult<Boolean> stopMonthEmployeeRecordList(@Valid @RequestBody InsuranceMonthEmployeeStopListReqVO reqVO) {
        insuranceMonthService.stopMonthEmployeeRecordList(reqVO);
        return success(true);
    }

    @GetMapping("/month-employee-record/uninsured-employee-list")
    @Operation(summary = "查询本月未参保员工")
    @Parameter(name = "monthRecordId", description = "月度社保表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:insurance:month-record:query')")
    public CommonResult<List<Long>> getUninsuredEmployeeList(@RequestParam("monthRecordId") Long monthRecordId) {
        return success(insuranceMonthService.getUninsuredEmployeeIdList(monthRecordId));
    }

}

package cn.zhicloud.module.hr.controller.admin.hrm.salary;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.salary.vo.*;
import cn.zhicloud.module.hr.service.hrm.salary.HrmSalaryBusinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 薪资业务 Controller（员工薪资档案/调薪记录/月度工资表/员工月度工资/工资条）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 薪资业务")
@RestController
@RequestMapping("/hrm/salary")
@Validated
public class HrmSalaryBusinessController {

    @Resource
    private HrmSalaryBusinessService salaryBusinessService;

    // ========== 员工薪资档案 ==========

    @GetMapping("/employee-info/page")
    @Operation(summary = "获得员工薪资档案分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:query')")
    public CommonResult<PageResult<HrmSalaryEmployeeInfoVO>> getEmployeeInfoPage(@Valid HrmSalaryEmployeeInfoPageReqVO reqVO) {
        return success(salaryBusinessService.getEmployeeInfoPage(reqVO));
    }

    @GetMapping("/employee-info/get")
    @Operation(summary = "获得员工薪资档案详情")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:query')")
    public CommonResult<HrmSalaryEmployeeInfoVO> getEmployeeInfo(@RequestParam("employeeId") Long employeeId) {
        return success(salaryBusinessService.getEmployeeInfo(employeeId));
    }

    @GetMapping("/employee-info/status-count")
    @Operation(summary = "获得员工薪资档案状态数量")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:query')")
    public CommonResult<List<HrmSalaryEmployeeStatusCountRespVO>> getEmployeeStatusCount(@Valid HrmSalaryEmployeeInfoPageReqVO reqVO) {
        return success(salaryBusinessService.getEmployeeStatusCount(reqVO));
    }

    @PutMapping("/employee-info/update")
    @Operation(summary = "定薪/调薪")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:update')")
    public CommonResult<Boolean> updateEmployeeInfo(@Valid @RequestBody HrmSalaryEmployeeInfoUpdateReqVO reqVO) {
        salaryBusinessService.updateEmployeeInfo(reqVO);
        return success(true);
    }

    @PutMapping("/employee-info/update-list")
    @Operation(summary = "批量定薪/调薪")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:update')")
    public CommonResult<Boolean> updateEmployeeInfoList(@Valid @RequestBody List<HrmSalaryEmployeeInfoUpdateReqVO> list) {
        salaryBusinessService.updateEmployeeInfoList(list);
        return success(true);
    }

    @GetMapping("/employee-info/get-adjustment-min-effect-date")
    @Operation(summary = "获得最早调薪生效日期")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:query')")
    public CommonResult<LocalDateTime> getAdjustmentMinEffectDate() {
        return success(salaryBusinessService.getAdjustmentMinEffectDate());
    }

    @GetMapping("/employee-info/get-change-import-template")
    @Operation(summary = "获得调薪导入模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:import')")
    public void getChangeImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "调薪导入模板.xls", "数据", HrmSalaryEmployeeChangeRecordVO.class,
                List.of(new HrmSalaryEmployeeChangeRecordVO()));
    }

    @GetMapping("/employee-info/get-fix-import-template")
    @Operation(summary = "获得定薪导入模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:import')")
    public void getFixImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "定薪导入模板.xls", "数据", HrmSalaryEmployeeChangeRecordVO.class,
                List.of(new HrmSalaryEmployeeChangeRecordVO()));
    }

    // ========== 调薪记录 ==========

    @GetMapping("/change-record/list")
    @Operation(summary = "获得员工调薪记录列表")
    @Parameter(name = "employeeId", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:query')")
    public CommonResult<List<HrmSalaryEmployeeChangeRecordVO>> getChangeRecordList(@RequestParam("employeeId") Long employeeId) {
        return success(salaryBusinessService.getChangeRecordList(employeeId));
    }

    @GetMapping("/change-record/get")
    @Operation(summary = "获得调薪记录详情")
    @Parameter(name = "id", description = "调薪记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:query')")
    public CommonResult<HrmSalaryEmployeeChangeRecordVO> getChangeRecord(@RequestParam("id") Long id) {
        return success(salaryBusinessService.getChangeRecord(id));
    }

    @PutMapping("/change-record/cancel")
    @Operation(summary = "取消调薪记录")
    @Parameter(name = "id", description = "调薪记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:employee-info:update')")
    public CommonResult<Boolean> cancelChangeRecord(@RequestParam("id") Long id) {
        salaryBusinessService.cancelChangeRecord(id);
        return success(true);
    }

    @DeleteMapping("/change-record/delete")
    @Operation(summary = "删除调薪记录")
    @Parameter(name = "id", description = "调薪记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:change-record:delete')")
    public CommonResult<Boolean> deleteChangeRecord(@RequestParam("id") Long id) {
        salaryBusinessService.deleteChangeRecord(id);
        return success(true);
    }

    // ========== 月度工资表 ==========

    @GetMapping("/month-record/page")
    @Operation(summary = "获得月度工资表分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<PageResult<HrmSalaryMonthRecordVO>> getMonthRecordPage(@Valid HrmSalaryMonthRecordPageReqVO reqVO) {
        return success(salaryBusinessService.getMonthRecordPage(reqVO));
    }

    @GetMapping("/month-record/get")
    @Operation(summary = "获得月度工资表详情")
    @Parameter(name = "id", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<HrmSalaryMonthRecordVO> getMonthRecord(@RequestParam("id") Long id) {
        return success(salaryBusinessService.getMonthRecord(id));
    }

    @GetMapping("/month-record/last")
    @Operation(summary = "获得最近一期月度工资表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<HrmSalaryMonthRecordVO> getLastMonthRecord() {
        return success(salaryBusinessService.getLastMonthRecord());
    }

    @PostMapping("/month-record/create-next")
    @Operation(summary = "创建下月工资表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:create')")
    public CommonResult<Long> createNextMonthRecord() {
        return success(salaryBusinessService.createNextMonthRecord());
    }

    @DeleteMapping("/month-record/delete")
    @Operation(summary = "删除月度工资表")
    @Parameter(name = "id", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:delete')")
    public CommonResult<Boolean> deleteMonthRecord(@RequestParam("id") Long id) {
        salaryBusinessService.deleteMonthRecord(id);
        return success(true);
    }

    @GetMapping("/month-record/payroll-readiness")
    @Operation(summary = "获得薪资核算就绪状态")
    @Parameter(name = "monthRecordId", description = "月度工资表编号")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<HrmSalaryPayrollReadinessRespVO> getPayrollReadiness(@RequestParam(value = "monthRecordId", required = false) Long monthRecordId) {
        return success(salaryBusinessService.getPayrollReadiness(monthRecordId));
    }

    @PostMapping("/month-record/compute")
    @Operation(summary = "核算月度工资表")
    @Parameter(name = "id", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:compute')")
    public CommonResult<Long> computeMonthRecord(@RequestParam("id") Long id) {
        return success(salaryBusinessService.computeMonthRecord(id));
    }

    @PostMapping("/month-record/compute-import")
    @Operation(summary = "导入并核算月度工资表")
    @Parameter(name = "id", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:compute')")
    public CommonResult<Boolean> computeImport(@RequestParam("id") Long id) {
        salaryBusinessService.computeImport(id);
        return success(true);
    }

    @GetMapping("/month-record/option-summary")
    @Operation(summary = "获得月度工资薪资项汇总")
    @Parameter(name = "monthRecordId", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<List<SalaryOptionValueVO>> getMonthOptionSummary(@RequestParam("monthRecordId") Long monthRecordId) {
        return success(salaryBusinessService.getMonthOptionSummary(monthRecordId));
    }

    @GetMapping("/month-record/get-attendance-import-template")
    @Operation(summary = "获得薪资考勤导入模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:update')")
    public void getAttendanceImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "薪资考勤导入模板.xls", "数据", HrmSalaryMonthEmployeeRecordVO.class,
                Collections.emptyList());
    }

    @GetMapping("/month-record/get-additional-deduction-import-template")
    @Operation(summary = "获得专项附加扣除导入模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:update')")
    public void getAdditionalDeductionImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "专项附加扣除导入模板.xls", "数据", HrmSalaryMonthEmployeeRecordVO.class,
                Collections.emptyList());
    }

    @GetMapping("/month-record/get-cumulative-tax-import-template")
    @Operation(summary = "获得累计个税导入模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:update')")
    public void getCumulativeTaxImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "累计个税导入模板.xls", "数据", HrmSalaryMonthEmployeeRecordVO.class,
                Collections.emptyList());
    }

    // ========== 员工月度工资 ==========

    @GetMapping("/month-employee-record/page")
    @Operation(summary = "获得员工月度工资分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<PageResult<HrmSalaryMonthEmployeeRecordVO>> getMonthEmployeeRecordPage(@Valid HrmSalaryMonthEmployeeRecordPageReqVO reqVO) {
        return success(salaryBusinessService.getMonthEmployeeRecordPage(reqVO));
    }

    @GetMapping("/month-employee-record/employee-page")
    @Operation(summary = "获得指定员工的月度工资分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<PageResult<HrmSalaryMonthEmployeeRecordVO>> getMonthEmployeeRecordEmployeePage(@Valid HrmSalaryMonthEmployeeRecordPageReqVO reqVO) {
        return success(salaryBusinessService.getMonthEmployeeRecordEmployeePage(reqVO));
    }

    @GetMapping("/month-employee-record/list")
    @Operation(summary = "获得员工月度工资列表")
    @Parameter(name = "monthRecordId", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<List<HrmSalaryMonthEmployeeRecordVO>> getMonthEmployeeRecordList(@RequestParam("monthRecordId") Long monthRecordId) {
        return success(salaryBusinessService.getMonthEmployeeRecordList(monthRecordId));
    }

    @PutMapping("/month-employee-record/update-list")
    @Operation(summary = "批量修改员工月度工资")
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:update')")
    public CommonResult<Boolean> updateMonthEmployeeRecordList(@Valid @RequestBody HrmSalaryMonthEmployeeUpdateListReqVO reqVO) {
        salaryBusinessService.updateMonthEmployeeRecordList(reqVO);
        return success(true);
    }

    @GetMapping("/month-employee-record/change-count")
    @Operation(summary = "获得月度员工异动数量")
    @Parameter(name = "monthRecordId", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<List<HrmSalaryChangeCountRespVO>> getMonthEmployeeChangeCount(@RequestParam("monthRecordId") Long monthRecordId) {
        return success(salaryBusinessService.getMonthEmployeeChangeCount(monthRecordId));
    }

    @GetMapping("/month-employee-record/performance-coefficients")
    @Operation(summary = "获得绩效系数列表")
    @Parameter(name = "monthRecordId", description = "月度工资表编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:month-record:query')")
    public CommonResult<List<HrmSalaryPerformanceCoefficientVO>> getPerformanceCoefficients(@RequestParam("monthRecordId") Long monthRecordId) {
        return success(salaryBusinessService.getPerformanceCoefficients(monthRecordId));
    }

    // ========== 工资条模板 ==========

    @GetMapping("/slip-template/list")
    @Operation(summary = "获得工资条模板列表")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<List<HrmSalarySlipTemplateVO>> getSlipTemplateList() {
        return success(salaryBusinessService.getSlipTemplateList());
    }

    @GetMapping("/slip-template/get")
    @Operation(summary = "获得工资条模板详情")
    @Parameter(name = "id", description = "工资条模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<HrmSalarySlipTemplateVO> getSlipTemplate(@RequestParam("id") Long id) {
        return success(salaryBusinessService.getSlipTemplate(id));
    }

    @PostMapping("/slip-template/create")
    @Operation(summary = "创建工资条模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:create')")
    public CommonResult<Long> createSlipTemplate(@Valid @RequestBody HrmSalarySlipTemplateVO reqVO) {
        return success(salaryBusinessService.createSlipTemplate(reqVO));
    }

    @PutMapping("/slip-template/update")
    @Operation(summary = "修改工资条模板")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:update')")
    public CommonResult<Boolean> updateSlipTemplate(@Valid @RequestBody HrmSalarySlipTemplateVO reqVO) {
        salaryBusinessService.updateSlipTemplate(reqVO);
        return success(true);
    }

    @DeleteMapping("/slip-template/delete")
    @Operation(summary = "删除工资条模板")
    @Parameter(name = "id", description = "工资条模板编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:delete')")
    public CommonResult<Boolean> deleteSlipTemplate(@RequestParam("id") Long id) {
        salaryBusinessService.deleteSlipTemplate(id);
        return success(true);
    }

    // ========== 工资条发放记录 ==========

    @GetMapping("/slip-send-record/page")
    @Operation(summary = "获得工资条发放记录分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<PageResult<HrmSalarySlipSendRecordVO>> getSlipSendRecordPage(
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "monthRecordId", required = false) Long monthRecordId) {
        return success(salaryBusinessService.getSlipSendRecordPage(pageNo, pageSize, monthRecordId));
    }

    @GetMapping("/slip-send-record/get")
    @Operation(summary = "获得工资条发放记录详情")
    @Parameter(name = "id", description = "工资条发放记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<HrmSalarySlipSendRecordVO> getSlipSendRecord(@RequestParam("id") Long id) {
        return success(salaryBusinessService.getSlipSendRecord(id));
    }

    @PostMapping("/slip-send-record/create")
    @Operation(summary = "发放工资条")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:create')")
    public CommonResult<Long> createSlipSendRecord(@Valid @RequestBody HrmSalarySlipSendRecordCreateReqVO reqVO) {
        return success(salaryBusinessService.createSlipSendRecord(reqVO));
    }

    @DeleteMapping("/slip-send-record/delete")
    @Operation(summary = "删除工资条发放记录")
    @Parameter(name = "id", description = "工资条发放记录编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:delete')")
    public CommonResult<Boolean> deleteSlipSendRecord(@RequestParam("id") Long id) {
        salaryBusinessService.deleteSlipSendRecord(id);
        return success(true);
    }

    @GetMapping("/slip-send-record/employee-page")
    @Operation(summary = "获得工资条待发员工分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<PageResult<HrmSalaryMonthEmployeeRecordVO>> getSlipSendEmployeePage(
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "monthRecordId", required = false) Long monthRecordId) {
        HrmSalaryMonthEmployeeRecordPageReqVO reqVO = new HrmSalaryMonthEmployeeRecordPageReqVO();
        reqVO.setPageNo(pageNo);
        reqVO.setPageSize(pageSize);
        reqVO.setMonthRecordId(monthRecordId);
        return success(salaryBusinessService.getMonthEmployeeRecordEmployeePage(reqVO));
    }

    // ========== 工资条 ==========

    @GetMapping("/slip/page")
    @Operation(summary = "获得工资条分页")
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<PageResult<HrmSalarySlipVO>> getSlipPage(
            @RequestParam(value = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "sendRecordId", required = false) Long sendRecordId) {
        return success(salaryBusinessService.getSlipPage(pageNo, pageSize, employeeId, sendRecordId));
    }

    @GetMapping("/slip/get")
    @Operation(summary = "获得工资条详情")
    @Parameter(name = "id", description = "工资条编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:query')")
    public CommonResult<HrmSalarySlipVO> getSlip(@RequestParam("id") Long id) {
        return success(salaryBusinessService.getSlip(id));
    }

    @PutMapping("/slip/remark")
    @Operation(summary = "标记工资条已查看")
    @Parameter(name = "id", description = "工资条编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:salary:slip:update')")
    public CommonResult<Boolean> remarkSlip(@RequestParam("id") Long id) {
        salaryBusinessService.remarkSlip(id);
        return success(true);
    }

}

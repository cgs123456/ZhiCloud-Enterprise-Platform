package cn.zhicloud.module.hr.controller.admin.hrm;

import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.excel.core.util.ExcelUtils;
import cn.zhicloud.module.hr.controller.admin.hrm.vo.*;
import cn.zhicloud.module.hr.service.hrm.HrmEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 员工档案 Controller（前端 views/hrm/employee 契约）
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 员工档案")
@RestController
@RequestMapping("/hrm/employee")
@Validated
public class HrmEmployeeController {

    @Resource
    private HrmEmployeeService employeeService;

    // ========== 查询 ==========

    @GetMapping("/page")
    @Operation(summary = "获得员工档案分页")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<PageResult<HrmEmployeeRespVO>> getEmployeePage(@Valid HrmEmployeePageReqVO pageReqVO) {
        return success(employeeService.getEmployeePage(pageReqVO));
    }

    @GetMapping("/simple-page")
    @Operation(summary = "获得员工精简分页")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<PageResult<HrmEmployeeRespVO>> getEmployeeSimplePage(@Valid HrmEmployeePageReqVO pageReqVO) {
        return success(employeeService.getEmployeePage(pageReqVO));
    }

    @GetMapping("/list")
    @Operation(summary = "获得指定员工列表")
    @Parameter(name = "ids", description = "员工编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeRespVO>> getEmployeeList(@RequestParam("ids") String ids) {
        return success(employeeService.getEmployeeList(splitIds(ids)));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得指定员工精简列表")
    @Parameter(name = "ids", description = "员工编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeRespVO>> getEmployeeSimpleList(@RequestParam("ids") String ids) {
        return success(employeeService.getEmployeeSimpleList(splitIds(ids)));
    }

    @GetMapping("/get")
    @Operation(summary = "获得员工档案详情")
    @Parameter(name = "id", description = "员工编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<HrmEmployeeRespVO> getEmployee(@RequestParam("id") Long id) {
        return success(employeeService.getEmployee(id));
    }

    @GetMapping("/status-count")
    @Operation(summary = "获得员工状态数量")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeStatusCountRespVO>> getEmployeeStatusCount(@Valid HrmEmployeePageReqVO pageReqVO) {
        return success(employeeService.getEmployeeStatusCount(pageReqVO));
    }

    @GetMapping("/dept-statistics")
    @Operation(summary = "获得员工部门统计")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<HrmEmployeeDeptStatisticsRespVO>> getEmployeeDeptStatistics() {
        return success(employeeService.getEmployeeDeptStatistics());
    }

    @GetMapping("/bound-user-id-list")
    @Operation(summary = "获得已建档后台用户编号列表")
    @PreAuthorize("@ss.hasPermission('hrm:employee:query')")
    public CommonResult<List<Long>> getBoundUserIdList() {
        return success(employeeService.getBoundUserIdList());
    }

    // ========== CRUD ==========

    @PostMapping("/create")
    @Operation(summary = "创建员工档案")
    @PreAuthorize("@ss.hasPermission('hrm:employee:create')")
    public CommonResult<Long> createEmployee(@Valid @RequestBody HrmEmployeeSaveReqVO createReqVO) {
        return success(employeeService.createEmployee(createReqVO));
    }

    @PostMapping("/create-list")
    @Operation(summary = "从后台用户批量创建员工档案")
    @PreAuthorize("@ss.hasPermission('hrm:employee:create')")
    public CommonResult<List<Long>> createEmployeeList(@Valid @RequestBody List<HrmEmployeeCreateFromUserReqVO> list) {
        return success(employeeService.createEmployeeListFromUser(list));
    }

    @PutMapping("/update")
    @Operation(summary = "修改员工档案")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody HrmEmployeeSaveReqVO updateReqVO) {
        employeeService.updateEmployee(updateReqVO);
        return success(true);
    }

    @PutMapping("/confirm-entry")
    @Operation(summary = "确认员工入职")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> confirmEmployeeEntry(@Valid @RequestBody HrmEmployeeSaveReqVO reqVO) {
        employeeService.confirmEntry(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工档案")
    @Parameter(name = "id", description = "员工编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteEmployee(@RequestParam("id") Long id) {
        employeeService.deleteEmployee(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除员工档案")
    @Parameter(name = "ids", description = "员工编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:delete')")
    public CommonResult<Boolean> deleteEmployeeList(@RequestParam("ids") String ids) {
        employeeService.deleteEmployeeList(splitIds(ids));
        return success(true);
    }

    // ========== 生命周期 ==========

    @PostMapping("/rehire")
    @Operation(summary = "办理员工再入职")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> rehireEmployee(@Valid @RequestBody HrmEmployeeRehireReqVO reqVO) {
        employeeService.rehireEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/regular")
    @Operation(summary = "办理员工转正")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> regularEmployee(@Valid @RequestBody HrmEmployeeChangeReqVO reqVO) {
        employeeService.regularEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/transfer")
    @Operation(summary = "办理员工调岗")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> transferEmployee(@Valid @RequestBody HrmEmployeeChangeReqVO reqVO) {
        employeeService.transferEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/promote")
    @Operation(summary = "办理员工晋升")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> promoteEmployee(@Valid @RequestBody HrmEmployeeChangeReqVO reqVO) {
        employeeService.promoteEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/demote")
    @Operation(summary = "办理员工降级")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> demoteEmployee(@Valid @RequestBody HrmEmployeeChangeReqVO reqVO) {
        employeeService.demoteEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/convert-to-full-time")
    @Operation(summary = "办理员工转为全职")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> convertEmployeeToFullTime(@Valid @RequestBody HrmEmployeeChangeReqVO reqVO) {
        employeeService.convertToFullTimeEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/quit")
    @Operation(summary = "办理员工离职")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> quitEmployee(@Valid @RequestBody HrmEmployeeQuitReqVO reqVO) {
        employeeService.quitEmployee(reqVO);
        return success(true);
    }

    @PutMapping("/cancel-quit")
    @Operation(summary = "取消员工离职")
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<Boolean> cancelEmployeeQuit(@Valid @RequestBody HrmEmployeeCancelQuitReqVO reqVO) {
        employeeService.cancelQuitEmployee(reqVO);
        return success(true);
    }

    @PostMapping("/send-profile-fill-message")
    @Operation(summary = "发送填写员工档案通知")
    @Parameter(name = "ids", description = "员工编号列表，逗号分隔", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:employee:update')")
    public CommonResult<HrmEmployeeNotifyRespVO> sendEmployeeProfileFillMessage(@RequestParam("ids") String ids) {
        return success(employeeService.sendProfileFillMessage(splitIds(ids)));
    }

    // ========== 导入 / 导出 ==========

    @GetMapping("/export-excel")
    @Operation(summary = "导出员工档案 Excel")
    @PreAuthorize("@ss.hasPermission('hrm:employee:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEmployeeExcel(@Valid HrmEmployeePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(-1); // 不分页
        List<HrmEmployeeRespVO> list = employeeService.getEmployeePage(pageReqVO).getList();
        List<HrmEmployeeExportExcelVO> excelRows = CollectionUtils.convertList(list, vo -> {
            HrmEmployeeExportExcelVO row = new HrmEmployeeExportExcelVO();
            row.setId(vo.getId());
            row.setName(vo.getName());
            row.setJobNumber(vo.getJobNumber());
            row.setMobile(vo.getMobile());
            row.setSex(vo.getSex());
            row.setEmail(vo.getEmail());
            row.setDeptName(vo.getDeptName());
            row.setPostName(vo.getPostName());
            row.setType(vo.getType());
            row.setEntryStatus(vo.getEntryStatus());
            row.setEntryTime(vo.getEntryTime());
            row.setRemark(vo.getRemark());
            return row;
        });
        ExcelUtils.write(response, "员工档案.xls", "数据", HrmEmployeeExportExcelVO.class, excelRows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入员工档案模板")
    @PreAuthorize("@ss.hasPermission('hrm:employee:import')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        HrmEmployeeImportExcelVO sample = new HrmEmployeeImportExcelVO();
        sample.setName("张三");
        sample.setJobNumber("EMP001");
        sample.setMobile("13800138000");
        sample.setSex(1);
        sample.setEmail("zhangsan@zhicloud.cn");
        sample.setDeptId(103L);
        sample.setPostName("软件工程师");
        sample.setType(1);
        sample.setStatus(1);
        sample.setEntryTime(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        sample.setIdNumber("110101199001011234");
        sample.setRemark("示例行，导入前请删除");
        ExcelUtils.write(response, "员工档案导入模板.xls", "员工档案列表",
                HrmEmployeeImportExcelVO.class, Collections.singletonList(sample));
    }

    @PostMapping("/import")
    @Operation(summary = "导入员工档案")
    @PreAuthorize("@ss.hasPermission('hrm:employee:import')")
    public CommonResult<HrmEmployeeImportRespVO> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "duplicateStrategy", required = false, defaultValue = "3") Integer duplicateStrategy)
            throws Exception {
        List<HrmEmployeeImportExcelVO> list = ExcelUtils.read(file, HrmEmployeeImportExcelVO.class);
        return success(employeeService.importEmployeeList(list, duplicateStrategy));
    }

    private List<Long> splitIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

}

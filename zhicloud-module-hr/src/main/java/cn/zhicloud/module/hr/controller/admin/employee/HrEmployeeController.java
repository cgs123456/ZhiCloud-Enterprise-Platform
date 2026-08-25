package cn.zhicloud.module.hr.controller.admin.employee;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeePageReqVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeResignReqVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeRespVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeSaveReqVO;
import cn.zhicloud.module.hr.controller.admin.employee.vo.HrEmployeeTransferReqVO;
import cn.zhicloud.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.zhicloud.module.hr.service.employee.HrEmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 员工档案")
@RestController
@RequestMapping("/hr/employee")
@Validated
public class HrEmployeeController {

    @Resource
    private HrEmployeeService employeeService;

    @PostMapping("/create")
    @Operation(summary = "创建员工")
    @PreAuthorize("@ss.hasPermission('hr:employee:create')")
    public CommonResult<Long> createEmployee(@Valid @RequestBody HrEmployeeSaveReqVO createReqVO) {
        return success(employeeService.createEmployee(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody HrEmployeeSaveReqVO updateReqVO) {
        employeeService.updateEmployee(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:employee:delete')")
    public CommonResult<Boolean> deleteEmployee(@RequestParam("id") Long id) {
        employeeService.deleteEmployee(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得员工")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<HrEmployeeRespVO> getEmployee(@RequestParam("id") Long id) {
        HrEmployeeDO employee = employeeService.getEmployee(id);
        return success(BeanUtils.toBean(employee, HrEmployeeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得员工分页")
    @PreAuthorize("@ss.hasPermission('hr:employee:query')")
    public CommonResult<PageResult<HrEmployeeRespVO>> getEmployeePage(@Valid HrEmployeePageReqVO pageReqVO) {
        PageResult<HrEmployeeDO> pageResult = employeeService.getEmployeePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrEmployeeRespVO.class));
    }

    @PutMapping("/resign")
    @Operation(summary = "员工离职")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> resignEmployee(@Valid @RequestBody HrEmployeeResignReqVO reqVO) {
        employeeService.resignEmployee(reqVO);
        return success(true);
    }

    @PutMapping("/transfer")
    @Operation(summary = "员工调动")
    @PreAuthorize("@ss.hasPermission('hr:employee:update')")
    public CommonResult<Boolean> transferEmployee(@Valid @RequestBody HrEmployeeTransferReqVO reqVO) {
        employeeService.transferEmployee(reqVO);
        return success(true);
    }

}
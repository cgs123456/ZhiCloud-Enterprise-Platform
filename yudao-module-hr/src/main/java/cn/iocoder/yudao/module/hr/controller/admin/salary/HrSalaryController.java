package cn.iocoder.yudao.module.hr.controller.admin.salary;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryApproveReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryCalculateReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryPageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalaryRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.salary.vo.HrSalarySaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.salary.HrSalaryDO;
import cn.iocoder.yudao.module.hr.service.salary.HrSalaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HR 薪资记录")
@RestController
@RequestMapping("/hr/salary")
@Validated
public class HrSalaryController {

    @Resource
    private HrSalaryService salaryService;

    @PostMapping("/create")
    @Operation(summary = "创建薪资记录")
    @PreAuthorize("@ss.hasPermission('hr:salary:create')")
    public CommonResult<Long> createSalary(@Valid @RequestBody HrSalarySaveReqVO createReqVO) {
        return success(salaryService.createSalary(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新薪资记录")
    @PreAuthorize("@ss.hasPermission('hr:salary:update')")
    public CommonResult<Boolean> updateSalary(@Valid @RequestBody HrSalarySaveReqVO updateReqVO) {
        salaryService.updateSalary(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除薪资记录")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:salary:delete')")
    public CommonResult<Boolean> deleteSalary(@RequestParam("id") Long id) {
        salaryService.deleteSalary(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得薪资记录")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:salary:query')")
    public CommonResult<HrSalaryRespVO> getSalary(@RequestParam("id") Long id) {
        HrSalaryDO salary = salaryService.getSalary(id);
        return success(BeanUtils.toBean(salary, HrSalaryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得薪资记录分页")
    @PreAuthorize("@ss.hasPermission('hr:salary:query')")
    public CommonResult<PageResult<HrSalaryRespVO>> getSalaryPage(@Valid HrSalaryPageReqVO pageReqVO) {
        PageResult<HrSalaryDO> pageResult = salaryService.getSalaryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrSalaryRespVO.class));
    }

    @PostMapping("/calculate")
    @Operation(summary = "月度薪资核算")
    @PreAuthorize("@ss.hasPermission('hr:salary:update')")
    public CommonResult<Long> calculateMonthlySalary(@Valid @RequestBody HrSalaryCalculateReqVO reqVO) {
        return success(salaryService.calculateMonthlySalary(reqVO));
    }

    @PutMapping("/approve")
    @Operation(summary = "审核薪资")
    @PreAuthorize("@ss.hasPermission('hr:salary:update')")
    public CommonResult<Boolean> approveSalary(@Valid @RequestBody HrSalaryApproveReqVO reqVO) {
        salaryService.approveSalary(reqVO);
        return success(true);
    }

}
package cn.zhicloud.module.hr.controller.admin.department;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentListReqVO;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentRespVO;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentSaveReqVO;
import cn.zhicloud.module.hr.controller.admin.department.vo.HrDepartmentSimpleRespVO;
import cn.zhicloud.module.hr.dal.dataobject.department.HrDepartmentDO;
import cn.zhicloud.module.hr.enums.department.HrDepartmentStatusEnum;
import cn.zhicloud.module.hr.service.department.HrDepartmentService;
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

@Tag(name = "管理后台 - HR 部门")
@RestController
@RequestMapping("/hr/department")
@Validated
public class HrDepartmentController {

    @Resource
    private HrDepartmentService departmentService;

    @PostMapping("/create")
    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermission('hr:department:create')")
    public CommonResult<Long> createDepartment(@Valid @RequestBody HrDepartmentSaveReqVO createReqVO) {
        return success(departmentService.createDepartment(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermission('hr:department:update')")
    public CommonResult<Boolean> updateDepartment(@Valid @RequestBody HrDepartmentSaveReqVO updateReqVO) {
        departmentService.updateDepartment(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:department:delete')")
    public CommonResult<Boolean> deleteDepartment(@RequestParam("id") Long id) {
        departmentService.deleteDepartment(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:department:query')")
    public CommonResult<HrDepartmentRespVO> getDepartment(@RequestParam("id") Long id) {
        HrDepartmentDO department = departmentService.getDepartment(id);
        return success(BeanUtils.toBean(department, HrDepartmentRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得部门列表")
    @PreAuthorize("@ss.hasPermission('hr:department:query')")
    public CommonResult<List<HrDepartmentRespVO>> getDepartmentList(HrDepartmentListReqVO reqVO) {
        List<HrDepartmentDO> list = departmentService.getDepartmentList(reqVO);
        return success(BeanUtils.toBean(list, HrDepartmentRespVO.class));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "获得部门精简信息列表", description = "只包含启用的部门，主要用于前端的下拉选项")
    public CommonResult<List<HrDepartmentSimpleRespVO>> getSimpleDepartmentList() {
        HrDepartmentListReqVO reqVO = new HrDepartmentListReqVO();
        reqVO.setStatus(HrDepartmentStatusEnum.ENABLE.getStatus());
        List<HrDepartmentDO> list = departmentService.getDepartmentList(reqVO);
        return success(BeanUtils.toBean(list, HrDepartmentSimpleRespVO.class));
    }

}
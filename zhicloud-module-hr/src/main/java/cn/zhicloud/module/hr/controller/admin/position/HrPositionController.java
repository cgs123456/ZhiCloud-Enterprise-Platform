package cn.zhicloud.module.hr.controller.admin.position;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionPageReqVO;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionRespVO;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.position.HrPositionDO;
import cn.zhicloud.module.hr.service.position.HrPositionService;
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

@Tag(name = "管理后台 - HR 职位")
@RestController
@RequestMapping("/hr/position")
@Validated
public class HrPositionController {

    @Resource
    private HrPositionService positionService;

    @PostMapping("/create")
    @Operation(summary = "创建职位")
    @PreAuthorize("@ss.hasPermission('hr:position:create')")
    public CommonResult<Long> createPosition(@Valid @RequestBody HrPositionSaveReqVO createReqVO) {
        return success(positionService.createPosition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新职位")
    @PreAuthorize("@ss.hasPermission('hr:position:update')")
    public CommonResult<Boolean> updatePosition(@Valid @RequestBody HrPositionSaveReqVO updateReqVO) {
        positionService.updatePosition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除职位")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('hr:position:delete')")
    public CommonResult<Boolean> deletePosition(@RequestParam("id") Long id) {
        positionService.deletePosition(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得职位")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hr:position:query')")
    public CommonResult<HrPositionRespVO> getPosition(@RequestParam("id") Long id) {
        HrPositionDO position = positionService.getPosition(id);
        return success(BeanUtils.toBean(position, HrPositionRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得职位分页")
    @PreAuthorize("@ss.hasPermission('hr:position:query')")
    public CommonResult<PageResult<HrPositionRespVO>> getPositionPage(@Valid HrPositionPageReqVO pageReqVO) {
        PageResult<HrPositionDO> pageResult = positionService.getPositionPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HrPositionRespVO.class));
    }

    @GetMapping("/list-by-dept")
    @Operation(summary = "获得指定部门的职位列表")
    @Parameter(name = "deptId", description = "部门编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('hr:position:query')")
    public CommonResult<List<HrPositionRespVO>> getPositionListByDept(@RequestParam("deptId") Long deptId) {
        List<HrPositionDO> list = positionService.getPositionListByDeptId(deptId);
        return success(BeanUtils.toBean(list, HrPositionRespVO.class));
    }

}
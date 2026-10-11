package cn.zhicloud.module.hr.controller.admin.hrm;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionPageReqVO;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionRespVO;
import cn.zhicloud.module.hr.controller.admin.position.vo.HrPositionSaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.position.HrPositionDO;
import cn.zhicloud.module.hr.service.position.HrPositionService;
import cn.zhicloud.module.system.api.dept.DeptApi;
import cn.zhicloud.module.system.api.dept.dto.DeptRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * HRM 职位管理 Controller（HR/HRM 融合：职位主数据）
 *
 * 融合口径（台账 §12.199）：职位主数据是 HR 域既有能力（hr_position 表 + HrPositionService），
 * HRM 侧只补一个 /hrm/position 入口复用**同一服务与同一张表**——单一真相，不建平行表；
 * 旧 /hr/position 与新 /hrm/position 并存（旧页面退役后旧入口随之下线）。
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - HRM 职位管理")
@RestController
@RequestMapping("/hrm/position")
@Validated
public class HrmPositionController {

    @Resource
    private HrPositionService positionService;
    @Resource
    private DeptApi deptApi;

    @GetMapping("/page")
    @Operation(summary = "获得职位分页")
    @PreAuthorize("@ss.hasPermission('hrm:position:query')")
    public CommonResult<PageResult<HrPositionRespVO>> getPositionPage(@Valid HrPositionPageReqVO pageReqVO) {
        PageResult<HrPositionDO> pageResult = positionService.getPositionPage(pageReqVO);
        PageResult<HrPositionRespVO> respPage = BeanUtils.toBean(pageResult, HrPositionRespVO.class);
        enrichDeptName(respPage.getList());
        return success(respPage);
    }

    @GetMapping("/get")
    @Operation(summary = "获得职位详情")
    @Parameter(name = "id", description = "职位编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('hrm:position:query')")
    public CommonResult<HrPositionRespVO> getPosition(@RequestParam("id") Long id) {
        HrPositionRespVO respVO = BeanUtils.toBean(positionService.getPosition(id), HrPositionRespVO.class);
        enrichDeptName(java.util.Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/list-by-dept")
    @Operation(summary = "获得指定部门的职位列表")
    @Parameter(name = "deptId", description = "部门编号", required = true, example = "2048")
    @PreAuthorize("@ss.hasPermission('hrm:position:query')")
    public CommonResult<List<HrPositionRespVO>> getPositionListByDept(@RequestParam("deptId") Long deptId) {
        List<HrPositionRespVO> list = BeanUtils.toBean(positionService.getPositionListByDeptId(deptId), HrPositionRespVO.class);
        enrichDeptName(list);
        return success(list);
    }

    @PostMapping("/create")
    @Operation(summary = "创建职位")
    @PreAuthorize("@ss.hasPermission('hrm:position:create')")
    public CommonResult<Long> createPosition(@Valid @RequestBody HrPositionSaveReqVO createReqVO) {
        return success(positionService.createPosition(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新职位")
    @PreAuthorize("@ss.hasPermission('hrm:position:update')")
    public CommonResult<Boolean> updatePosition(@Valid @RequestBody HrPositionSaveReqVO updateReqVO) {
        positionService.updatePosition(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除职位")
    @Parameter(name = "id", description = "职位编号", required = true)
    @PreAuthorize("@ss.hasPermission('hrm:position:delete')")
    public CommonResult<Boolean> deletePosition(@RequestParam("id") Long id) {
        positionService.deletePosition(id);
        return success(true);
    }


    /**
     * 部门名称 enrich（HRM 职位页展示；旧 /hr 入口不调本方法，无副作用）
     */
    private void enrichDeptName(List<HrPositionRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> deptIds = list.stream().map(HrPositionRespVO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(deptIds);
        for (HrPositionRespVO vo : list) {
            DeptRespDTO dept = deptMap.get(vo.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }
    }

}

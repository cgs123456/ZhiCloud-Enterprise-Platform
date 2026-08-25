package cn.zhicloud.module.erp.controller.admin.production.mps;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.production.mps.vo.ErpMpsPlanDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.production.mps.vo.ErpMpsPlanGenerateReqVO;
import cn.zhicloud.module.erp.controller.admin.production.mps.vo.ErpMpsPlanPageReqVO;
import cn.zhicloud.module.erp.controller.admin.production.mps.vo.ErpMpsPlanRespVO;
import cn.zhicloud.module.erp.controller.admin.production.mps.vo.ErpMpsPlanSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;
import cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;
import cn.zhicloud.module.erp.service.production.mps.ErpMpsPlanService;
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

@Tag(name = "管理后台 - ERP 主生产计划")
@RestController
@RequestMapping("/erp/mps-plan")
@Validated
public class ErpMpsPlanController {

    @Resource
    private ErpMpsPlanService mpsPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建主生产计划")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:create')")
    public CommonResult<Long> createMpsPlan(@Valid @RequestBody ErpMpsPlanSaveReqVO createReqVO) {
        return success(mpsPlanService.createMpsPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新主生产计划")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:update')")
    public CommonResult<Boolean> updateMpsPlan(@Valid @RequestBody ErpMpsPlanSaveReqVO updateReqVO) {
        mpsPlanService.updateMpsPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除主生产计划")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:delete')")
    public CommonResult<Boolean> deleteMpsPlan(@RequestParam("ids") List<Long> ids) {
        mpsPlanService.deleteMpsPlan(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得主生产计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:query')")
    public CommonResult<ErpMpsPlanRespVO> getMpsPlan(@RequestParam("id") Long id) {
        ErpMpsPlanDO plan = mpsPlanService.getMpsPlan(id);
        if (plan == null) {
            return success(null);
        }
        ErpMpsPlanRespVO respVO = BeanUtils.toBean(plan, ErpMpsPlanRespVO.class);
        // 查询明细
        List<ErpMpsPlanDetailDO> details = mpsPlanService.getMpsPlanDetailListByPlanId(id);
        respVO.setDetails(BeanUtils.toBean(details, ErpMpsPlanDetailRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得主生产计划分页")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:query')")
    public CommonResult<PageResult<ErpMpsPlanRespVO>> getMpsPlanPage(@Valid ErpMpsPlanPageReqVO pageReqVO) {
        PageResult<ErpMpsPlanDO> pageResult = mpsPlanService.getMpsPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpMpsPlanRespVO.class));
    }

    @PostMapping("/generate")
    @Operation(summary = "生成 MPS 计划（核心算法）")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:generate')")
    public CommonResult<Long> generateMpsPlan(@Valid @RequestBody ErpMpsPlanGenerateReqVO reqVO) {
        return success(mpsPlanService.generateMpsPlan(reqVO));
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认主生产计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:confirm')")
    public CommonResult<Boolean> confirmPlan(@RequestParam("id") Long id) {
        mpsPlanService.confirmPlan(id);
        return success(true);
    }

    @PutMapping("/release-mrp")
    @Operation(summary = "下发 MRP")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:release')")
    public CommonResult<Boolean> releaseToMrp(@RequestParam("id") Long id) {
        mpsPlanService.releaseToMrp(id);
        return success(true);
    }

    @PutMapping("/close")
    @Operation(summary = "关闭主生产计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:close')")
    public CommonResult<Boolean> closePlan(@RequestParam("id") Long id) {
        mpsPlanService.closePlan(id);
        return success(true);
    }

    @GetMapping("/detail/list")
    @Operation(summary = "获得主生产计划明细列表")
    @Parameter(name = "planId", description = "主生产计划编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mps-plan:query')")
    public CommonResult<List<ErpMpsPlanDetailRespVO>> getMpsPlanDetailList(@RequestParam("planId") Long planId) {
        List<ErpMpsPlanDetailDO> list = mpsPlanService.getMpsPlanDetailListByPlanId(planId);
        return success(BeanUtils.toBean(list, ErpMpsPlanDetailRespVO.class));
    }

}
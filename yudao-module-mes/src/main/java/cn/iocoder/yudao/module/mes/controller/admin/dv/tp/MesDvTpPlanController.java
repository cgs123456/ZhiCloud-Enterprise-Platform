package cn.iocoder.yudao.module.mes.controller.admin.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpExecuteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import cn.iocoder.yudao.module.mes.service.dv.tp.MesDvTpPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "管理后台 - MES TPM 计划")
@RestController
@RequestMapping("/mes/dv/tp-plan")
@Validated
public class MesDvTpPlanController {

    @Resource
    private MesDvTpPlanService tpPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建 TPM 计划")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:create')")
    public CommonResult<Long> createTpPlan(@Valid @RequestBody MesDvTpPlanSaveReqVO createReqVO) {
        return success(tpPlanService.createTpPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 TPM 计划")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:update')")
    public CommonResult<Boolean> updateTpPlan(@Valid @RequestBody MesDvTpPlanSaveReqVO updateReqVO) {
        tpPlanService.updateTpPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 TPM 计划")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:delete')")
    public CommonResult<Boolean> deleteTpPlan(@RequestParam("ids") List<Long> ids) {
        tpPlanService.deleteTpPlan(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 TPM 计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:query')")
    public CommonResult<MesDvTpPlanRespVO> getTpPlan(@RequestParam("id") Long id) {
        MesDvTpPlanDO plan = tpPlanService.getTpPlan(id);
        if (plan == null) {
            return success(null);
        }
        MesDvTpPlanRespVO respVO = BeanUtils.toBean(plan, MesDvTpPlanRespVO.class);
        List<MesDvTpPlanItemDO> items = tpPlanService.getTpPlanItemListByPlanId(id);
        respVO.setItems(BeanUtils.toBean(items, MesDvTpPlanItemRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 TPM 计划分页")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:query')")
    public CommonResult<PageResult<MesDvTpPlanRespVO>> getTpPlanPage(@Valid MesDvTpPlanPageReqVO pageReqVO) {
        PageResult<MesDvTpPlanDO> pageResult = tpPlanService.getTpPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MesDvTpPlanRespVO.class));
    }

    @PutMapping("/enable")
    @Operation(summary = "启用 TPM 计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:update')")
    public CommonResult<Boolean> enablePlan(@RequestParam("id") Long id) {
        tpPlanService.enablePlan(id);
        return success(true);
    }

    @PutMapping("/disable")
    @Operation(summary = "禁用 TPM 计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:update')")
    public CommonResult<Boolean> disablePlan(@RequestParam("id") Long id) {
        tpPlanService.disablePlan(id);
        return success(true);
    }

    @PostMapping("/execute")
    @Operation(summary = "执行 TPM 计划")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:execute')")
    public CommonResult<Long> executePlan(@Valid @RequestBody MesDvTpExecuteReqVO reqVO) {
        return success(tpPlanService.executePlan(reqVO, getLoginUserId()));
    }

    @GetMapping("/overdue")
    @Operation(summary = "查询逾期未执行的计划")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:query')")
    public CommonResult<List<MesDvTpPlanRespVO>> getOverduePlans() {
        List<MesDvTpPlanDO> list = tpPlanService.getOverduePlans();
        return success(BeanUtils.toBean(list, MesDvTpPlanRespVO.class));
    }

    @GetMapping("/record/list")
    @Operation(summary = "获得 TPM 执行记录列表")
    @Parameter(name = "planId", description = "TPM 计划编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:query')")
    public CommonResult<List<MesDvTpRecordRespVO>> getTpRecordList(@RequestParam("planId") Long planId) {
        List<MesDvTpRecordDO> list = tpPlanService.getTpRecordListByPlanId(planId);
        return success(BeanUtils.toBean(list, MesDvTpRecordRespVO.class));
    }

}
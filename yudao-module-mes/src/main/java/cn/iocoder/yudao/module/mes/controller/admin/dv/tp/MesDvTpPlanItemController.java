package cn.iocoder.yudao.module.mes.controller.admin.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanItemSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import cn.iocoder.yudao.module.mes.service.dv.tp.MesDvTpPlanItemService;
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

@Tag(name = "管理后台 - MES TPM 计划项目")
@RestController
@RequestMapping("/mes/dv/tp-plan-item")
@Validated
public class MesDvTpPlanItemController {

    @Resource
    private MesDvTpPlanItemService tpPlanItemService;

    @PostMapping("/create")
    @Operation(summary = "添加 TPM 计划项目")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:create')")
    public CommonResult<Long> addPlanItem(@Valid @RequestBody MesDvTpPlanItemSaveReqVO createReqVO) {
        return success(tpPlanItemService.addPlanItem(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 TPM 计划项目")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:update')")
    public CommonResult<Boolean> updatePlanItem(@Valid @RequestBody MesDvTpPlanItemSaveReqVO updateReqVO) {
        tpPlanItemService.updatePlanItem(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 TPM 计划项目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:delete')")
    public CommonResult<Boolean> deletePlanItem(@RequestParam("id") Long id) {
        tpPlanItemService.deletePlanItem(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 TPM 计划项目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:query')")
    public CommonResult<MesDvTpPlanItemRespVO> getTpPlanItem(@RequestParam("id") Long id) {
        MesDvTpPlanItemDO item = tpPlanItemService.getTpPlanItem(id);
        return success(BeanUtils.toBean(item, MesDvTpPlanItemRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得 TPM 计划项目列表")
    @Parameter(name = "planId", description = "TPM 计划编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('mes:dv-tp-plan:query')")
    public CommonResult<List<MesDvTpPlanItemRespVO>> getTpPlanItemList(@RequestParam("planId") Long planId) {
        List<MesDvTpPlanItemDO> list = tpPlanItemService.getTpPlanItemListByPlanId(planId);
        return success(BeanUtils.toBean(list, MesDvTpPlanItemRespVO.class));
    }

}
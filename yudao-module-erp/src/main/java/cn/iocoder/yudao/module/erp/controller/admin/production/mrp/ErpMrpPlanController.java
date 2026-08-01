package cn.iocoder.yudao.module.erp.controller.admin.production.mrp;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpResultPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mrp.vo.ErpMrpResultRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mrp.ErpMrpPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mrp.ErpMrpResultDO;
import cn.iocoder.yudao.module.erp.service.production.mrp.ErpMrpPlanService;
import cn.iocoder.yudao.module.erp.service.production.mrp.ErpMrpResultService;
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

@Tag(name = "管理后台 - ERP 物料需求计划")
@RestController
@RequestMapping("/erp/mrp-plan")
@Validated
public class ErpMrpPlanController {

    @Resource
    private ErpMrpPlanService mrpPlanService;
    @Resource
    private ErpMrpResultService mrpResultService;

    @PostMapping("/create")
    @Operation(summary = "创建 MRP 计划")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:create')")
    public CommonResult<Long> createMrpPlan(@Valid @RequestBody ErpMrpPlanSaveReqVO createReqVO) {
        return success(mrpPlanService.createMrpPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 MRP 计划")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:update')")
    public CommonResult<Boolean> updateMrpPlan(@Valid @RequestBody ErpMrpPlanSaveReqVO updateReqVO) {
        mrpPlanService.updateMrpPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除 MRP 计划")
    @Parameter(name = "ids", description = "编号数组", required = true)
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:delete')")
    public CommonResult<Boolean> deleteMrpPlan(@RequestParam("ids") List<Long> ids) {
        mrpPlanService.deleteMrpPlan(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得 MRP 计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:query')")
    public CommonResult<ErpMrpPlanRespVO> getMrpPlan(@RequestParam("id") Long id) {
        ErpMrpPlanDO plan = mrpPlanService.getMrpPlan(id);
        if (plan == null) {
            return success(null);
        }
        ErpMrpPlanRespVO respVO = BeanUtils.toBean(plan, ErpMrpPlanRespVO.class);
        // 查询结果
        List<ErpMrpResultDO> results = mrpResultService.getMrpResultListByPlanId(id);
        respVO.setResults(BeanUtils.toBean(results, ErpMrpResultRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得 MRP 计划分页")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:query')")
    public CommonResult<PageResult<ErpMrpPlanRespVO>> getMrpPlanPage(@Valid ErpMrpPlanPageReqVO pageReqVO) {
        PageResult<ErpMrpPlanDO> pageResult = mrpPlanService.getMrpPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpMrpPlanRespVO.class));
    }

    @PutMapping("/execute")
    @Operation(summary = "执行 MRP 计算")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:execute')")
    public CommonResult<Boolean> executeMrp(@RequestParam("id") Long id) {
        mrpPlanService.executeMrp(id);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认 MRP 计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:confirm')")
    public CommonResult<Boolean> confirmMrpPlan(@RequestParam("id") Long id) {
        mrpPlanService.confirmMrpPlan(id);
        return success(true);
    }

    @GetMapping("/result/page")
    @Operation(summary = "获得 MRP 结果分页")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:query')")
    public CommonResult<PageResult<ErpMrpResultRespVO>> getMrpResultPage(@Valid ErpMrpResultPageReqVO pageReqVO) {
        PageResult<ErpMrpResultDO> pageResult = mrpResultService.getMrpResultPageByPlanId(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpMrpResultRespVO.class));
    }

    @GetMapping("/result/list")
    @Operation(summary = "获得 MRP 结果列表")
    @Parameter(name = "planId", description = "MRP 计划编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:mrp-plan:query')")
    public CommonResult<List<ErpMrpResultRespVO>> getMrpResultList(@RequestParam("planId") Long planId) {
        List<ErpMrpResultDO> list = mrpResultService.getMrpResultListByPlanId(planId);
        return success(BeanUtils.toBean(list, ErpMrpResultRespVO.class));
    }

}

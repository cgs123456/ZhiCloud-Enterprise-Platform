package cn.zhicloud.module.erp.controller.admin.finance;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan.ErpFundPlanSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpFundPlanDO;
import cn.zhicloud.module.erp.service.finance.ErpFundPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 资金计划")
@RestController
@RequestMapping("/erp/fund-plan")
@Validated
public class ErpFundPlanController {

    @Resource
    private ErpFundPlanService fundPlanService;

    @PostMapping("/create")
    @Operation(summary = "创建资金计划")
    @PreAuthorize("@ss.hasPermission('erp:fund-plan:create')")
    public CommonResult<Long> createFundPlan(@Valid @RequestBody ErpFundPlanSaveReqVO createReqVO) {
        return success(fundPlanService.createFundPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新资金计划")
    @PreAuthorize("@ss.hasPermission('erp:fund-plan:update')")
    public CommonResult<Boolean> updateFundPlan(@Valid @RequestBody ErpFundPlanSaveReqVO updateReqVO) {
        fundPlanService.updateFundPlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除资金计划")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:fund-plan:delete')")
    public CommonResult<Boolean> deleteFundPlan(@RequestParam("id") Long id) {
        fundPlanService.deleteFundPlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得资金计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:fund-plan:query')")
    public CommonResult<ErpFundPlanRespVO> getFundPlan(@RequestParam("id") Long id) {
        ErpFundPlanDO fundPlan = fundPlanService.getFundPlan(id);
        return success(BeanUtils.toBean(fundPlan, ErpFundPlanRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得资金计划分页")
    @PreAuthorize("@ss.hasPermission('erp:fund-plan:query')")
    public CommonResult<PageResult<ErpFundPlanRespVO>> getFundPlanPage(@Valid ErpFundPlanPageReqVO pageReqVO) {
        PageResult<ErpFundPlanDO> pageResult = fundPlanService.getFundPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpFundPlanRespVO.class));
    }

    @GetMapping("/sum-by-period")
    @Operation(summary = "按期间汇总资金计划净额（收款-付款）")
    @Parameter(name = "planPeriod", description = "计划期间", required = true, example = "2026-07")
    @PreAuthorize("@ss.hasPermission('erp:fund-plan:query')")
    public CommonResult<BigDecimal> sumByPeriod(@RequestParam("planPeriod") String planPeriod) {
        return success(fundPlanService.sumByPeriod(planPeriod));
    }

}
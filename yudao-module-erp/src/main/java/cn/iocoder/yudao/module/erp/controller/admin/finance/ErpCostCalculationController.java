package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.finance.cost.ErpCostCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 成本核算")
@RestController
@RequestMapping("/erp/cost-calculation")
@Validated
public class ErpCostCalculationController {

    @Resource
    private ErpCostCalculationService costCalculationService;

    @PostMapping("/calculate-standard-cost")
    @Operation(summary = "执行标准成本卷积计算")
    @PreAuthorize("@ss.hasPermission('erp:cost-calculation:calculate')")
    public CommonResult<List<BigDecimal>> calculateStandardCost(
            @RequestParam("productId") Long productId,
            @RequestParam("costPeriod") String costPeriod) {
        return success(costCalculationService.calculateStandardCostByConvolution(productId, costPeriod));
    }

    @PostMapping("/collect-actual-cost")
    @Operation(summary = "归集工单成本到实际成本")
    @PreAuthorize("@ss.hasPermission('erp:cost-calculation:calculate')")
    public CommonResult<Integer> collectActualCost(
            @RequestParam("productId") Long productId,
            @RequestParam("costPeriod") String costPeriod) {
        return success(costCalculationService.collectActualCostFromWorkOrders(productId, costPeriod));
    }

    @PostMapping("/analyze-variance")
    @Operation(summary = "执行差异分析")
    @PreAuthorize("@ss.hasPermission('erp:cost-calculation:calculate')")
    public CommonResult<Integer> analyzeVariance(
            @RequestParam("productId") Long productId,
            @RequestParam("costPeriod") String costPeriod) {
        return success(costCalculationService.analyzeVariance(productId, costPeriod));
    }

}

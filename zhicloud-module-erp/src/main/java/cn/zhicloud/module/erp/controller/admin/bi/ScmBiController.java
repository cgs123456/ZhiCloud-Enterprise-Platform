package cn.zhicloud.module.erp.controller.admin.bi;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiAgingRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiInventoryTurnoverRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiOrderFulfillmentRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiPriceFluctuationRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiPurchaseOnTimeRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiSlowMovingRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiStockoutRespVO;
import cn.zhicloud.module.erp.controller.admin.bi.vo.ScmBiSupplierScoreRespVO;
import cn.zhicloud.module.erp.service.bi.inventory.ScmInventoryBiService;
import cn.zhicloud.module.erp.service.bi.purchase.ScmPurchaseBiService;
import cn.zhicloud.module.erp.service.bi.sale.ScmSaleBiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 供应链 BI")
@RestController
@RequestMapping("/erp/scm-bi")
@Validated
public class ScmBiController {

    @Resource
    private ScmInventoryBiService inventoryBiService;
    @Resource
    private ScmPurchaseBiService purchaseBiService;
    @Resource
    private ScmSaleBiService saleBiService;

    // ========== 库存 BI ==========

    @GetMapping("/inventory-turnover-rate")
    @Operation(summary = "获得库存周转率")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<ScmBiInventoryTurnoverRespVO> getInventoryTurnoverRate(
            @Parameter(description = "开始时间") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(inventoryBiService.getInventoryTurnoverRate(beginTime, endTime));
    }

    @GetMapping("/inventory-aging")
    @Operation(summary = "获得库龄分析")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<List<ScmBiAgingRespVO>> getInventoryAging() {
        return success(inventoryBiService.getInventoryAging());
    }

    @GetMapping("/slow-moving-inventory")
    @Operation(summary = "获得呆滞库存分析")
    @Parameter(name = "idleDays", description = "停滞天数阈值", example = "90")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<List<ScmBiSlowMovingRespVO>> getSlowMovingInventory(
            @RequestParam(value = "idleDays", required = false) Integer idleDays) {
        return success(inventoryBiService.getSlowMovingInventory(idleDays));
    }

    // ========== 采购 BI ==========

    @GetMapping("/purchase-on-time-rate")
    @Operation(summary = "获得采购到货及时率")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<ScmBiPurchaseOnTimeRespVO> getPurchaseOnTimeRate(
            @Parameter(description = "开始时间") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(purchaseBiService.getPurchaseOnTimeRate(beginTime, endTime));
    }

    @GetMapping("/price-fluctuation")
    @Operation(summary = "获得采购价格波动")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<List<ScmBiPriceFluctuationRespVO>> getPriceFluctuation(
            @Parameter(description = "开始时间") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(purchaseBiService.getPriceFluctuation(beginTime, endTime));
    }

    @GetMapping("/supplier-performance")
    @Operation(summary = "获得供应商绩效评分")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<List<ScmBiSupplierScoreRespVO>> getSupplierPerformance(
            @Parameter(description = "开始时间") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(purchaseBiService.getSupplierPerformance(beginTime, endTime));
    }

    // ========== 销售 BI ==========

    @GetMapping("/order-fulfillment-rate")
    @Operation(summary = "获得订单履约率")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<ScmBiOrderFulfillmentRespVO> getOrderFulfillmentRate(
            @Parameter(description = "开始时间") @RequestParam(value = "beginTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime beginTime,
            @Parameter(description = "结束时间") @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return success(saleBiService.getOrderFulfillmentRate(beginTime, endTime));
    }

    @GetMapping("/stockout-rate")
    @Operation(summary = "获得缺货率")
    @PreAuthorize("@ss.hasPermission('erp:scm-bi:query')")
    public CommonResult<ScmBiStockoutRespVO> getStockoutRate() {
        return success(saleBiService.getStockoutRate());
    }

}

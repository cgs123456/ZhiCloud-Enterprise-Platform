package cn.zhicloud.module.erp.controller.admin.sale.query;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.zhicloud.module.erp.query.sale.SaleOrderDetailVO;
import cn.zhicloud.module.erp.query.sale.SaleOrderQueryService;
import cn.zhicloud.module.erp.query.sale.SaleOrderStatisticsVO;
import cn.zhicloud.module.erp.query.sale.SaleOrderSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * 销售订单查询 Controller（CQRS 试点）
 *
 * <p>只读查询接口，与现有 {@code ErpSaleOrderController} 并存。
 * 演示 CQRS 读写分离：写侧走现有 Controller / Service，读侧走本 Controller / QueryService，
 * 读模型投影为独立 VO，便于针对查询场景优化（字段裁剪、聚合统计）。
 *
 * <p>权限复用现有销售订单查询权限，不新增权限标识。
 *
 * @author DDD 试点
 */
@Tag(name = "管理后台 - ERP 销售订单查询（CQRS 试点）")
@RestController
@RequestMapping("/erp/sale-order-ddd")
@Validated
public class SaleOrderQueryController {

    @Resource
    private SaleOrderQueryService saleOrderQueryService;

    @GetMapping("/summary")
    @Operation(summary = "分页查询销售订单摘要（CQRS 读模型）")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<PageResult<SaleOrderSummaryVO>> querySummary(@Valid ErpSaleOrderPageReqVO pageReqVO) {
        return success(saleOrderQueryService.querySummary(pageReqVO));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "查询销售订单详情（CQRS 读模型）")
    @Parameter(name = "id", description = "订单编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<SaleOrderDetailVO> queryDetail(@PathVariable("id") Long id) {
        return success(saleOrderQueryService.queryDetail(id));
    }

    @GetMapping("/statistics")
    @Operation(summary = "销售订单统计查询（CQRS 读模型）")
    @PreAuthorize("@ss.hasPermission('erp:sale-order:query')")
    public CommonResult<SaleOrderStatisticsVO> queryStatistics(
            @RequestParam(value = "start", required = false)
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate start,
            @RequestParam(value = "end", required = false)
            @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY) LocalDate end) {
        return success(saleOrderQueryService.queryStatistics(start, end));
    }

}

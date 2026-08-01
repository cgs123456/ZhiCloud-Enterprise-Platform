package cn.iocoder.yudao.module.ai.mcp.tools;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 模块 MCP Tool 暴露
 *
 * 设计要点：
 *  1. {@code @ConditionalOnBean(ErpStockService.class)} 仅当 ERP 模块加载时才注入；
 *  2. 不修改 erp 模块任何代码，仅在 ai 模块做包装；
 *  3. 每个 @Tool 方法第一个参数固定为 {@code Long tenantId}，通过 {@link McpToolContextHelper} 设置租户上下文；
 *  4. 返回简化 VO，不暴露 DO 中的 creator/updater/updateTime/tenantId/deleted 等敏感字段。
 *
 * 暴露三类查询：
 *  - 库存查询（按产品+仓库、按产品总数、分页）
 *  - 采购订单查询（按 ID、分页）
 *  - 销售订单查询（按 ID、分页）
 *
 * @author 芋道源码
 */
@Component
@ConditionalOnBean(ErpStockService.class)
@RequiredArgsConstructor
@Slf4j
public class ErpMcpTools extends TenantAwareMcpTool {

    private final ErpStockService erpStockService;
    private final ErpPurchaseOrderService erpPurchaseOrderService;
    private final ErpSaleOrderService erpSaleOrderService;

    // ==================== 库存查询 ====================

    @Tool(name = "erp_get_stock_by_product_and_warehouse",
            description = "按产品 + 仓库查询 ERP 库存（Get ERP stock by product id and warehouse id）。返回单条库存记录（含数量），不存在则返回 null。")
    @McpToolRequiresPermission("erp:stock:query")
    public ErpStockRespVO getStockByProductAndWarehouse(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "产品编号 / Product id") Long productId,
            @ToolParam(description = "仓库编号 / Warehouse id") Long warehouseId) {
        return executeInTenant(tenantId, () ->
                ErpStockRespVO.convert(erpStockService.getStock(productId, warehouseId)));
    }

    @Tool(name = "erp_get_stock_count_by_product",
            description = "查询产品所有仓库的 ERP 库存总量（Get total ERP stock count for a product across all warehouses）。无库存记录返回 0。")
    @McpToolRequiresPermission("erp:stock:query")
    public BigDecimal getStockCountByProduct(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "产品编号 / Product id") Long productId) {
        return executeInTenant(tenantId, () -> erpStockService.getStockCount(productId));
    }

    @Tool(name = "erp_get_stock_page",
            description = "分页查询 ERP 库存（Page query ERP stock records）。可按产品编号、仓库编号过滤。返回当前页库存列表。")
    @McpToolRequiresPermission("erp:stock:query")
    public ErpStockPageRespVO getStockPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "产品编号 / Product id", required = false) Long productId,
            @ToolParam(description = "仓库编号 / Warehouse id", required = false) Long warehouseId) {
        return executeInTenant(tenantId, () -> {
            ErpStockPageReqVO pageReqVO = new ErpStockPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setProductId(productId);
            pageReqVO.setWarehouseId(warehouseId);
            PageResult<ErpStockDO> page = erpStockService.getStockPage(pageReqVO);
            return ErpStockPageRespVO.convert(page);
        });
    }

    // ==================== 采购订单查询 ====================

    @Tool(name = "erp_get_purchase_order_by_id",
            description = "按编号查询 ERP 采购订单详情（Get ERP purchase order by id）。返回订单号、供应商、状态、金额等信息。")
    @McpToolRequiresPermission("erp:purchase-order:query")
    public ErpPurchaseOrderRespVO getPurchaseOrderById(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "采购订单编号 / Purchase order id") Long id) {
        return executeInTenant(tenantId, () ->
                ErpPurchaseOrderRespVO.convert(erpPurchaseOrderService.getPurchaseOrder(id)));
    }

    @Tool(name = "erp_get_purchase_order_page",
            description = "分页查询 ERP 采购订单列表（Page query ERP purchase orders）。可按订单号、供应商、采购状态、入库状态过滤。")
    @McpToolRequiresPermission("erp:purchase-order:query")
    public ErpPurchaseOrderPageRespVO getPurchaseOrderPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "采购单号 / Purchase order no", required = false) String no,
            @ToolParam(description = "供应商编号 / Supplier id", required = false) Long supplierId,
            @ToolParam(description = "采购状态：0=未审核 1=审核中 2=已审核 / Status",
                    required = false) Integer status,
            @ToolParam(description = "产品编号 / Product id", required = false) Long productId) {
        return executeInTenant(tenantId, () -> {
            ErpPurchaseOrderPageReqVO pageReqVO = new ErpPurchaseOrderPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setNo(no);
            pageReqVO.setSupplierId(supplierId);
            pageReqVO.setStatus(status);
            pageReqVO.setProductId(productId);
            PageResult<ErpPurchaseOrderDO> page = erpPurchaseOrderService.getPurchaseOrderPage(pageReqVO);
            return ErpPurchaseOrderPageRespVO.convert(page);
        });
    }

    // ==================== 销售订单查询 ====================

    @Tool(name = "erp_get_sale_order_by_id",
            description = "按编号查询 ERP 销售订单详情（Get ERP sale order by id）。返回订单号、客户、状态、金额等信息。")
    @McpToolRequiresPermission("erp:sale-order:query")
    public ErpSaleOrderRespVO getSaleOrderById(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "销售订单编号 / Sale order id") Long id) {
        return executeInTenant(tenantId, () ->
                ErpSaleOrderRespVO.convert(erpSaleOrderService.getSaleOrder(id)));
    }

    @Tool(name = "erp_get_sale_order_page",
            description = "分页查询 ERP 销售订单列表（Page query ERP sale orders）。可按订单号、客户、销售状态、出库状态过滤。")
    @McpToolRequiresPermission("erp:sale-order:query")
    public ErpSaleOrderPageRespVO getSaleOrderPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "销售单号 / Sale order no", required = false) String no,
            @ToolParam(description = "客户编号 / Customer id", required = false) Long customerId,
            @ToolParam(description = "销售状态：0=未审核 1=审核中 2=已审核 / Status",
                    required = false) Integer status,
            @ToolParam(description = "产品编号 / Product id", required = false) Long productId) {
        return executeInTenant(tenantId, () -> {
            ErpSaleOrderPageReqVO pageReqVO = new ErpSaleOrderPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setNo(no);
            pageReqVO.setCustomerId(customerId);
            pageReqVO.setStatus(status);
            pageReqVO.setProductId(productId);
            PageResult<ErpSaleOrderDO> page = erpSaleOrderService.getSaleOrderPage(pageReqVO);
            return ErpSaleOrderPageRespVO.convert(page);
        });
    }

    // ==================== 简化 VO ====================

    /**
     * ERP 库存简化 VO
     */
    @Data
    public static class ErpStockRespVO {
        private Long id;
        private Long productId;
        private Long warehouseId;
        private BigDecimal count;

        public static ErpStockRespVO convert(ErpStockDO src) {
            if (src == null) {
                return null;
            }
            ErpStockRespVO vo = new ErpStockRespVO();
            vo.setId(src.getId());
            vo.setProductId(src.getProductId());
            vo.setWarehouseId(src.getWarehouseId());
            vo.setCount(src.getCount());
            return vo;
        }

        public static List<ErpStockRespVO> convertList(List<ErpStockDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(ErpStockRespVO::convert).collect(Collectors.toList());
        }
    }

    @Data
    public static class ErpStockPageRespVO {
        private Long total;
        private List<ErpStockRespVO> list;

        public static ErpStockPageRespVO convert(PageResult<ErpStockDO> page) {
            ErpStockPageRespVO vo = new ErpStockPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(ErpStockRespVO.convertList(page.getList()));
            return vo;
        }
    }

    /**
     * ERP 采购订单简化 VO
     */
    @Data
    public static class ErpPurchaseOrderRespVO {
        private Long id;
        private String no;
        private Integer status;
        private Long supplierId;
        private Long accountId;
        private LocalDateTime orderTime;
        private BigDecimal totalCount;
        private BigDecimal totalPrice;
        private BigDecimal totalProductPrice;
        private BigDecimal totalTaxPrice;
        private BigDecimal discountPercent;
        private BigDecimal discountPrice;
        private BigDecimal depositPrice;
        private BigDecimal inCount;
        private BigDecimal returnCount;
        private String remark;

        public static ErpPurchaseOrderRespVO convert(ErpPurchaseOrderDO src) {
            if (src == null) {
                return null;
            }
            ErpPurchaseOrderRespVO vo = new ErpPurchaseOrderRespVO();
            vo.setId(src.getId());
            vo.setNo(src.getNo());
            vo.setStatus(src.getStatus());
            vo.setSupplierId(src.getSupplierId());
            vo.setAccountId(src.getAccountId());
            vo.setOrderTime(src.getOrderTime());
            vo.setTotalCount(src.getTotalCount());
            vo.setTotalPrice(src.getTotalPrice());
            vo.setTotalProductPrice(src.getTotalProductPrice());
            vo.setTotalTaxPrice(src.getTotalTaxPrice());
            vo.setDiscountPercent(src.getDiscountPercent());
            vo.setDiscountPrice(src.getDiscountPrice());
            vo.setDepositPrice(src.getDepositPrice());
            vo.setInCount(src.getInCount());
            vo.setReturnCount(src.getReturnCount());
            vo.setRemark(src.getRemark());
            return vo;
        }

        public static List<ErpPurchaseOrderRespVO> convertList(List<ErpPurchaseOrderDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(ErpPurchaseOrderRespVO::convert).collect(Collectors.toList());
        }
    }

    @Data
    public static class ErpPurchaseOrderPageRespVO {
        private Long total;
        private List<ErpPurchaseOrderRespVO> list;

        public static ErpPurchaseOrderPageRespVO convert(PageResult<ErpPurchaseOrderDO> page) {
            ErpPurchaseOrderPageRespVO vo = new ErpPurchaseOrderPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(ErpPurchaseOrderRespVO.convertList(page.getList()));
            return vo;
        }
    }

    /**
     * ERP 销售订单简化 VO
     */
    @Data
    public static class ErpSaleOrderRespVO {
        private Long id;
        private String no;
        private Integer status;
        private Long customerId;
        private Long accountId;
        private Long saleUserId;
        private LocalDateTime orderTime;
        private BigDecimal totalCount;
        private BigDecimal totalPrice;
        private BigDecimal totalProductPrice;
        private BigDecimal totalTaxPrice;
        private BigDecimal discountPercent;
        private BigDecimal discountPrice;
        private BigDecimal depositPrice;
        private BigDecimal outCount;
        private BigDecimal returnCount;
        private String remark;

        public static ErpSaleOrderRespVO convert(ErpSaleOrderDO src) {
            if (src == null) {
                return null;
            }
            ErpSaleOrderRespVO vo = new ErpSaleOrderRespVO();
            vo.setId(src.getId());
            vo.setNo(src.getNo());
            vo.setStatus(src.getStatus());
            vo.setCustomerId(src.getCustomerId());
            vo.setAccountId(src.getAccountId());
            vo.setSaleUserId(src.getSaleUserId());
            vo.setOrderTime(src.getOrderTime());
            vo.setTotalCount(src.getTotalCount());
            vo.setTotalPrice(src.getTotalPrice());
            vo.setTotalProductPrice(src.getTotalProductPrice());
            vo.setTotalTaxPrice(src.getTotalTaxPrice());
            vo.setDiscountPercent(src.getDiscountPercent());
            vo.setDiscountPrice(src.getDiscountPrice());
            vo.setDepositPrice(src.getDepositPrice());
            vo.setOutCount(src.getOutCount());
            vo.setReturnCount(src.getReturnCount());
            vo.setRemark(src.getRemark());
            return vo;
        }

        public static List<ErpSaleOrderRespVO> convertList(List<ErpSaleOrderDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(ErpSaleOrderRespVO::convert).collect(Collectors.toList());
        }
    }

    @Data
    public static class ErpSaleOrderPageRespVO {
        private Long total;
        private List<ErpSaleOrderRespVO> list;

        public static ErpSaleOrderPageRespVO convert(PageResult<ErpSaleOrderDO> page) {
            ErpSaleOrderPageRespVO vo = new ErpSaleOrderPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(ErpSaleOrderRespVO.convertList(page.getList()));
            return vo;
        }
    }

}

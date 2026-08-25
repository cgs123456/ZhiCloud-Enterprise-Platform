package cn.zhicloud.module.ai.mcp.tools;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.inventory.vo.WmsInventoryPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.service.inventory.WmsInventoryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WMS 模块 MCP Tool 暴露
 *
 * 设计要点：
 *  1. {@code @ConditionalOnBean(WmsInventoryService.class)} 仅当 WMS 模块加载时才注入；
 *  2. 不修改 wms 模块任何代码，仅在 ai 模块做包装；
 *  3. 每个 @Tool 方法第一个参数固定为 {@code Long tenantId}，通过 {@link McpToolContextHelper} 设置租户上下文；
 *  4. 返回简化 VO（{@link WmsInventoryRespVO}），不暴露 DO 中的 creator/updater/updateTime/tenantId/deleted 等敏感字段。
 *
 * @author 智云
 */
@Component
@ConditionalOnBean(WmsInventoryService.class)
@RequiredArgsConstructor
@Slf4j
public class WmsMcpTools extends TenantAwareMcpTool {

    private final WmsInventoryService wmsInventoryService;

    // ==================== 库存查询 ====================

    @Tool(name = "wms_get_inventory_count_by_sku",
            description = "查询指定 SKU 的当前库存数量（WMS inventory count by SKU id）。返回库存数量，无库存时为 0。")
    @McpToolRequiresPermission("wms:inventory:query")
    public long getInventoryCountBySku(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "商品 SKU 编号 / Inventory SKU id") Long skuId) {
        return executeInTenant(tenantId, () -> wmsInventoryService.getInventoryCountBySkuId(skuId));
    }

    @Tool(name = "wms_get_inventory_count_by_warehouse",
            description = "查询指定仓库的当前库存数量（WMS inventory count by warehouse id）。返回该仓库所有 SKU 的库存总和。")
    @McpToolRequiresPermission("wms:inventory:query")
    public long getInventoryCountByWarehouse(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "仓库编号 / Warehouse id") Long warehouseId) {
        return executeInTenant(tenantId, () -> wmsInventoryService.getInventoryCountByWarehouseId(warehouseId));
    }

    @Tool(name = "wms_get_inventory_page",
            description = "分页查询 WMS 库存列表（WMS inventory page query）。可按仓库维度（type=warehouse）或商品维度（type=item）统计，支持过滤 SKU、仓库名称、最小库存等条件。返回当前页库存记录列表。")
    @McpToolRequiresPermission("wms:inventory:query")
    public WmsInventoryPageRespVO getInventoryPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "统计维度：warehouse=按仓库维度 / item=按商品维度 / Aggregation type",
                    required = false) String type,
            @ToolParam(description = "商品名称（模糊匹配） / Item name (fuzzy)", required = false) String itemName,
            @ToolParam(description = "SKU 编号 / SKU id", required = false) Long skuId,
            @ToolParam(description = "仓库编号 / Warehouse id", required = false) Long warehouseId,
            @ToolParam(description = "是否只查询正库存 / Only positive quantity", required = false) Boolean onlyPositiveQuantity) {
        return executeInTenant(tenantId, () -> {
            WmsInventoryPageReqVO pageReqVO = new WmsInventoryPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setType(type == null ? WmsInventoryPageReqVO.TYPE_WAREHOUSE : type);
            pageReqVO.setItemName(itemName);
            pageReqVO.setSkuId(skuId);
            pageReqVO.setWarehouseId(warehouseId);
            pageReqVO.setOnlyPositiveQuantity(onlyPositiveQuantity);
            PageResult<WmsInventoryDO> page = wmsInventoryService.getInventoryPage(pageReqVO);
            return WmsInventoryPageRespVO.convert(page);
        });
    }

    // ==================== 简化 VO ====================

    /**
     * WMS 库存简化 VO
     */
    @Data
    public static class WmsInventoryRespVO {
        /**
         * 库存记录编号
         */
        private Long id;
        /**
         * 商品 SKU 编号
         */
        private Long skuId;
        /**
         * 仓库编号
         */
        private Long warehouseId;
        /**
         * 库存数量
         */
        private BigDecimal quantity;
        /**
         * 备注
         */
        private String remark;

        public static WmsInventoryRespVO convert(WmsInventoryDO src) {
            if (src == null) {
                return null;
            }
            WmsInventoryRespVO vo = new WmsInventoryRespVO();
            vo.setId(src.getId());
            vo.setSkuId(src.getSkuId());
            vo.setWarehouseId(src.getWarehouseId());
            vo.setQuantity(src.getQuantity());
            vo.setRemark(src.getRemark());
            return vo;
        }

        public static List<WmsInventoryRespVO> convertList(List<WmsInventoryDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(WmsInventoryRespVO::convert).collect(Collectors.toList());
        }
    }

    /**
     * WMS 库存分页结果 VO
     */
    @Data
    public static class WmsInventoryPageRespVO {
        /**
         * 总数
         */
        private Long total;
        /**
         * 当前页数据
         */
        private List<WmsInventoryRespVO> list;

        public static WmsInventoryPageRespVO convert(PageResult<WmsInventoryDO> page) {
            WmsInventoryPageRespVO vo = new WmsInventoryPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(WmsInventoryRespVO.convertList(page.getList()));
            return vo;
        }
    }

}

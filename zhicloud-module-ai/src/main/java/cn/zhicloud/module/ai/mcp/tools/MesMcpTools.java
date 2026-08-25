package cn.zhicloud.module.ai.mcp.tools;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.zhicloud.module.mes.service.wm.materialstock.MesWmMaterialStockService;
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
 * MES 模块 MCP Tool 暴露
 *
 * 设计要点：
 *  1. {@code @ConditionalOnBean(MesProWorkOrderService.class)} 仅当 MES 模块加载时才注入；
 *  2. 不修改 mes 模块任何代码，仅在 ai 模块做包装；
 *  3. 每个 @Tool 方法第一个参数固定为 {@code Long tenantId}，通过 {@link McpToolContextHelper} 设置租户上下文；
 *  4. 返回简化 VO，不暴露 DO 中的 creator/updater/updateTime/tenantId/deleted 等敏感字段。
 *
 * @author 智云
 */
@Component
@ConditionalOnBean(MesProWorkOrderService.class)
@RequiredArgsConstructor
@Slf4j
public class MesMcpTools extends TenantAwareMcpTool {

    private final MesProWorkOrderService mesProWorkOrderService;
    private final MesWmMaterialStockService mesWmMaterialStockService;

    // ==================== 工单查询 ====================

    @Tool(name = "mes_get_work_order_by_id",
            description = "按编号查询 MES 生产工单详情（Get MES production work order by id）。返回工单编码、产品、数量、状态等信息。")
    @McpToolRequiresPermission("mes:pro-work-order:query")
    public MesProWorkOrderRespVO getWorkOrderById(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "工单编号 / Work order id") Long id) {
        return executeInTenant(tenantId, () ->
                MesProWorkOrderRespVO.convert(mesProWorkOrderService.getWorkOrder(id)));
    }

    @Tool(name = "mes_get_work_order_by_code",
            description = "按编码查询 MES 生产工单详情（Get MES production work order by code）。返回工单编码、产品、数量、状态等信息。")
    @McpToolRequiresPermission("mes:pro-work-order:query")
    public MesProWorkOrderRespVO getWorkOrderByCode(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "工单编码 / Work order code, e.g. WO-001") String code) {
        return executeInTenant(tenantId, () ->
                MesProWorkOrderRespVO.convert(mesProWorkOrderService.getWorkOrder(code)));
    }

    @Tool(name = "mes_get_work_order_page",
            description = "分页查询 MES 生产工单列表（Page query MES production work orders）。可按工单编码、名称、类型、状态、产品编号过滤。返回当前页工单列表。")
    @McpToolRequiresPermission("mes:pro-work-order:query")
    public MesProWorkOrderPageRespVO getWorkOrderPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "工单编码 / Work order code", required = false) String code,
            @ToolParam(description = "工单名称 / Work order name", required = false) String name,
            @ToolParam(description = "工单类型 / Work order type", required = false) Integer type,
            @ToolParam(description = "产品编号 / Product id", required = false) Long productId,
            @ToolParam(description = "工单状态 / Work order status", required = false) Integer status) {
        return executeInTenant(tenantId, () -> {
            MesProWorkOrderPageReqVO pageReqVO = new MesProWorkOrderPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setCode(code);
            pageReqVO.setName(name);
            pageReqVO.setType(type);
            pageReqVO.setProductId(productId);
            pageReqVO.setStatus(status);
            PageResult<MesProWorkOrderDO> page = mesProWorkOrderService.getWorkOrderPage(pageReqVO);
            return MesProWorkOrderPageRespVO.convert(page);
        });
    }

    // ==================== 库存查询 ====================

    @Tool(name = "mes_get_material_stock_list_by_area",
            description = "查询指定库位的 MES 物料库存列表（Get MES material stock list by storage area id）。返回该库位下所有物料的库存台账记录。")
    @McpToolRequiresPermission("mes:wm-material-stock:query")
    public List<MesWmMaterialStockRespVO> getMaterialStockListByArea(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "库位编号 / Storage area id") Long areaId) {
        return executeInTenant(tenantId, () ->
                MesWmMaterialStockRespVO.convertList(mesWmMaterialStockService.getMaterialStockListByAreaId(areaId)));
    }

    // ==================== 简化 VO ====================

    /**
     * MES 生产工单简化 VO
     */
    @Data
    public static class MesProWorkOrderRespVO {
        private Long id;
        private String code;
        private String name;
        private Integer type;
        private Integer orderSourceType;
        private String orderSourceCode;
        private Long productId;
        private BigDecimal quantity;
        private BigDecimal quantityProduced;
        private BigDecimal quantityChanged;
        private BigDecimal quantityScheduled;
        private Long clientId;
        private Long vendorId;
        private String batchCode;
        private LocalDateTime requestDate;
        private Integer status;
        private String remark;

        public static MesProWorkOrderRespVO convert(MesProWorkOrderDO src) {
            if (src == null) {
                return null;
            }
            MesProWorkOrderRespVO vo = new MesProWorkOrderRespVO();
            vo.setId(src.getId());
            vo.setCode(src.getCode());
            vo.setName(src.getName());
            vo.setType(src.getType());
            vo.setOrderSourceType(src.getOrderSourceType());
            vo.setOrderSourceCode(src.getOrderSourceCode());
            vo.setProductId(src.getProductId());
            vo.setQuantity(src.getQuantity());
            vo.setQuantityProduced(src.getQuantityProduced());
            vo.setQuantityChanged(src.getQuantityChanged());
            vo.setQuantityScheduled(src.getQuantityScheduled());
            vo.setClientId(src.getClientId());
            vo.setVendorId(src.getVendorId());
            vo.setBatchCode(src.getBatchCode());
            vo.setRequestDate(src.getRequestDate());
            vo.setStatus(src.getStatus());
            vo.setRemark(src.getRemark());
            return vo;
        }

        public static List<MesProWorkOrderRespVO> convertList(List<MesProWorkOrderDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(MesProWorkOrderRespVO::convert).collect(Collectors.toList());
        }
    }

    /**
     * MES 生产工单分页结果 VO
     */
    @Data
    public static class MesProWorkOrderPageRespVO {
        private Long total;
        private List<MesProWorkOrderRespVO> list;

        public static MesProWorkOrderPageRespVO convert(PageResult<MesProWorkOrderDO> page) {
            MesProWorkOrderPageRespVO vo = new MesProWorkOrderPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(MesProWorkOrderRespVO.convertList(page.getList()));
            return vo;
        }
    }

    /**
     * MES 物料库存简化 VO
     */
    @Data
    public static class MesWmMaterialStockRespVO {
        private Long id;
        private Long itemTypeId;
        private Long itemId;
        private Long batchId;
        private String batchCode;
        private Long warehouseId;
        private Long locationId;
        private Long areaId;
        private Long vendorId;
        private BigDecimal quantity;
        private LocalDateTime receiptTime;
        private Boolean frozen;

        public static MesWmMaterialStockRespVO convert(MesWmMaterialStockDO src) {
            if (src == null) {
                return null;
            }
            MesWmMaterialStockRespVO vo = new MesWmMaterialStockRespVO();
            vo.setId(src.getId());
            vo.setItemTypeId(src.getItemTypeId());
            vo.setItemId(src.getItemId());
            vo.setBatchId(src.getBatchId());
            vo.setBatchCode(src.getBatchCode());
            vo.setWarehouseId(src.getWarehouseId());
            vo.setLocationId(src.getLocationId());
            vo.setAreaId(src.getAreaId());
            vo.setVendorId(src.getVendorId());
            vo.setQuantity(src.getQuantity());
            vo.setReceiptTime(src.getReceiptTime());
            vo.setFrozen(src.getFrozen());
            return vo;
        }

        public static List<MesWmMaterialStockRespVO> convertList(List<MesWmMaterialStockDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(MesWmMaterialStockRespVO::convert).collect(Collectors.toList());
        }
    }

}

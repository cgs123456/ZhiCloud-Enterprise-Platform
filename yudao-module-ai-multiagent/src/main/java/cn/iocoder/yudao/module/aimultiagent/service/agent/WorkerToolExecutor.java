package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.aimultiagent.config.MultiAgentProperties;
import cn.iocoder.yudao.module.aimultiagent.service.metrics.MultiAgentMetrics;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.iocoder.yudao.module.qms.service.inspectionorder.InspectionOrderService;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.vo.WmsInventoryPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.md.merchant.vo.WmsMerchantPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.receipt.vo.order.WmsReceiptOrderPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.order.shipment.vo.order.WmsShipmentOrderPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.receipt.WmsReceiptOrderDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.iocoder.yudao.module.wms.service.inventory.WmsInventoryService;
import cn.iocoder.yudao.module.wms.service.md.merchant.WmsMerchantService;
import cn.iocoder.yudao.module.wms.service.order.receipt.WmsReceiptOrderService;
import cn.iocoder.yudao.module.wms.service.order.shipment.WmsShipmentOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Worker 真实工具执行器
 *
 * <p>将 MultiAgent 中声明的工具名称（wms_* / qms_*）映射到 WMS / QMS 业务模块的<b>真实 Service 调用</b>，
 * 使 Worker 在 {@code execute()} 时真正拉取业务系统的真实数据，而不是只让 LLM 凭空生成文本。
 *
 * <p>每个工具调用都在指定租户上下文（{@link TenantContextHolder}）中执行，确保多租户数据隔离。
 * 工具名称与 {@code AbstractWorkerAgent#getSupportedTools()} 中声明的一致，由 Worker 按任务所需工具触发。
 *
 * @author 智云
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkerToolExecutor {

    private final WmsReceiptOrderService receiptOrderService;
    private final WmsMerchantService merchantService;
    private final WmsShipmentOrderService shipmentOrderService;
    private final WmsInventoryService inventoryService;
    private final InspectionOrderService inspectionOrderService;

    private final MultiAgentProperties properties;
    private final MultiAgentMetrics metrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 按工具名称执行真实业务调用，返回结构化数据快照（供 LLM 作为上下文）。
     *
     * @param toolName 工具名称（如 wms_receipt_order_list）
     * @param tenantId 租户编号
     * @return 真实数据快照（JSON 字符串）；不支持的工具返回说明
     */
    public String execute(String toolName, Long tenantId) {
        return execute(toolName, tenantId, properties.getWorker().getDefaultLimit());
    }

    public String execute(String toolName, Long tenantId, int limit) {
        // 限幅，避免异常大的分页拖垮业务库
        int safeLimit = Math.max(1, Math.min(limit, properties.getWorker().getMaxLimit()));
        // 使用 TenantUtils.execute 临时切换租户并在结束后还原，
        // 避免直接 clear() 摧毁外层（Web 请求）租户上下文导致后续 DB 操作越租户 / NPE。
        String data = TenantUtils.execute(tenantId, () -> doExecute(toolName, safeLimit, tenantId));
        // 工具命中 / 失败指标
        if (isToolFailure(data)) {
            metrics.recordToolFailure(toolName);
        } else {
            metrics.recordToolCall(toolName);
        }
        return data;
    }

    private String doExecute(String toolName, int limit, Long tenantId) {
        try {
            return switch (toolName) {
                case "wms_receipt_order_list" -> jsonify(listReceiptOrders(limit), "收货单列表");
                case "wms_merchant_list" -> jsonify(listMerchants(limit), "商户列表");
                case "wms_inventory_list" -> jsonify(listInventory(limit), "库存列表");
                case "wms_shipment_order_list" -> jsonify(listShipmentOrders(limit), "发货单列表");
                case "qms_inspection_order_list" -> jsonify(listInspectionOrders(limit), "质检单列表");
                default -> "（工具 " + toolName + " 暂无对应真实业务实现，已跳过真实数据拉取）";
            };
        } catch (Exception e) {
            log.warn("[WorkerToolExecutor][工具 {} 调用失败 tenantId={}]", toolName, tenantId, e);
            return "（工具 " + toolName + " 调用失败：" + e.getMessage() + "）";
        }
    }

    // ==================== 真实业务调用（WMS / QMS Service） ====================

    private List<WmsReceiptOrderDO> listReceiptOrders(int limit) {
        WmsReceiptOrderPageReqVO pageReqVO = new WmsReceiptOrderPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(limit);
        PageResult<WmsReceiptOrderDO> page = receiptOrderService.getReceiptOrderPage(pageReqVO);
        return page.getList();
    }

    private List<WmsMerchantDO> listMerchants(int limit) {
        WmsMerchantPageReqVO pageReqVO = new WmsMerchantPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(limit);
        PageResult<WmsMerchantDO> page = merchantService.getMerchantPage(pageReqVO);
        return page.getList();
    }

    private List<WmsInventoryDO> listInventory(int limit) {
        WmsInventoryPageReqVO pageReqVO = new WmsInventoryPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(limit);
        PageResult<WmsInventoryDO> page = inventoryService.getInventoryPage(pageReqVO);
        return page.getList();
    }

    private List<WmsShipmentOrderDO> listShipmentOrders(int limit) {
        WmsShipmentOrderPageReqVO pageReqVO = new WmsShipmentOrderPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(limit);
        PageResult<WmsShipmentOrderDO> page = shipmentOrderService.getShipmentOrderPage(pageReqVO);
        return page.getList();
    }

    private List<InspectionOrderDO> listInspectionOrders(int limit) {
        InspectionOrderPageReqVO pageReqVO = new InspectionOrderPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(limit);
        PageResult<InspectionOrderDO> page = inspectionOrderService.getInspectionOrderPage(pageReqVO);
        return page.getList();
    }

    private String jsonify(List<?> data, String title) {
        try {
            String json = objectMapper.writeValueAsString(data);
            return "【" + title + "（真实业务数据，共 " + (data == null ? 0 : data.size()) + " 条）】\n" + json;
        } catch (Exception e) {
            return "【" + title + "】" + (data == null ? "[]" : data.toString());
        }
    }

    /**
     * 判断工具返回内容是否为「失败 / 不支持」说明（而非真实数据快照）。
     */
    private static boolean isToolFailure(String data) {
        return data != null && (data.contains("调用失败") || data.contains("暂无对应真实业务实现"));
    }

}

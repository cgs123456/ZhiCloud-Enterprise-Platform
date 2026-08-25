package cn.zhicloud.module.ai.mcp.tools;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.capa.vo.CAPADocumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.capa.CAPADocumentDO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.enums.qms.CAPAStatusEnum;
import cn.zhicloud.module.qms.service.capa.CAPADocumentService;
import cn.zhicloud.module.qms.service.inspectionorder.InspectionOrderService;
import cn.zhicloud.module.qms.service.inspectionrecord.InspectionRecordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QMS 模块 MCP Tool 暴露
 *
 * 设计要点：
 *  1. {@code @ConditionalOnBean(InspectionOrderService.class)} 仅当 QMS 模块加载时才注入；
 *  2. 不修改 qms 模块任何代码，仅在 ai 模块做包装；
 *  3. 每个 @Tool 方法第一个参数固定为 {@code Long tenantId}，通过 {@link McpToolContextHelper} 设置租户上下文；
 *  4. 返回简化 VO（内部静态类），不暴露 DO 中的 creator/updater/updateTime/tenantId/deleted 等敏感字段。
 *
 * @author 智云
 */
@Component
@ConditionalOnBean(InspectionOrderService.class)
@RequiredArgsConstructor
@Slf4j
public class QmsMcpTools extends TenantAwareMcpTool {

    private final InspectionOrderService inspectionOrderService;
    private final CAPADocumentService capaDocumentService;
    private final InspectionRecordService inspectionRecordService;

    // ==================== 检验单查询 ====================

    @Tool(name = "qms_get_inspection_order_by_id",
            description = "按编号查询 QMS 检验单详情（QMS inspection order detail by id）。返回检验单的编号、单号、类型、状态、检验员等核心字段。")
    @McpToolRequiresPermission("qms:inspection-order:query")
    public InspectionOrderRespVO getInspectionOrderById(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "检验单编号 / Inspection order id") Long id) {
        return executeInTenant(tenantId, () ->
                InspectionOrderRespVO.convert(inspectionOrderService.getInspectionOrder(id)));
    }

    // ==================== CAPA 文档查询 ====================

    @Tool(name = "qms_get_capa_page",
            description = "分页查询 QMS CAPA 文档（QMS CAPA document page query）。可按 CAPA 单号、来源、状态、责任人等条件过滤。返回当前页 CAPA 文档列表。")
    @McpToolRequiresPermission("qms:capa:query")
    public CAPADocumentPageRespVO getCAPAPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "CAPA 单号（模糊匹配） / CAPA no (fuzzy)", required = false) String capaNo,
            @ToolParam(description = "来源 / Source", required = false) Integer source,
            @ToolParam(description = "状态 / Status", required = false) Integer status,
            @ToolParam(description = "责任人（模糊匹配） / Responsible person (fuzzy)", required = false) String responsiblePerson) {
        return executeInTenant(tenantId, () -> {
            CAPADocumentPageReqVO pageReqVO = new CAPADocumentPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setCapaNo(capaNo);
            pageReqVO.setSource(source);
            pageReqVO.setStatus(status);
            pageReqVO.setResponsiblePerson(responsiblePerson);
            PageResult<CAPADocumentDO> page = capaDocumentService.getCAPADocumentPage(pageReqVO);
            return CAPADocumentPageRespVO.convert(page);
        });
    }

    @Tool(name = "qms_list_unclosed_capa",
            description = "列出所有未关闭的 QMS CAPA 文档（List all unclosed QMS CAPA documents, status != CLOSED）。返回状态为待处理或处理中的 CAPA 列表，用于跟踪未结项。")
    @McpToolRequiresPermission("qms:capa:query")
    public List<CAPADocumentRespVO> listUnclosedCAPA(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId) {
        return executeInTenant(tenantId, () -> {
            List<CAPADocumentDO> result = new ArrayList<>();
            // 查询待处理（OPEN）状态
            CAPADocumentPageReqVO openReqVO = new CAPADocumentPageReqVO();
            openReqVO.setPageNo(1);
            openReqVO.setPageSize(200);
            openReqVO.setStatus(CAPAStatusEnum.OPEN.getStatus());
            PageResult<CAPADocumentDO> openPage = capaDocumentService.getCAPADocumentPage(openReqVO);
            if (openPage.getList() != null) {
                result.addAll(openPage.getList());
            }
            // 查询处理中（IN_PROGRESS）状态
            CAPADocumentPageReqVO inProgressReqVO = new CAPADocumentPageReqVO();
            inProgressReqVO.setPageNo(1);
            inProgressReqVO.setPageSize(200);
            inProgressReqVO.setStatus(CAPAStatusEnum.IN_PROGRESS.getStatus());
            PageResult<CAPADocumentDO> inProgressPage = capaDocumentService.getCAPADocumentPage(inProgressReqVO);
            if (inProgressPage.getList() != null) {
                result.addAll(inProgressPage.getList());
            }
            return CAPADocumentRespVO.convertList(result);
        });
    }

    // ==================== 检验记录查询 ====================

    @Tool(name = "qms_get_inspection_record_page",
            description = "分页查询 QMS 检验记录（QMS inspection record page query）。可按检验单 ID、检验项目 ID、检验结果、检验员等条件过滤。返回当前页检验记录列表。")
    @McpToolRequiresPermission("qms:inspection-record:query")
    public InspectionRecordPageRespVO getInspectionRecordPage(
            @ToolParam(description = "租户编号 / Tenant id") Long tenantId,
            @ToolParam(description = "页码，从 1 开始 / Page number, 1-based") Integer pageNo,
            @ToolParam(description = "每页条数，1-200 / Page size, 1-200") Integer pageSize,
            @ToolParam(description = "检验单 ID / Inspection order id", required = false) Long orderId,
            @ToolParam(description = "检验项目 ID / Inspection item id", required = false) Long itemId,
            @ToolParam(description = "检验结果 / Inspection result", required = false) Integer result,
            @ToolParam(description = "检验员（模糊匹配） / Inspector (fuzzy)", required = false) String inspector) {
        return executeInTenant(tenantId, () -> {
            InspectionRecordPageReqVO pageReqVO = new InspectionRecordPageReqVO();
            pageReqVO.setPageNo(pageNo == null ? 1 : pageNo);
            pageReqVO.setPageSize(pageSize == null ? 10 : pageSize);
            pageReqVO.setOrderId(orderId);
            pageReqVO.setItemId(itemId);
            pageReqVO.setResult(result);
            pageReqVO.setInspector(inspector);
            PageResult<InspectionRecordDO> page = inspectionRecordService.getInspectionRecordPage(pageReqVO);
            return InspectionRecordPageRespVO.convert(page);
        });
    }

    // ==================== 简化 VO ====================

    /**
     * QMS 检验单简化 VO
     */
    @Data
    public static class InspectionOrderRespVO {
        /**
         * 编号
         */
        private Long id;
        /**
         * 检验单号
         */
        private String orderNo;
        /**
         * 检验类型
         */
        private Integer type;
        /**
         * 供应商 ID
         */
        private Long supplierId;
        /**
         * 批次号
         */
        private String batchNo;
        /**
         * 工单 ID
         */
        private Long workOrderId;
        /**
         * 产品 ID
         */
        private Long productId;
        /**
         * 检验员
         */
        private String inspector;
        /**
         * 检验时间
         */
        private LocalDateTime inspectTime;
        /**
         * 状态
         */
        private Integer status;
        /**
         * 备注
         */
        private String remark;

        public static InspectionOrderRespVO convert(InspectionOrderDO src) {
            if (src == null) {
                return null;
            }
            InspectionOrderRespVO vo = new InspectionOrderRespVO();
            vo.setId(src.getId());
            vo.setOrderNo(src.getOrderNo());
            vo.setType(src.getType());
            vo.setSupplierId(src.getSupplierId());
            vo.setBatchNo(src.getBatchNo());
            vo.setWorkOrderId(src.getWorkOrderId());
            vo.setProductId(src.getProductId());
            vo.setInspector(src.getInspector());
            vo.setInspectTime(src.getInspectTime());
            vo.setStatus(src.getStatus());
            vo.setRemark(src.getRemark());
            return vo;
        }
    }

    /**
     * QMS CAPA 文档简化 VO
     */
    @Data
    public static class CAPADocumentRespVO {
        /**
         * 编号
         */
        private Long id;
        /**
         * CAPA 单号
         */
        private String capaNo;
        /**
         * 来源
         */
        private Integer source;
        /**
         * 问题描述
         */
        private String problem;
        /**
         * 原因
         */
        private String cause;
        /**
         * 根本原因分析
         */
        private String rootCauseAnalysis;
        /**
         * 纠正措施
         */
        private String correctiveAction;
        /**
         * 预防措施
         */
        private String preventiveAction;
        /**
         * 责任人
         */
        private String responsiblePerson;
        /**
         * 截止日期
         */
        private LocalDateTime dueDate;
        /**
         * 关闭日期
         */
        private LocalDateTime closeDate;
        /**
         * 状态
         */
        private Integer status;
        /**
         * 备注
         */
        private String remark;

        public static CAPADocumentRespVO convert(CAPADocumentDO src) {
            if (src == null) {
                return null;
            }
            CAPADocumentRespVO vo = new CAPADocumentRespVO();
            vo.setId(src.getId());
            vo.setCapaNo(src.getCapaNo());
            vo.setSource(src.getSource());
            vo.setProblem(src.getProblem());
            vo.setCause(src.getCause());
            vo.setRootCauseAnalysis(src.getRootCauseAnalysis());
            vo.setCorrectiveAction(src.getCorrectiveAction());
            vo.setPreventiveAction(src.getPreventiveAction());
            vo.setResponsiblePerson(src.getResponsiblePerson());
            vo.setDueDate(src.getDueDate());
            vo.setCloseDate(src.getCloseDate());
            vo.setStatus(src.getStatus());
            vo.setRemark(src.getRemark());
            return vo;
        }

        public static List<CAPADocumentRespVO> convertList(List<CAPADocumentDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(CAPADocumentRespVO::convert).collect(Collectors.toList());
        }
    }

    /**
     * QMS CAPA 文档分页结果 VO
     */
    @Data
    public static class CAPADocumentPageRespVO {
        /**
         * 总数
         */
        private Long total;
        /**
         * 当前页数据
         */
        private List<CAPADocumentRespVO> list;

        public static CAPADocumentPageRespVO convert(PageResult<CAPADocumentDO> page) {
            CAPADocumentPageRespVO vo = new CAPADocumentPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(CAPADocumentRespVO.convertList(page.getList()));
            return vo;
        }
    }

    /**
     * QMS 检验记录简化 VO
     */
    @Data
    public static class InspectionRecordRespVO {
        /**
         * 编号
         */
        private Long id;
        /**
         * 检验单 ID
         */
        private Long orderId;
        /**
         * 检验项目 ID
         */
        private Long itemId;
        /**
         * 实测值
         */
        private String measuredValue;
        /**
         * 检验结果
         */
        private Integer result;
        /**
         * 检验员
         */
        private String inspector;
        /**
         * 检验时间
         */
        private LocalDateTime inspectTime;
        /**
         * 备注
         */
        private String remark;

        public static InspectionRecordRespVO convert(InspectionRecordDO src) {
            if (src == null) {
                return null;
            }
            InspectionRecordRespVO vo = new InspectionRecordRespVO();
            vo.setId(src.getId());
            vo.setOrderId(src.getOrderId());
            vo.setItemId(src.getItemId());
            vo.setMeasuredValue(src.getMeasuredValue());
            vo.setResult(src.getResult());
            vo.setInspector(src.getInspector());
            vo.setInspectTime(src.getInspectTime());
            vo.setRemark(src.getRemark());
            return vo;
        }

        public static List<InspectionRecordRespVO> convertList(List<InspectionRecordDO> list) {
            if (list == null || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(InspectionRecordRespVO::convert).collect(Collectors.toList());
        }
    }

    /**
     * QMS 检验记录分页结果 VO
     */
    @Data
    public static class InspectionRecordPageRespVO {
        /**
         * 总数
         */
        private Long total;
        /**
         * 当前页数据
         */
        private List<InspectionRecordRespVO> list;

        public static InspectionRecordPageRespVO convert(PageResult<InspectionRecordDO> page) {
            InspectionRecordPageRespVO vo = new InspectionRecordPageRespVO();
            vo.setTotal(page.getTotal());
            vo.setList(InspectionRecordRespVO.convertList(page.getList()));
            return vo;
        }
    }

}

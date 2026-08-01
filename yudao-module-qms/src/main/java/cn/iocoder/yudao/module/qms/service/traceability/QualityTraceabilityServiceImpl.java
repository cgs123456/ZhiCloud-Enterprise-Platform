package cn.iocoder.yudao.module.qms.service.traceability;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.traceability.vo.QualityTraceNodeVO;
import cn.iocoder.yudao.module.qms.controller.admin.traceability.vo.QualityTraceabilityRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.QUALITY_TRACEABILITY_NO_DATA;

/**
 * QMS 质量追溯 Service 实现类
 *
 * <p>基于检验单（qms_inspection_order）构建追溯链，检验单串联了供应商、批次、工单、产品等关键信息：
 * <ul>
 *   <li>正向追溯（原料 → 成品）：原料批次 → IQC 检验单 → 工单 → FQC/OQC 检验单 → 成品批次</li>
 *   <li>反向追溯（成品 → 原料）：成品批次 → FQC/OQC 检验单 → 工单 → IQC 检验单 → 供应商来料</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QualityTraceabilityServiceImpl implements QualityTraceabilityService {

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Override
    public QualityTraceabilityRespVO forwardTrace(Long materialId, String batchNo) {
        QualityTraceabilityRespVO resp = new QualityTraceabilityRespVO();
        resp.setDirection("FORWARD");
        resp.setStartMaterialId(materialId);
        resp.setStartBatchNo(batchNo);

        // 1. 查询原料批次关联的检验单（IQC 来料检验）
        List<InspectionOrderDO> materialOrders = inspectionOrderMapper.selectList(
                new LambdaQueryWrapperX<InspectionOrderDO>()
                        .eqIfPresent(InspectionOrderDO::getBatchNo, batchNo)
                        .eqIfPresent(InspectionOrderDO::getProductId, materialId)
                        .eqIfPresent(InspectionOrderDO::getType, InspectionTypeEnum.IQC.getType())
                        .orderByDesc(InspectionOrderDO::getId));

        if (materialOrders.isEmpty()) {
            throw exception(QUALITY_TRACEABILITY_NO_DATA);
        }

        // 2. 以原料检验单为根，向下追溯工单产出的成品
        for (InspectionOrderDO materialOrder : materialOrders) {
            QualityTraceNodeVO materialNode = buildNode("MATERIAL", materialOrder);
            // 查询同工单下的成品检验单（FQC/OQC）
            if (materialOrder.getWorkOrderId() != null) {
                List<InspectionOrderDO> productOrders = inspectionOrderMapper.selectList(
                        new LambdaQueryWrapperX<InspectionOrderDO>()
                                .eq(InspectionOrderDO::getWorkOrderId, materialOrder.getWorkOrderId())
                                .in(InspectionOrderDO::getType,
                                        InspectionTypeEnum.IPQC.getType(),
                                        InspectionTypeEnum.OQC.getType())
                                .orderByDesc(InspectionOrderDO::getId));
                for (InspectionOrderDO productOrder : productOrders) {
                    QualityTraceNodeVO productNode = buildNode("PRODUCT", productOrder);
                    materialNode.getChildren().add(productNode);
                }
            }
            resp.getTraceChain().add(materialNode);
        }
        return resp;
    }

    @Override
    public QualityTraceabilityRespVO backwardTrace(Long productId, String batchNo) {
        QualityTraceabilityRespVO resp = new QualityTraceabilityRespVO();
        resp.setDirection("BACKWARD");
        resp.setStartMaterialId(productId);
        resp.setStartBatchNo(batchNo);

        // 1. 查询成品批次关联的检验单（FQC/OQC）
        List<InspectionOrderDO> productOrders = inspectionOrderMapper.selectList(
                new LambdaQueryWrapperX<InspectionOrderDO>()
                        .eqIfPresent(InspectionOrderDO::getBatchNo, batchNo)
                        .eqIfPresent(InspectionOrderDO::getProductId, productId)
                        .in(InspectionOrderDO::getType,
                                InspectionTypeEnum.IPQC.getType(),
                                InspectionTypeEnum.OQC.getType())
                        .orderByDesc(InspectionOrderDO::getId));

        if (productOrders.isEmpty()) {
            throw exception(QUALITY_TRACEABILITY_NO_DATA);
        }

        // 2. 以成品检验单为根，向上追溯工单领用的原料批次
        for (InspectionOrderDO productOrder : productOrders) {
            QualityTraceNodeVO productNode = buildNode("PRODUCT", productOrder);
            // 查询同工单下的原料检验单（IQC）
            if (productOrder.getWorkOrderId() != null) {
                List<InspectionOrderDO> materialOrders = inspectionOrderMapper.selectList(
                        new LambdaQueryWrapperX<InspectionOrderDO>()
                                .eq(InspectionOrderDO::getWorkOrderId, productOrder.getWorkOrderId())
                                .eq(InspectionOrderDO::getType, InspectionTypeEnum.IQC.getType())
                                .orderByDesc(InspectionOrderDO::getId));
                for (InspectionOrderDO materialOrder : materialOrders) {
                    QualityTraceNodeVO materialNode = buildNode("MATERIAL", materialOrder);
                    productNode.getChildren().add(materialNode);
                }
            }
            resp.getTraceChain().add(productNode);
        }
        return resp;
    }

    /**
     * 根据检验单构建追溯节点
     */
    private QualityTraceNodeVO buildNode(String nodeType, InspectionOrderDO order) {
        QualityTraceNodeVO node = new QualityTraceNodeVO();
        node.setNodeType(nodeType);
        node.setId(order.getId());
        node.setBatchNo(order.getBatchNo());
        node.setMaterialId(order.getProductId());
        node.setSupplierId(order.getSupplierId());
        node.setWorkOrderId(order.getWorkOrderId());
        node.setInspectionOrderId(order.getId());
        node.setInspectionResult(order.getStatus());
        node.setCreateTime(order.getCreateTime());
        node.setChildren(new ArrayList<>());
        return node;
    }

}

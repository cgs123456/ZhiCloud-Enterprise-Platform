package cn.zhicloud.module.qms.service.traceability;

import cn.zhicloud.module.qms.controller.admin.traceability.vo.QualityTraceabilityRespVO;

/**
 * QMS 质量追溯 Service 接口
 *
 * @author 智云
 */
public interface QualityTraceabilityService {

    /**
     * 正向追溯（原料 → 成品）
     *
     * <p>追溯链：原料批次 → 关联生产工单 → 工单产出成品批次 → 成品出库记录。
     *
     * @param materialId 物料 ID（可选）
     * @param batchNo    批次号
     * @return 追溯结果（含追溯链树形结构）
     */
    QualityTraceabilityRespVO forwardTrace(Long materialId, String batchNo);

    /**
     * 反向追溯（成品 → 原料）
     *
     * <p>追溯链：成品批次 → 关联生产工单 → 工单领用原料批次 → 供应商来料检验记录。
     *
     * @param productId 产品 ID（可选）
     * @param batchNo   批次号
     * @return 追溯结果（含追溯链树形结构）
     */
    QualityTraceabilityRespVO backwardTrace(Long productId, String batchNo);

}

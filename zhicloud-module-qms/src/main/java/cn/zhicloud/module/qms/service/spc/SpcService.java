package cn.zhicloud.module.qms.service.spc;

import cn.zhicloud.module.qms.controller.admin.spc.vo.SamplingPlanRespVO;
import cn.zhicloud.module.qms.controller.admin.spc.vo.SpcAnalysisRespVO;

import java.math.BigDecimal;

/**
 * SPC 统计过程控制 Service 接口
 *
 * <p>提供基于检验记录的工序能力分析（Cp/Cpk）和抽样方案查询（MIL-STD-105E / GB 2828）。
 *
 * @author zhicloud
 */
public interface SpcService {

    /**
     * 基于检验项目 ID 计算工序能力分析
     *
     * <p>从 {@code qms_inspection_record} 表按 itemId 拉取所有 measuredValue，
     * 计算 mean / stdDev / Cp / Cpk / 控制限 / 超限样本数。
     *
     * @param itemId 检验项目 ID（必须配置 upperLimit / lowerLimit）
     * @return SPC 分析结果
     */
    SpcAnalysisRespVO analyze(Long itemId);

    /**
     * 查询 MIL-STD-105E 抽样方案
     *
     * <p>按批量大小、检验水平、AQL 查询正常/加严/放宽三种检验的 Ac/Re 与样本量。
     *
     * @param lotSize 批量大小
     * @param inspectionLevel 检验水平（S1~S4 / I / II / III，默认 II）
     * @param aql 接收质量限（0.065~1000，默认 1.0）
     * @return 抽样方案
     */
    SamplingPlanRespVO getSamplingPlan(Long lotSize, String inspectionLevel, BigDecimal aql);

}

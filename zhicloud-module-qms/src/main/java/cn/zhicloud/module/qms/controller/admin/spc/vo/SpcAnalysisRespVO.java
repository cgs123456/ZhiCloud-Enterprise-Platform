package cn.zhicloud.module.qms.controller.admin.spc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SPC 分析结果 VO
 *
 * <p>基于检验记录计算的统计过程控制指标。
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - SPC 分析结果 Response VO")
@Data
public class SpcAnalysisRespVO {

    @Schema(description = "检验项目 ID")
    private Long itemId;

    @Schema(description = "样本数量")
    private int sampleCount;

    @Schema(description = "样本均值")
    private BigDecimal mean;

    @Schema(description = "样本标准差（n-1 分母，Bessel 校正）")
    private BigDecimal stdDev;

    @Schema(description = "规格上限（USL）")
    private BigDecimal upperSpecLimit;

    @Schema(description = "规格下限（LSL）")
    private BigDecimal lowerSpecLimit;

    @Schema(description = "目标值")
    private BigDecimal target;

    @Schema(description = "控制上限 UCL = mean + 3*stdDev")
    private BigDecimal upperControlLimit;

    @Schema(description = "控制下限 LCL = mean - 3*stdDev")
    private BigDecimal lowerControlLimit;

    @Schema(description = "工序能力指数 Cp = (USL - LSL) / (6 * stdDev)")
    private BigDecimal cp;

    @Schema(description = "工序能力指数 Cpk = min((USL-mean)/(3*stdDev), (mean-LSL)/(3*stdDev))")
    private BigDecimal cpk;

    @Schema(description = "工序能力评价：>1.67 优秀；1.33~1.67 充足；1.0~1.33 勉强；<1.0 不足")
    private String capabilityLevel;

    @Schema(description = "超出控制限的样本数")
    private int outOfControlCount;

    @Schema(description = "样本值列表（按检验时间升序，用于绘制控制图）")
    private List<BigDecimal> samples;

    // ========== P0-9 Western Electric 8 规则 + 控制图类型 ==========

    @Schema(description = "控制图类型（XBAR_R / XBAR_S / I_MR / P / NP / C / U），默认 I_MR")
    private String chartType;

    @Schema(description = "Western Electric 8 规则违反记录列表")
    private List<SpcRuleViolationVO> ruleViolations;

}

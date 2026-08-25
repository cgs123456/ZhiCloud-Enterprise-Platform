package cn.zhicloud.module.qms.controller.admin.qualitycost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - QMS 质量成本汇总 Response VO（PAIF 四类）")
@Data
public class QualityCostSummaryRespVO {

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024")
    private Integer periodYear;

    @Schema(description = "月份（1-12）", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    private Integer periodMonth;

    @Schema(description = "预防成本金额", example = "50000.0000")
    private BigDecimal preventionAmount;

    @Schema(description = "鉴定成本金额", example = "30000.0000")
    private BigDecimal appraisalAmount;

    @Schema(description = "内部故障成本金额", example = "12000.0000")
    private BigDecimal internalFailureAmount;

    @Schema(description = "外部故障成本金额", example = "8000.0000")
    private BigDecimal externalFailureAmount;

    @Schema(description = "总质量成本", example = "100000.0000")
    private BigDecimal totalAmount;

    @Schema(description = "预防成本占比（百分比，如 50.00）", example = "50.00")
    private BigDecimal preventionRatio;

    @Schema(description = "鉴定成本占比（百分比）", example = "30.00")
    private BigDecimal appraisalRatio;

    @Schema(description = "内部故障成本占比（百分比）", example = "12.00")
    private BigDecimal internalFailureRatio;

    @Schema(description = "外部故障成本占比（百分比）", example = "8.00")
    private BigDecimal externalFailureRatio;

}
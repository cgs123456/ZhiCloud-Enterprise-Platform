package cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - QMS 质量成本年度趋势 Response VO")
@Data
public class QualityCostTrendRespVO {

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024")
    private Integer periodYear;

    @Schema(description = "月度趋势列表")
    private List<MonthlyTrendItem> items;

    @Schema(description = "月度趋势项")
    @Data
    public static class MonthlyTrendItem {

        @Schema(description = "月份（1-12）", example = "1")
        private Integer periodMonth;

        @Schema(description = "预防成本金额", example = "5000.0000")
        private BigDecimal preventionAmount;

        @Schema(description = "鉴定成本金额", example = "3000.0000")
        private BigDecimal appraisalAmount;

        @Schema(description = "内部故障成本金额", example = "1200.0000")
        private BigDecimal internalFailureAmount;

        @Schema(description = "外部故障成本金额", example = "800.0000")
        private BigDecimal externalFailureAmount;

        @Schema(description = "当月总质量成本", example = "10000.0000")
        private BigDecimal totalAmount;
    }

}
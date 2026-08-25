package cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 供应商评估新增/修改 Request VO")
@Data
public class ErpSupplierEvaluationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "评估周期 yyyyMM", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotEmpty(message = "评估周期不能为空")
    private String evaluationPeriod;

    @Schema(description = "质量评分", example = "90.00")
    private BigDecimal qualityScore;

    @Schema(description = "交期评分", example = "85.00")
    private BigDecimal deliveryScore;

    @Schema(description = "价格评分", example = "80.00")
    private BigDecimal priceScore;

    @Schema(description = "服务评分", example = "88.00")
    private BigDecimal serviceScore;

    @Schema(description = "综合评分", example = "86.00")
    private BigDecimal totalScore;

    @Schema(description = "等级 A/B/C/D", example = "A")
    private String grade;

    @Schema(description = "评估人", example = "张三")
    private String evaluator;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "评估指标项列表")
    private List<Item> items;

    @Schema(description = "评估指标项")
    @Data
    public static class Item {

        @Schema(description = "编号", example = "1024")
        private Long id;

        @Schema(description = "评估编号", example = "1")
        private Long evaluationId;

        @Schema(description = "指标名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "到货及时率")
        @NotEmpty(message = "指标名称不能为空")
        private String indicator;

        @Schema(description = "得分", example = "90.00")
        private BigDecimal score;

        @Schema(description = "权重", example = "30.00")
        private BigDecimal weight;

        @Schema(description = "加权得分", example = "27.00")
        private BigDecimal weightedScore;

        @Schema(description = "备注", example = "备注")
        private String remark;

    }

}

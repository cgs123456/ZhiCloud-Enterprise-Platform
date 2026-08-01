package cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 主生产计划生成 Request VO")
@Data
public class ErpMpsPlanGenerateReqVO {

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "计划周期（yyyyMM）", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotEmpty(message = "计划周期不能为空")
    private String planPeriod;

    @Schema(description = "安全库存（为空则默认 0）", example = "50.00")
    private BigDecimal safetyStock;

    @Schema(description = "提前期（天，为空则默认 7）", example = "7")
    private Integer leadTimeDays;

    @Schema(description = "预测数量（为空则默认 0）", example = "100.00")
    private BigDecimal forecastQuantity;

}
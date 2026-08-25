package cn.zhicloud.module.erp.controller.admin.production.mps.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 主生产计划新增/修改 Request VO")
@Data
public class ErpMpsPlanSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MPS-001")
    @NotEmpty(message = "计划编号不能为空")
    private String planNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "计划周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotEmpty(message = "计划周期不能为空")
    private String planPeriod;

    @Schema(description = "计划类型", example = "10")
    private Integer planType;

    @Schema(description = "需求日期")
    private LocalDate demandDate;

    @Schema(description = "计划数量", example = "100.00")
    private BigDecimal plannedQuantity;

    @Schema(description = "计划完工日期")
    private LocalDate plannedFinishDate;

    @Schema(description = "来源", example = "10")
    private Integer source;

    @Schema(description = "来源订单编号", example = "1")
    private Long sourceOrderId;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
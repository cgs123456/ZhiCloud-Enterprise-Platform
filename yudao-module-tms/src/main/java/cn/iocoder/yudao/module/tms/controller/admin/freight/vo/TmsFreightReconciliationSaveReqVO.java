package cn.iocoder.yudao.module.tms.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - TMS 运费对账新增/修改 Request VO")
@Data
public class TmsFreightReconciliationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "对账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FR001")
    @NotBlank(message = "对账单号不能为空")
    private String no;

    @Schema(description = "承运商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "承运商不能为空")
    private Long carrierId;

    @Schema(description = "对账周期开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08-01")
    @NotNull(message = "对账周期开始日期不能为空")
    private LocalDate periodStart;

    @Schema(description = "对账周期结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-08-31")
    @NotNull(message = "对账周期结束日期不能为空")
    private LocalDate periodEnd;

    @Schema(description = "系统运费总额", example = "15000.00")
    private BigDecimal systemAmount;

    @Schema(description = "承运商账单金额", example = "15200.00")
    private BigDecimal carrierAmount;

    @Schema(description = "备注", example = "首次对账")
    private String remark;

}

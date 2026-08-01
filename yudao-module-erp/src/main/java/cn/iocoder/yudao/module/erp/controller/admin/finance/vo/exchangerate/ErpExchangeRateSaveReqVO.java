package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.exchangerate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 汇率新增/修改 Request VO")
@Data
public class ErpExchangeRateSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "源币种编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "源币种编号不能为空")
    private Long fromCurrencyId;

    @Schema(description = "目标币种编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "目标币种编号不能为空")
    private Long toCurrencyId;

    @Schema(description = "汇率", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.14")
    @NotNull(message = "汇率不能为空")
    private BigDecimal rate;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-01")
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "备注", example = "Q3 汇率")
    private String remark;

}

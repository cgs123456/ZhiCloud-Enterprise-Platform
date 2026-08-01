package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.taxrate;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.tax.ErpTaxRateTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 税率新增/修改 Request VO")
@Data
public class ErpTaxRateSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "税率编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "VAT13")
    @NotBlank(message = "税率编码不能为空")
    private String code;

    @Schema(description = "税率名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "增值税 13%")
    @NotBlank(message = "税率名称不能为空")
    private String name;

    @Schema(description = "税率类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "税率类型不能为空")
    @InEnum(ErpTaxRateTypeEnum.class)
    private Integer rateType;

    @Schema(description = "税率（0~1）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.13")
    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率必须大于等于 0")
    @DecimalMax(value = "1", message = "税率必须小于等于 1")
    private BigDecimal rate;

    @Schema(description = "是否默认（0 否 1 是）", example = "1")
    private Integer isDefault;

    @Schema(description = "生效日期", example = "2026-01-01")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}

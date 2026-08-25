package cn.zhicloud.module.erp.controller.admin.finance.vo.taxrate;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 税率 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpTaxRateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "税率编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "VAT13")
    @ExcelProperty("税率编码")
    private String code;

    @Schema(description = "税率名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "增值税 13%")
    @ExcelProperty("税率名称")
    private String name;

    @Schema(description = "税率类型", example = "10")
    @ExcelProperty("税率类型")
    private Integer rateType;

    @Schema(description = "税率", example = "0.13")
    @ExcelProperty("税率")
    private BigDecimal rate;

    @Schema(description = "是否默认", example = "1")
    @ExcelProperty("是否默认")
    private Integer isDefault;

    @Schema(description = "生效日期", example = "2026-01-01")
    @ExcelProperty("生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    @ExcelProperty("失效日期")
    private LocalDate expiryDate;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态", example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

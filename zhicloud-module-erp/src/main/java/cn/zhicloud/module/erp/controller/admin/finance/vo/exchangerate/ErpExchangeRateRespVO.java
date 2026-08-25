package cn.zhicloud.module.erp.controller.admin.finance.vo.exchangerate;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 汇率 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpExchangeRateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "源币种编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("源币种编号")
    private Long fromCurrencyId;

    @Schema(description = "目标币种编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("目标币种编号")
    private Long toCurrencyId;

    @Schema(description = "汇率", requiredMode = Schema.RequiredMode.REQUIRED, example = "0.14")
    @ExcelProperty("汇率")
    private BigDecimal rate;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-07-01")
    @ExcelProperty("生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    @ExcelProperty("失效日期")
    private LocalDate expiryDate;

    @Schema(description = "备注", example = "Q3 汇率")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

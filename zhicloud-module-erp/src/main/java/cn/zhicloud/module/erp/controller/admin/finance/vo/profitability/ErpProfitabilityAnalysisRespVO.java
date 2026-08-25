package cn.zhicloud.module.erp.controller.admin.finance.vo.profitability;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 获利能力分析 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpProfitabilityAnalysisRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "利润中心编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("利润中心编号")
    private Long profitCenterId;

    @Schema(description = "会计期间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("会计期间编号")
    private Long periodId;

    @Schema(description = "收入", example = "100000.00")
    @ExcelProperty("收入")
    private BigDecimal revenue;

    @Schema(description = "成本", example = "60000.00")
    @ExcelProperty("成本")
    private BigDecimal cost;

    @Schema(description = "利润", example = "40000.00")
    @ExcelProperty("利润")
    private BigDecimal profit;

    @Schema(description = "利润率", example = "0.4000")
    @ExcelProperty("利润率")
    private BigDecimal profitMargin;

    @Schema(description = "备注", example = "Q3 获利分析")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

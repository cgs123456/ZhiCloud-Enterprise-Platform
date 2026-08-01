package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costvariance;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 成本差异 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCostVarianceRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "产品 ID", example = "1")
    @ExcelProperty("产品 ID")
    private Long productId;

    @Schema(description = "成本期间", example = "202607")
    @ExcelProperty("成本期间")
    private String costPeriod;

    @Schema(description = "成本项目 ID", example = "1")
    @ExcelProperty("成本项目 ID")
    private Long costItemId;

    @Schema(description = "标准成本", example = "100.00")
    @ExcelProperty("标准成本")
    private BigDecimal standardCost;

    @Schema(description = "实际成本", example = "110.00")
    @ExcelProperty("实际成本")
    private BigDecimal actualCost;

    @Schema(description = "差异金额", example = "10.00")
    @ExcelProperty("差异金额")
    private BigDecimal varianceAmount;

    @Schema(description = "差异率(%)", example = "10.00")
    @ExcelProperty("差异率(%)")
    private BigDecimal varianceRate;

    @Schema(description = "差异类型", example = "20")
    @ExcelProperty("差异类型")
    private Integer varianceType;

    @Schema(description = "分析说明")
    @ExcelProperty("分析说明")
    private String analysisRemark;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

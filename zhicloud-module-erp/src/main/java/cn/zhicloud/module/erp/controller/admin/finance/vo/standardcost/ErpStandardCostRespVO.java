package cn.zhicloud.module.erp.controller.admin.finance.vo.standardcost;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 标准成本 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStandardCostRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "产品 ID", example = "1")
    @ExcelProperty("产品 ID")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    @ExcelProperty("产品编码")
    private String productCode;

    @Schema(description = "产品名称", example = "产品A")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "成本项目 ID", example = "1")
    @ExcelProperty("成本项目 ID")
    private Long costItemId;

    @Schema(description = "标准成本", example = "100.00")
    @ExcelProperty("标准成本")
    private BigDecimal standardCost;

    @Schema(description = "生效日期", example = "2026-01-01")
    @ExcelProperty("生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2026-12-31")
    @ExcelProperty("失效日期")
    private LocalDate expiryDate;

    @Schema(description = "状态", example = "20")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

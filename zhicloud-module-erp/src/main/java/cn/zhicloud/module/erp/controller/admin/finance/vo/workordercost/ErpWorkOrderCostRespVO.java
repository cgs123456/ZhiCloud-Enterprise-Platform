package cn.zhicloud.module.erp.controller.admin.finance.vo.workordercost;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 工单成本归集 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpWorkOrderCostRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "工单 ID", example = "1")
    @ExcelProperty("工单 ID")
    private Long workOrderId;

    @Schema(description = "工单编码", example = "WO2026070001")
    @ExcelProperty("工单编码")
    private String workOrderCode;

    @Schema(description = "产品 ID", example = "1")
    @ExcelProperty("产品 ID")
    private Long productId;

    @Schema(description = "成本期间", example = "202607")
    @ExcelProperty("成本期间")
    private String costPeriod;

    @Schema(description = "材料成本", example = "500.00")
    @ExcelProperty("材料成本")
    private BigDecimal materialCost;

    @Schema(description = "人工成本", example = "200.00")
    @ExcelProperty("人工成本")
    private BigDecimal laborCost;

    @Schema(description = "制造费用", example = "150.00")
    @ExcelProperty("制造费用")
    private BigDecimal overheadCost;

    @Schema(description = "外协成本", example = "100.00")
    @ExcelProperty("外协成本")
    private BigDecimal outsourcingCost;

    @Schema(description = "总成本", example = "950.00")
    @ExcelProperty("总成本")
    private BigDecimal totalCost;

    @Schema(description = "工单产量", example = "100")
    @ExcelProperty("工单产量")
    private BigDecimal quantity;

    @Schema(description = "单位成本", example = "9.50")
    @ExcelProperty("单位成本")
    private BigDecimal unitCost;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

package cn.zhicloud.module.erp.controller.admin.stock.vo.stockbatch;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 库存批次 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockBatchRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "B2026070001")
    @ExcelProperty("批次号")
    private String batchNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("产品编号")
    private Long productId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("仓库编号")
    private Long warehouseId;

    @Schema(description = "生产日期", example = "2026-07-01")
    @ExcelProperty("生产日期")
    private LocalDate productionDate;

    @Schema(description = "过期日期", example = "2027-07-01")
    @ExcelProperty("过期日期")
    private LocalDate expiryDate;

    @Schema(description = "批次数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("批次数量")
    private BigDecimal quantity;

    @Schema(description = "批次状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("批次状态")
    private Integer status;

    @Schema(description = "备注", example = "首批")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

package cn.zhicloud.module.erp.controller.admin.stock.vo.stockserial;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 库存序列号 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockSerialRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "序列号", requiredMode = Schema.RequiredMode.REQUIRED, example = "SN2026070001")
    @ExcelProperty("序列号")
    private String serialNo;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("产品编号")
    private Long productId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("仓库编号")
    private Long warehouseId;

    @Schema(description = "批次编号", example = "1")
    @ExcelProperty("批次编号")
    private Long batchId;

    @Schema(description = "序列号状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("序列号状态")
    private Integer status;

    @Schema(description = "备注", example = "高价值产品")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

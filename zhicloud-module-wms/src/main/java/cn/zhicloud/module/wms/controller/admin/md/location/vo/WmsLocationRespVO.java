package cn.zhicloud.module.wms.controller.admin.md.location.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 库位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class WmsLocationRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "库区 ID", example = "1024")
    @ExcelProperty("库区 ID")
    private Long zoneId;

    @Schema(description = "仓库 ID", example = "1024")
    @ExcelProperty("仓库 ID")
    private Long warehouseId;

    @Schema(description = "库位编号", example = "L001")
    @ExcelProperty("库位编号")
    private String code;

    @Schema(description = "库位名称", example = "A-01-01")
    @ExcelProperty("库位名称")
    private String name;

    @Schema(description = "库位条码", example = "100001")
    @ExcelProperty("库位条码")
    private String barcode;

    @Schema(description = "库位类型", example = "10")
    @ExcelProperty("库位类型")
    private Integer type;

    @Schema(description = "承重容量（kg）", example = "1000.00")
    @ExcelProperty("承重容量")
    private BigDecimal capacityWeight;

    @Schema(description = "容积容量（m³）", example = "10.00")
    @ExcelProperty("容积容量")
    private BigDecimal capacityVolume;

    @Schema(description = "状态", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}

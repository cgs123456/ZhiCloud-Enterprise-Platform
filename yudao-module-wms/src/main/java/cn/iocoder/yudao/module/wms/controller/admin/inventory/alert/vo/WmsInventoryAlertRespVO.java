package cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 库存预警 Response VO")
@Data
public class WmsInventoryAlertRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "预警类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOW_STOCK")
    private String alertType;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long productId;

    @Schema(description = "SKU 编码", example = "SKU001")
    private String skuCode;

    @Schema(description = "SKU 名称", example = "红色S码")
    private String skuName;

    @Schema(description = "批次号", example = "BATCH202605110001")
    private String batchNo;

    @Schema(description = "当前库存", example = "10.00")
    private BigDecimal currentQuantity;

    @Schema(description = "阈值", example = "100.00")
    private BigDecimal thresholdValue;

    @Schema(description = "预警时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime alertTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
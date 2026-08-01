package cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WMS 批次效期预警 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 批次效期预警 Response VO")
@Data
public class WmsBatchExpiryAlertRespVO {

    @Schema(description = "预警编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "预警类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "NEAR_EXPIRY")
    private String alertType;

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "华东仓")
    private String warehouseName;

    @Schema(description = "商品 SKU 编号", example = "2048")
    private Long productId;

    @Schema(description = "SKU 编码", example = "SKU001")
    private String skuCode;

    @Schema(description = "SKU 名称", example = "可口可乐 500ml")
    private String skuName;

    @Schema(description = "批次号", example = "BATCH202605110001")
    private String batchNo;

    @Schema(description = "供应商批次号", example = "SUP-BATCH-001")
    private String supplierBatchNo;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    @Schema(description = "保质期天数", example = "365")
    private Integer shelfLifeDays;

    @Schema(description = "当前库存数量", example = "100.00")
    private BigDecimal currentQuantity;

    @Schema(description = "阈值", example = "30")
    private BigDecimal thresholdValue;

    @Schema(description = "预警时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime alertTime;

    @Schema(description = "状态（0 未处理 1 已确认 2 已处理）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "批次即将过期")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

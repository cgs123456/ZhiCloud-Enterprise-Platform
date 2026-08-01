package cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * WMS 库存批次 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 库存批次 Response VO")
@Data
public class WmsInventoryBatchRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "库存编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long inventoryId;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BATCH202605110001")
    private String batchNo;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    @Schema(description = "保质期天数", example = "365")
    private Integer shelfLifeDays;

    @Schema(description = "供应商批次号", example = "SUP-BATCH-202605110001")
    private String supplierBatchNo;

    @Schema(description = "批次数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal quantity;

    @Schema(description = "锁定数量", example = "0.00")
    private BigDecimal lockedQuantity;

    @Schema(description = "可用数量", example = "100.00")
    private BigDecimal availableQuantity;

    @Schema(description = "批次状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVAILABLE")
    private String status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

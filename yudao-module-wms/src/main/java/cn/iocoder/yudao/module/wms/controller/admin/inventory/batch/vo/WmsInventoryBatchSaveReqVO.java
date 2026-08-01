package cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.wms.enums.inventory.WmsInventoryBatchStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WMS 库存批次保存 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 库存批次保存 Request VO")
@Data
public class WmsInventoryBatchSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "库存编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "库存编号不能为空")
    private Long inventoryId;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BATCH202605110001")
    @NotBlank(message = "批次号不能为空")
    @Size(max = 64, message = "批次号长度不能超过 64 个字符")
    private String batchNo;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "过期日期")
    private LocalDate expiryDate;

    @Schema(description = "保质期天数", example = "365")
    private Integer shelfLifeDays;

    @Schema(description = "供应商批次号", example = "SUP-BATCH-202605110001")
    @Size(max = 64, message = "供应商批次号长度不能超过 64 个字符")
    private String supplierBatchNo;

    @Schema(description = "批次数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "批次数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "锁定数量", example = "0.00")
    private BigDecimal lockedQuantity;

    @Schema(description = "批次状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "AVAILABLE")
    @InEnum(WmsInventoryBatchStatusEnum.class)
    private String status;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}

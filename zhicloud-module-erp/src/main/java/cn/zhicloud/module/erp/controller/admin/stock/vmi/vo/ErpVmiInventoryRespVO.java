package cn.zhicloud.module.erp.controller.admin.stock.vmi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP VMI 库存 Response VO")
@Data
public class ErpVmiInventoryRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long supplierId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "当前库存数量", example = "100")
    private BigDecimal quantity;

    @Schema(description = "可用库存数量", example = "90")
    private BigDecimal availableQuantity;

    @Schema(description = "锁定库存数量", example = "10")
    private BigDecimal lockedQuantity;

    @Schema(description = "最低库存", example = "20")
    private BigDecimal minQuantity;

    @Schema(description = "最高库存", example = "200")
    private BigDecimal maxQuantity;

    @Schema(description = "补货点", example = "30")
    private BigDecimal replenishmentPoint;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

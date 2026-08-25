package cn.zhicloud.module.erp.controller.admin.stock.vmi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP VMI 库存新增/修改 Request VO")
@Data
public class ErpVmiInventorySaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "产品编号不能为空")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "当前库存数量", example = "100")
    private BigDecimal quantity;

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

}

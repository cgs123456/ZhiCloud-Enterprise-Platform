package cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 安全库存配置保存 Request VO")
@Data
public class WmsSafetyStockConfigSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "商品 SKU 不能为空")
    private Long productId;

    @Schema(description = "安全库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "安全库存不能为空")
    private BigDecimal safetyStock;

    @Schema(description = "最高库存", example = "500.00")
    private BigDecimal maxStock;

    @Schema(description = "最低库存", example = "50.00")
    private BigDecimal minStock;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
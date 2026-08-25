package cn.zhicloud.module.wms.controller.admin.inventory.alert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 安全库存配置 Response VO")
@Data
public class WmsSafetyStockConfigRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long productId;

    @Schema(description = "SKU 编码", example = "SKU001")
    private String skuCode;

    @Schema(description = "SKU 名称", example = "红色S码")
    private String skuName;

    @Schema(description = "安全库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal safetyStock;

    @Schema(description = "最高库存", example = "500.00")
    private BigDecimal maxStock;

    @Schema(description = "最低库存", example = "50.00")
    private BigDecimal minStock;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
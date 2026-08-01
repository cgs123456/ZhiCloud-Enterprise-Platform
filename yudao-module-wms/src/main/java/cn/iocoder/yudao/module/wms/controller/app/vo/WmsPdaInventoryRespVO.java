package cn.iocoder.yudao.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "用户 App - PDA 库存查询 Response VO")
@Data
public class WmsPdaInventoryRespVO {

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "SKU 编号", example = "2048")
    private Long skuId;

    @Schema(description = "SKU 编码", example = "SKU001")
    private String skuCode;

    @Schema(description = "SKU 名称", example = "10kg 箱装")
    private String skuName;

    @Schema(description = "商品名称", example = "红富士苹果")
    private String itemName;

    @Schema(description = "单位", example = "箱")
    private String unit;

    @Schema(description = "库存列表")
    private List<InventoryItem> inventories;

    @Schema(description = "库存明细项")
    @Data
    public static class InventoryItem {

        @Schema(description = "库存编号", example = "1024")
        private Long inventoryId;

        @Schema(description = "库存数量", example = "100.00")
        private BigDecimal quantity;

        @Schema(description = "可用数量", example = "80.00")
        private BigDecimal availableQuantity;

        @Schema(description = "库位编号", example = "1024")
        private Long locationId;

    }

}

package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * PDA 扫码响应 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 扫码 Response VO")
@Data
public class WmsPdaScanRespVO {

    @Schema(description = "扫码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOCATION")
    private String scanType;

    @Schema(description = "扫码内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOC001")
    private String scanCode;

    @Schema(description = "是否匹配成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean success;

    // ========== 库位扫码结果 ==========

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;
    @Schema(description = "仓库名称", example = "成品仓")
    private String warehouseName;
    @Schema(description = "库存列表（库位扫码时返回）")
    private List<InventoryItem> inventories;

    // ========== 物料扫码结果 ==========

    @Schema(description = "商品 SKU 编号", example = "4096")
    private Long skuId;
    @Schema(description = "规格编号", example = "SKU001")
    private String skuCode;
    @Schema(description = "规格名称", example = "10kg 箱装")
    private String skuName;
    @Schema(description = "商品编号", example = "8192")
    private Long itemId;
    @Schema(description = "商品编码", example = "ITEM001")
    private String itemCode;
    @Schema(description = "商品名称", example = "红富士苹果")
    private String itemName;
    @Schema(description = "单位", example = "箱")
    private String unit;
    @Schema(description = "条码", example = "6900000000001")
    private String barCode;

    /**
     * 库存项（库位扫码时返回的库存明细）
     */
    @Schema(description = "库存明细项")
    @Data
    public static class InventoryItem {

        @Schema(description = "库存编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private Long inventoryId;

        @Schema(description = "商品 SKU 编号", example = "4096")
        private Long skuId;
        @Schema(description = "规格编号", example = "SKU001")
        private String skuCode;
        @Schema(description = "规格名称", example = "10kg 箱装")
        private String skuName;
        @Schema(description = "商品名称", example = "红富士苹果")
        private String itemName;
        @Schema(description = "单位", example = "箱")
        private String unit;

        @Schema(description = "库存数量", example = "100.00")
        private BigDecimal quantity;
        @Schema(description = "可用数量", example = "80.00")
        private BigDecimal availableQuantity;

    }

}

package cn.iocoder.yudao.module.wms.controller.admin.pickpath.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * WMS 拣货路径库位节点 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - WMS 拣货路径库位节点 Response VO")
@Data
public class WmsPickPathLocationRespVO {

    @Schema(description = "访问顺序（从 1 开始）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sequence;

    @Schema(description = "库存编号（库位）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long inventoryId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long warehouseId;
    @Schema(description = "仓库名称", example = "成品仓")
    private String warehouseName;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
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

    @Schema(description = "需拣数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    private BigDecimal pickQuantity;
    @Schema(description = "库存可用数量", example = "100.00")
    private BigDecimal availableQuantity;

    @Schema(description = "库位坐标 X（伪坐标，用于路径计算）", example = "1")
    private Integer coordX;
    @Schema(description = "库位坐标 Y（伪坐标，用于路径计算）", example = "2")
    private Integer coordY;

}

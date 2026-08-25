package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PDA 盘点录入请求 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 盘点录入 Request VO")
@Data
public class WmsPdaCheckReqVO {

    @Schema(description = "盘库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "盘库单编号不能为空")
    private Long checkOrderId;

    @Schema(description = "库存编号", example = "2048")
    private Long inventoryId;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "商品 SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "8192")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "账面数量", example = "100.00")
    private BigDecimal bookQuantity;

    @Schema(description = "实盘数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "98.00")
    @NotNull(message = "实盘数量不能为空")
    private BigDecimal checkQuantity;

    @Schema(description = "单价", example = "10.00")
    private BigDecimal price;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}

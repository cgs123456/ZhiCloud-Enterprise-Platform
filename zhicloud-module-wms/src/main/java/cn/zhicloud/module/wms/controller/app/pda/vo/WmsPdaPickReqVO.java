package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PDA 拣货执行请求 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 拣货执行 Request VO")
@Data
public class WmsPdaPickReqVO {

    @Schema(description = "出库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出库单编号不能为空")
    private Long shipmentOrderId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "商品 SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "拣货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    @NotNull(message = "拣货数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "批次号（可选，指定批次出库）", example = "BATCH202605110001")
    @Size(max = 64, message = "批次号长度不能超过 64 个字符")
    private String batchNo;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}

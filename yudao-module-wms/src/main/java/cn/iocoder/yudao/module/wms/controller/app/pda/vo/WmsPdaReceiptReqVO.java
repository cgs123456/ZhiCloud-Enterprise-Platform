package cn.iocoder.yudao.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * PDA 收货确认请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "用户 App - PDA 收货确认 Request VO")
@Data
public class WmsPdaReceiptReqVO {

    @Schema(description = "入库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "入库单编号不能为空")
    private Long receiptOrderId;

    @Schema(description = "商品 SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "商品 SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "收货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "收货数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "批次号", example = "BATCH202605110001")
    @Size(max = 64, message = "批次号长度不能超过 64 个字符")
    private String batchNo;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}

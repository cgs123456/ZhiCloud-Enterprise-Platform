package cn.iocoder.yudao.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 App - PDA 确认收货 Request VO")
@Data
public class WmsPdaConfirmReceiptReqVO {

    @Schema(description = "ASN 单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "ASN 单编号不能为空")
    private Long asnOrderId;

    @Schema(description = "ASN 明细编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "ASN 明细编号不能为空")
    private Long detailId;

    @Schema(description = "收货数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "收货数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "收货数量必须大于 0")
    private BigDecimal receivedQuantity;

    @Schema(description = "库位编号（上架库位）", example = "1024")
    private Long locationId;

}

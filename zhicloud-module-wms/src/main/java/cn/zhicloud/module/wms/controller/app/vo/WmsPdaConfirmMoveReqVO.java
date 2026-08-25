package cn.zhicloud.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 App - PDA 确认移库 Request VO")
@Data
public class WmsPdaConfirmMoveReqVO {

    @Schema(description = "来源库位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "来源库位编号不能为空")
    private Long fromLocationId;

    @Schema(description = "目标库位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "目标库位编号不能为空")
    private Long toLocationId;

    @Schema(description = "SKU 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "SKU 编号不能为空")
    private Long skuId;

    @Schema(description = "移库数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "移库数量不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "移库数量必须大于 0")
    private BigDecimal quantity;

}

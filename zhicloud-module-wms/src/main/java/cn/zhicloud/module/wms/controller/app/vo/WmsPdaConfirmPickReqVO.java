package cn.zhicloud.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "用户 App - PDA 确认拣货 Request VO")
@Data
public class WmsPdaConfirmPickReqVO {

    @Schema(description = "拣货任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "拣货任务编号不能为空")
    private Long taskId;

    @Schema(description = "已拣数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "已拣数量不能为空")
    @DecimalMin(value = "0", message = "已拣数量不能小于 0")
    private BigDecimal pickedQuantity;

}

package cn.zhicloud.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "用户 App - PDA 确认打包 Request VO")
@Data
public class WmsPdaConfirmPackReqVO {

    @Schema(description = "出库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出库单编号不能为空")
    private Long shipmentOrderId;

}

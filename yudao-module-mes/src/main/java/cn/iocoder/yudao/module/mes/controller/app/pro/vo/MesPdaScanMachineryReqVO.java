package cn.iocoder.yudao.module.mes.controller.app.pro.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "用户 APP - PDA 扫描设备 Request VO")
@Data
public class MesPdaScanMachineryReqVO {

    @Schema(description = "设备编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC-001")
    @NotEmpty(message = "设备编码不能为空")
    private String machineryCode;

}

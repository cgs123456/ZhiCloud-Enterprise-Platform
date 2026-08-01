package cn.iocoder.yudao.module.wms.controller.app.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "用户 App - PDA 扫描 ASN Request VO")
@Data
public class WmsPdaScanAsnReqVO {

    @Schema(description = "ASN 条码（ASN 编号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "ASN202605110001")
    @NotEmpty(message = "ASN 条码不能为空")
    private String scanCode;

}

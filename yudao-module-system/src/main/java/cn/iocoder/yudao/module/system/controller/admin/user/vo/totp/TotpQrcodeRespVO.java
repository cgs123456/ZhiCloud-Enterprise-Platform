package cn.iocoder.yudao.module.system.controller.admin.user.vo.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - TOTP 二维码 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpQrcodeRespVO {

    @Schema(description = "otpauth URI（用于生成二维码）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "otpauth://totp/yudao:admin?secret=JBSWY3DPEHPK3PXP&issuer=yudao")
    private String otpauthUrl;

}

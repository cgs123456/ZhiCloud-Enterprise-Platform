package cn.zhicloud.module.system.controller.admin.user.vo.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - TOTP 绑定 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpEnableRespVO {

    @Schema(description = "TOTP 密钥（明文，仅此一次返回，前端可手动输入）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "JBSWY3DPEHPK3PXP")
    private String secret;

    @Schema(description = "otpauth URI（用于生成二维码）", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "otpauth://totp/zhicloud:admin?secret=JBSWY3DPEHPK3PXP&issuer=zhicloud")
    private String otpauthUrl;

}

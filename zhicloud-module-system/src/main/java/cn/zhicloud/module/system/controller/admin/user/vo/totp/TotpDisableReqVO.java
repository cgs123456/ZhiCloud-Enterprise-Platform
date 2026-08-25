package cn.zhicloud.module.system.controller.admin.user.vo.totp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 解绑 TOTP Request VO")
@Data
public class TotpDisableReqVO {

    @Schema(description = "TOTP 验证码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotEmpty(message = "TOTP 验证码不能为空")
    @Length(min = 6, max = 8, message = "TOTP 验证码长度为 6-8 位")
    private String totpCode;

}

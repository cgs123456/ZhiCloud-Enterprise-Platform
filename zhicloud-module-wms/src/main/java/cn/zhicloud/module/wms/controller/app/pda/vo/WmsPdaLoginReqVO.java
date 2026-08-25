package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * PDA 登录请求 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 登录 Request VO")
@Data
public class WmsPdaLoginReqVO {

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhicloudyuanma")
    @NotEmpty(message = "登录账号不能为空")
    @Size(min = 4, max = 30, message = "账号长度为 4-30 位")
    private String username;

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "buzhidao")
    @NotEmpty(message = "密码不能为空")
    @Size(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

}

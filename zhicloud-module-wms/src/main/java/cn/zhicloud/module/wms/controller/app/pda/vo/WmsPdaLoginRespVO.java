package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * PDA 登录响应 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 登录 Response VO")
@Data
public class WmsPdaLoginRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long userId;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "zhicloudyuanma")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "智云")
    private String nickname;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "访问令牌", example = "happy")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "nice")
    private String refreshToken;

}

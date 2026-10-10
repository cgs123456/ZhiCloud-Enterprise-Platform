package cn.zhicloud.module.fms.controller.admin.config.vo.accountuser;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账套成员权限修改 Request VO")
@Data
public class FmsAccountUserUpdateMemberReqVO {

    @Schema(description = "后台用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "成员权限级别：1=主管 2=查看者 3=会计", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成员权限级别不能为空")
    private Integer level;

}

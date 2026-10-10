package cn.zhicloud.module.fms.controller.admin.config.vo.accountuser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 账套成员 Response VO")
@Data
public class FmsAccountUserRespVO {

    @Schema(description = "后台用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "用户昵称")
    private String nickname;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "用户邮箱")
    private String email;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "是否默认账套", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean defaultStatus;

    @Schema(description = "是否账套创建人", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean founder;

    @Schema(description = "成员权限级别：1=主管 2=查看者 3=会计", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer level;

}

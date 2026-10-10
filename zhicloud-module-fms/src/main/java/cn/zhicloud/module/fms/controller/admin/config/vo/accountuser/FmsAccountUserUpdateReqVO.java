package cn.zhicloud.module.fms.controller.admin.config.vo.accountuser;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - FMS 账套成员修改 Request VO")
@Data
public class FmsAccountUserUpdateReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "账套成员数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotNull(message = "账套成员数组不能为空")
    private List<FmsAccountUserUpdateMemberReqVO> members;

}

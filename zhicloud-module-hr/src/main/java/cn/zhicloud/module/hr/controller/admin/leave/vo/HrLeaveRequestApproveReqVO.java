package cn.zhicloud.module.hr.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 请假单审批 Request VO")
@Data
public class HrLeaveRequestApproveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "是否批准 true批准 false驳回", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    @Schema(description = "审批备注", example = "同意")
    private String approveRemark;

}
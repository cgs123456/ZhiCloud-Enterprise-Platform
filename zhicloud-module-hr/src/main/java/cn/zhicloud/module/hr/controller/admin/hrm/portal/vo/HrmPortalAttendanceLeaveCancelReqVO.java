package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 员工自助端 - 请假申请取消 Request VO
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 请假申请取消 Request VO")
@Data
public class HrmPortalAttendanceLeaveCancelReqVO {

    @Schema(description = "请假记录编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请假记录编号不能为空")
    private Long id;

    @Schema(description = "取消原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "取消原因不能为空")
    private String reason;

}

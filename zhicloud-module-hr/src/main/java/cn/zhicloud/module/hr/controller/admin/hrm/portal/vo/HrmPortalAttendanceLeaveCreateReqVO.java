package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工自助端 - 请假申请创建 Request VO
 *
 * 对齐前端 HrmAttendanceLeaveCreateVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 请假申请创建 Request VO")
@Data
public class HrmPortalAttendanceLeaveCreateReqVO {

    @Schema(description = "请假类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请假类型不能为空")
    private String type;

    @Schema(description = "请假开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请假开始时间不能为空")
    private LocalDateTime startTime;

    @Schema(description = "请假结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请假结束时间不能为空")
    private LocalDateTime endTime;

    @Schema(description = "请假天数", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "请假天数不能为空")
    private BigDecimal day;

    @Schema(description = "请假事由")
    private String reason;

    @Schema(description = "备注")
    private String remark;

}

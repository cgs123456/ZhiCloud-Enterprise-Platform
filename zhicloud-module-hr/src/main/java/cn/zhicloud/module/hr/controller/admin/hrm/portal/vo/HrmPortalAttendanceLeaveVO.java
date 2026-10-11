package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 员工自助端 - 请假申请 VO
 *
 * 对齐前端 api/hrm/portal/attendance/leave HrmAttendanceLeaveVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 请假申请 VO")
@Data
public class HrmPortalAttendanceLeaveVO {

    @Schema(description = "请假记录编号")
    private Long id;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "请假类型")
    private String type;

    @Schema(description = "请假开始时间")
    private LocalDateTime startTime;

    @Schema(description = "请假结束时间")
    private LocalDateTime endTime;

    @Schema(description = "请假天数")
    private BigDecimal day;

    @Schema(description = "请假事由")
    private String reason;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "审批状态：1审批中 2审批通过 3审批不通过 4已取消")
    private Integer approvalStatus;

    @Schema(description = "流程实例编号")
    private String processInstanceId;

    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    @Schema(description = "审批意见")
    private String approvalReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

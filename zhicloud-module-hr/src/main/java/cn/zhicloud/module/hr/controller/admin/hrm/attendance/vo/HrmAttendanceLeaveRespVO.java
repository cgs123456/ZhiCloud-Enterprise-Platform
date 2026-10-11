package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 考勤请假 Response VO")
@Data
public class HrmAttendanceLeaveRespVO {

    @Schema(description = "请假记录编号")
    private Long id;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "职位名称")
    private String postName;

    @Schema(description = "请假类型（hrm_attendance_leave_type 字典 value）")
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

    @Schema(description = "审批状态")
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

package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 每日考勤明细 Response VO")
@Data
public class HrmAttendanceDailyDetailRespVO {

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

    @Schema(description = "考勤时间")
    private LocalDateTime attendanceTime;

    @Schema(description = "班次名称")
    private String shiftName;

    @Schema(description = "是否排班")
    private Boolean scheduled;

    @Schema(description = "应打卡次数")
    private Integer requiredClockCount;

    @Schema(description = "应出勤分钟数")
    private Integer scheduledMinutes;

    @Schema(description = "缺卡次数")
    private Integer misscardCount;

    @Schema(description = "是否旷工")
    private Boolean absenteeism;

    @Schema(description = "旷工分钟数")
    private Integer absenteeismMinutes;

    @Schema(description = "旷工天数")
    private BigDecimal absenteeismDays;

    @Schema(description = "是否请假")
    private Boolean leaveStatus;

    @Schema(description = "请假分钟数")
    private Integer leaveMinutes;

    @Schema(description = "请假天数")
    private BigDecimal leaveDays;

    @Schema(description = "考勤结果")
    private String attendanceResult;

    @Schema(description = "迟到次数")
    private Integer lateCount;

    @Schema(description = "迟到分钟数")
    private Integer lateMinutes;

    @Schema(description = "早退次数")
    private Integer earlyCount;

    @Schema(description = "早退分钟数")
    private Integer earlyMinutes;

    @Schema(description = "打卡记录")
    private List<HrmAttendanceClockRespVO> clockList;

}

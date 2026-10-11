package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 月度考勤汇总 Response VO")
@Data
public class HrmAttendanceMonthRecordRespVO {

    @Schema(description = "月度汇总编号")
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

    @Schema(description = "考勤组名称")
    private String attendanceGroupName;

    @Schema(description = "入职时间")
    private LocalDateTime entryTime;

    @Schema(description = "员工状态")
    private Integer employeeStatus;

    @Schema(description = "工作城市")
    private String workCity;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "应出勤天数")
    private BigDecimal attendDays;

    @Schema(description = "实际出勤天数")
    private BigDecimal actualDays;

    @Schema(description = "迟到分钟数")
    private Integer lateMinute;

    @Schema(description = "迟到次数")
    private Integer lateCount;

    @Schema(description = "早退分钟数")
    private Integer earlyMinute;

    @Schema(description = "早退次数")
    private Integer earlyCount;

    @Schema(description = "缺卡次数")
    private Integer misscardCount;

    @Schema(description = "旷工天数")
    private BigDecimal absenteeismDays;

    @Schema(description = "旷工分钟数")
    private Integer absenteeismMinutes;

    @Schema(description = "请假天数")
    private BigDecimal leaveDays;

    @Schema(description = "请假分钟数")
    private Integer leaveMinutes;

    @Schema(description = "迟到扣款")
    private BigDecimal lateDeductAmount;

    @Schema(description = "早退扣款")
    private BigDecimal earlyDeductAmount;

    @Schema(description = "缺卡扣款")
    private BigDecimal misscardDeductAmount;

    @Schema(description = "旷工扣款")
    private BigDecimal absenteeismDeductAmount;

    @Schema(description = "考勤扣款合计")
    private BigDecimal attendanceDeductAmount;

    @Schema(description = "是否全勤")
    private Boolean fullAttendance;

}

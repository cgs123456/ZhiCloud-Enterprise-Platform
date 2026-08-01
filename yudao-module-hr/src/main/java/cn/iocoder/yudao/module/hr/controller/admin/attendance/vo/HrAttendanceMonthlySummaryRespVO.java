package cn.iocoder.yudao.module.hr.controller.admin.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 考勤月度汇总 Response VO")
@Data
public class HrAttendanceMonthlySummaryRespVO {

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long employeeId;

    @Schema(description = "考勤月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "202401")
    private String month;

    @Schema(description = "应出勤天数", example = "22")
    private Integer totalDays;

    @Schema(description = "正常天数", example = "20")
    private Integer normalDays;

    @Schema(description = "迟到天数", example = "1")
    private Integer lateDays;

    @Schema(description = "早退天数", example = "0")
    private Integer earlyLeaveDays;

    @Schema(description = "缺勤天数", example = "1")
    private Integer absentDays;

    @Schema(description = "加班天数", example = "2")
    private Integer overtimeDays;

    @Schema(description = "加班总时长（小时）", example = "8.5")
    private BigDecimal totalOvertimeHours;

}
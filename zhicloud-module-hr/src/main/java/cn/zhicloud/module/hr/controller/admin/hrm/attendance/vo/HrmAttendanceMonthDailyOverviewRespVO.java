package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;

@Schema(description = "管理后台 - HRM 月度每日考勤概览 Response VO")
@Data
public class HrmAttendanceMonthDailyOverviewRespVO {

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

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "每日打卡概况（key=yyyy-MM-dd）")
    private Map<String, HrmAttendanceDailyOverviewRespVO> dailyClockMap;

}

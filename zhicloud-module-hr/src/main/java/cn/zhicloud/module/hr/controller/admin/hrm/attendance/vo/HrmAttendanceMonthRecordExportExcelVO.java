package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;

@Schema(description = "管理后台 - HRM 月度考勤汇总导出 Excel 行 VO")
@Data
public class HrmAttendanceMonthRecordExportExcelVO {

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "应出勤天数")
    private BigDecimal attendDays;

    @Schema(description = "实际出勤天数")
    private BigDecimal actualDays;

    @Schema(description = "迟到次数")
    private Integer lateCount;

    @Schema(description = "早退次数")
    private Integer earlyCount;

    @Schema(description = "缺卡次数")
    private Integer misscardCount;

    @Schema(description = "旷工天数")
    private BigDecimal absenteeismDays;

    @Schema(description = "请假天数")
    private BigDecimal leaveDays;

    @Schema(description = "考勤扣款合计")
    private BigDecimal attendanceDeductAmount;

    @Schema(description = "是否全勤")
    private Boolean fullAttendance;

}

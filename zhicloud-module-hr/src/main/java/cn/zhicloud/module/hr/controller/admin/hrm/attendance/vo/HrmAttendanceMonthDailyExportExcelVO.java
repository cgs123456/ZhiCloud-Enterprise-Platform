package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * HRM 月度打卡概况导出 Excel 行 VO（扁平结构：FastExcel 不支持嵌套 Map/List 列）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - HRM 月度打卡概况导出 Excel 行 VO")
@Data
public class HrmAttendanceMonthDailyExportExcelVO {

    @ExcelProperty("员工姓名")
    private String employeeName;

    @ExcelProperty("工号")
    private String jobNumber;

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("年份")
    private Integer year;

    @ExcelProperty("月份")
    private Integer month;

    @ExcelProperty("有打卡天数")
    private Integer clockDays;

}

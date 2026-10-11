package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工自助端 - 月度考勤导出 Excel 行 VO
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 月度考勤导出 Excel 行 VO")
@Data
public class HrmPortalAttendanceMonthExportExcelVO {

    @ExcelProperty("员工姓名")
    @Schema(description = "员工姓名")
    private String employeeName;

    @ExcelProperty("年份")
    @Schema(description = "年份")
    private Integer year;

    @ExcelProperty("月份")
    @Schema(description = "月份")
    private Integer month;

    @ExcelProperty("实际出勤天数")
    @Schema(description = "实际出勤天数")
    private BigDecimal actualDays;

    @ExcelProperty("请假天数")
    @Schema(description = "请假天数")
    private BigDecimal leaveDays;

}

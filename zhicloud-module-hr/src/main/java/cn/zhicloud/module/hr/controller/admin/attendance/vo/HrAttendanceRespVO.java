package cn.zhicloud.module.hr.controller.admin.attendance.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.hr.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 考勤记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HrAttendanceRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @ExcelProperty("员工 ID")
    private Long employeeId;

    @Schema(description = "考勤日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @ExcelProperty("考勤日期")
    private LocalDate attendanceDate;

    @Schema(description = "签到时间", example = "2024-01-01T09:00:00")
    @ExcelProperty("签到时间")
    private LocalDateTime checkInTime;

    @Schema(description = "签退时间", example = "2024-01-01T18:00:00")
    @ExcelProperty("签退时间")
    private LocalDateTime checkOutTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_ATTENDANCE_STATUS)
    private Integer status;

    @Schema(description = "加班时长（小时）", example = "2.5")
    @ExcelProperty("加班时长")
    private BigDecimal overtimeHours;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
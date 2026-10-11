package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import cn.idev.excel.annotation.ExcelProperty;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 考勤打卡导出 Excel 行 VO")
@Data
public class HrmAttendanceClockExportExcelVO {

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "应打卡日期")
    private LocalDateTime attendanceTime;

    @Schema(description = "打卡时间")
    private LocalDateTime clockTime;

    @Schema(description = "打卡类型：1上班 2下班")
    private Integer type;

    @Schema(description = "打卡状态")
    private Integer status;

    @Schema(description = "打卡地点")
    private String address;

    @Schema(description = "备注")
    private String remark;

}

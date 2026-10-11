package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 考勤请假导出 Excel 行 VO")
@Data
public class HrmAttendanceLeaveExportExcelVO {

    @Schema(description = "员工姓名")
    private String employeeName;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "请假类型")
    private String type;

    @Schema(description = "请假开始时间")
    private LocalDateTime startTime;

    @Schema(description = "请假结束时间")
    private LocalDateTime endTime;

    @Schema(description = "请假天数")
    private BigDecimal day;

    @Schema(description = "请假事由")
    private String reason;

    @Schema(description = "审批状态")
    private Integer approvalStatus;

}

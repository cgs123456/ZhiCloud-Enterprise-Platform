package cn.iocoder.yudao.module.hr.controller.admin.attendance.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.hr.enums.attendance.HrAttendanceStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 考勤记录新增/修改 Request VO")
@Data
public class HrAttendanceSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "考勤日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "考勤日期不能为空")
    private LocalDate attendanceDate;

    @Schema(description = "签到时间", example = "2024-01-01T09:00:00")
    private LocalDateTime checkInTime;

    @Schema(description = "签退时间", example = "2024-01-01T18:00:00")
    private LocalDateTime checkOutTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    @InEnum(HrAttendanceStatusEnum.class)
    private Integer status;

    @Schema(description = "加班时长（小时）", example = "2.5")
    private BigDecimal overtimeHours;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
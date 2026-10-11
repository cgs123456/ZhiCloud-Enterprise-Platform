package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Schema(description = "管理后台 - HRM 考勤节假日新增/修改 Request VO")
@Data
public class HrmAttendanceHolidaySaveReqVO {

    @Schema(description = "节假日编号")
    private Long id;

    @Schema(description = "日期")
    @NotNull(message = "日期不能为空")
    private LocalDate date;

    @Schema(description = "日期类型：1上班 2休息")
    @NotNull(message = "日期类型不能为空")
    private Integer type;

}

package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 考勤节假日 Response VO")
@Data
public class HrmAttendanceHolidayRespVO {

    @Schema(description = "节假日编号")
    private Long id;

    @Schema(description = "日期")
    private LocalDate date;

    @Schema(description = "日期类型：1上班 2休息")
    private Integer type;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}

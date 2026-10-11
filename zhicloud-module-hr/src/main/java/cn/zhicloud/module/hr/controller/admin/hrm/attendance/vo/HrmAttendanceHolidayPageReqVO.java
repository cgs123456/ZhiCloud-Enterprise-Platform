package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Schema(description = "管理后台 - HRM 考勤节假日分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmAttendanceHolidayPageReqVO extends PageParam {

    @Schema(description = "日期范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] dates;

    @Schema(description = "日期类型：1上班 2休息")
    private Integer type;

}

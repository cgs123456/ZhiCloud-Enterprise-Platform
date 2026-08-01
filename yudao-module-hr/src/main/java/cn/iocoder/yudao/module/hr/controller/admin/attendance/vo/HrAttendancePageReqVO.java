package cn.iocoder.yudao.module.hr.controller.admin.attendance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - HR 考勤记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrAttendancePageReqVO extends PageParam {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2024-01-31")
    private LocalDate endDate;

}
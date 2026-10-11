package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 首页员工概览 VO")
@Data
public class HrmHrHomeEmployeeSurveyVO {

    @Schema(description = "在职员工数量")
    private Long activeCount;

    @Schema(description = "本月入职员工数量")
    private Long entryThisMonthCount;

    @Schema(description = "本月待入职员工数量")
    private Long pendingEntryThisMonthCount;

    @Schema(description = "本月离职员工数量")
    private Long leaveThisMonthCount;

    @Schema(description = "本月待离职员工数量")
    private Long pendingLeaveThisMonthCount;

    @Schema(description = "本月转正员工数量")
    private Long regularThisMonthCount;

    @Schema(description = "本月调岗员工数量")
    private Long transferThisMonthCount;

}

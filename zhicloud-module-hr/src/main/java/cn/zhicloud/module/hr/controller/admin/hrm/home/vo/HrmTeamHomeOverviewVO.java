package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 团队工作台我的团队 VO")
@Data
public class HrmTeamHomeOverviewVO {

    @Schema(description = "团队人数")
    private Long employeeCount;

    @Schema(description = "本月入职人数")
    private Long entryThisMonthCount;

    @Schema(description = "本月离职人数")
    private Long leaveThisMonthCount;

    @Schema(description = "本月转正人数")
    private Long regularThisMonthCount;

}

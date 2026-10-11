package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 团队工作台统计响应 VO")
@Data
public class HrmTeamHomeStatisticsRespVO {

    @Schema(description = "当前登录员工编号")
    private Long leaderEmployeeId;

    @Schema(description = "我的团队")
    private HrmTeamHomeOverviewVO teamOverview;

    @Schema(description = "团队概况")
    private HrmTeamHomeSurveyVO teamSurvey;

}

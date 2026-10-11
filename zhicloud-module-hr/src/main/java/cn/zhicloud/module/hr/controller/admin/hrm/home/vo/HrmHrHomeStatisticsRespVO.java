package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 首页统计响应 VO")
@Data
public class HrmHrHomeStatisticsRespVO {

    @Schema(description = "员工概览")
    private HrmHrHomeEmployeeSurveyVO employeeSurvey;

    @Schema(description = "招聘概览")
    private HrmHrHomeRecruitSurveyVO recruitSurvey;

    @Schema(description = "薪资概览")
    private HrmHrHomeSalarySurveyVO salarySurvey;

    @Schema(description = "待办概览")
    private HrmHrHomeTodoSurveyVO todoSurvey;

}

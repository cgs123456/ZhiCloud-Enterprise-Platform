package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 首页招聘概览 VO")
@Data
public class HrmHrHomeRecruitSurveyVO {

    @Schema(description = "招聘中职位数量")
    private Long recruitingPostCount;

    @Schema(description = "流程中候选人数量")
    private Long candidateInProcessCount;

    @Schema(description = "待入职候选人数量")
    private Long pendingEntryCount;

    @Schema(description = "已入职候选人数量")
    private Long joinedCount;

}

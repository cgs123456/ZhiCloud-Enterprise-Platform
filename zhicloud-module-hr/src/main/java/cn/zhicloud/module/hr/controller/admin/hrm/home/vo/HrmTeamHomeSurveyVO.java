package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 团队工作台团队概况 VO")
@Data
public class HrmTeamHomeSurveyVO {

    @Schema(description = "员工状态占比")
    private java.util.List<HrmTeamHomeAnalysisItemVO> statusAnalysis;

    @Schema(description = "男女性别占比")
    private java.util.List<HrmTeamHomeAnalysisItemVO> sexAnalysis;

    @Schema(description = "成员年龄占比")
    private java.util.List<HrmTeamHomeAnalysisItemVO> ageAnalysis;

    @Schema(description = "成员司龄占比")
    private java.util.List<HrmTeamHomeAnalysisItemVO> companyAgeAnalysis;

}

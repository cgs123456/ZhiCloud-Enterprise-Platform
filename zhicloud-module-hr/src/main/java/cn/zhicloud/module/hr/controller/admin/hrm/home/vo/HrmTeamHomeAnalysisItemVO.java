package cn.zhicloud.module.hr.controller.admin.hrm.home.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 团队工作台统计分析项 VO")
@Data
public class HrmTeamHomeAnalysisItemVO {

    @Schema(description = "分类类型（null 表示未填写）")
    private Integer type;

    @Schema(description = "数量")
    private Long count;

}

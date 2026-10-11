package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 绩效计划等级数量 Response VO")
@Data
public class HrmPerformancePlanLevelCountRespVO {

    @Schema(description = "结果等级")
    private String level;

    @Schema(description = "数量")
    private Long count;

}

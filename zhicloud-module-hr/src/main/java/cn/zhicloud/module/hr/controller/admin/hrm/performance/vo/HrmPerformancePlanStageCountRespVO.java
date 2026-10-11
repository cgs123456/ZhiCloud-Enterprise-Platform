package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 绩效计划阶段数量 Response VO")
@Data
public class HrmPerformancePlanStageCountRespVO {

    @Schema(description = "阶段类型")
    private Integer stageType;

    @Schema(description = "数量")
    private Long count;

}

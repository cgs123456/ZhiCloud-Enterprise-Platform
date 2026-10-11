package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 绩效计划状态数量 Response VO")
@Data
public class HrmPerformancePlanStatusCountRespVO {

    @Schema(description = "计划状态")
    private Integer status;

    @Schema(description = "数量")
    private Long count;

}

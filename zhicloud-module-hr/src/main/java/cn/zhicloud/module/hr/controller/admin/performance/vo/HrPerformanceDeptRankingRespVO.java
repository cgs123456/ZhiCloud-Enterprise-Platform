package cn.zhicloud.module.hr.controller.admin.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 部门绩效排名 Response VO")
@Data
public class HrPerformanceDeptRankingRespVO {

    @Schema(description = "排名", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer rank;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long employeeId;

    @Schema(description = "考核周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024Q1")
    private String period;

    @Schema(description = "考核得分", example = "95.5")
    private BigDecimal score;

    @Schema(description = "考核等级", example = "10")
    private Integer grade;

}
package cn.zhicloud.module.ai.controller.admin.predictive.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - AI 预测性维护 Response VO")
@Data
public class PredictiveMaintenanceRespVO {

    @Schema(description = "设备编号", example = "1024")
    private Long deviceId;

    @Schema(description = "故障概率（0-1）", example = "0.65")
    private Double probability;

    @Schema(description = "风险等级（HIGH/MEDIUM/LOW）", example = "MEDIUM")
    private String riskLevel;

    @Schema(description = "建议维护日期", example = "2026-08-15")
    private LocalDate recommendedDate;

    @Schema(description = "推理过程")
    private String reasoning;

    @Schema(description = "维护建议（仅 /recommendation 接口返回）")
    private String recommendation;

}

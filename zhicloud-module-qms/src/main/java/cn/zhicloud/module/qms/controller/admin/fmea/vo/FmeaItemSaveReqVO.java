package cn.zhicloud.module.qms.controller.admin.fmea.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS FMEA 条目新增/修改 Request VO")
@Data
public class FmeaItemSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "FMEA 文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "FMEA 文档 ID 不能为空")
    private Long fmeaId;

    @Schema(description = "功能", example = "密封")
    private String function;

    @Schema(description = "失效模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "密封不良")
    @NotNull(message = "失效模式不能为空")
    private String failureMode;

    @Schema(description = "失效后果", example = "泄漏")
    private String failureEffect;

    @Schema(description = "严重度 S（1-10）", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    @NotNull(message = "严重度不能为空")
    private Integer severity;

    @Schema(description = "潜在失效原因", example = "密封圈尺寸不符")
    private String potentialCause;

    @Schema(description = "频度 O（1-10）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "频度不能为空")
    private Integer occurrence;

    @Schema(description = "现行控制措施", example = "全检")
    private String currentControls;

    @Schema(description = "探测度 D（1-10）", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @NotNull(message = "探测度不能为空")
    private Integer detection;

    @Schema(description = "建议措施", example = "更换密封圈供应商")
    private String actionRecommended;

    @Schema(description = "已采取措施", example = "已更换供应商")
    private String actionTaken;

    @Schema(description = "备注", example = "随便")
    private String remark;

}

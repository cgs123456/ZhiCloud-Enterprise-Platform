package cn.zhicloud.module.ai.controller.admin.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - AI 工作流校验 Request VO")
@Data
public class AiWorkflowValidateReqVO {

    @Schema(description = "工作流模型 JSON", required = true, example = "{}")
    @NotEmpty(message = "工作流模型不能为空")
    private String graph;

}

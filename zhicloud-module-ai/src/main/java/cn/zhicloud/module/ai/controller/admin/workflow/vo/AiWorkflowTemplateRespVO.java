package cn.zhicloud.module.ai.controller.admin.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - AI 工作流模板 Response VO")
@Data
public class AiWorkflowTemplateRespVO {

    @Schema(description = "模板标识", required = true, example = "qa-llm")
    private String code;

    @Schema(description = "模板名称", required = true, example = "LLM 问答模板")
    private String name;

    @Schema(description = "模板描述", example = "最基础的 LLM 问答工作流")
    private String description;

    @Schema(description = "分类（flow/ai/tool）", example = "ai")
    private String category;

    @Schema(description = "工作流模型 JSON", required = true, example = "{}")
    private String graph;

}

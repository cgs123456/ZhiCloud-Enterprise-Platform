package cn.zhicloud.module.ai.controller.admin.prompt.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI Prompt 模板 Response VO")
@Data
public class AiPromptTemplateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "RAG 问答系统提示")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "rag_qa_system")
    private String code;

    @Schema(description = "分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "RAG")
    private String category;

    @Schema(description = "模板内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "你是一个专业助手，请基于 {context} 回答 {question}")
    private String content;

    @Schema(description = "变量列表 JSON", example = "[\"context\",\"question\"]")
    private String variables;

    @Schema(description = "描述", example = "用于 RAG 检索增强问答的系统提示模板")
    private String description;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "默认启用")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}

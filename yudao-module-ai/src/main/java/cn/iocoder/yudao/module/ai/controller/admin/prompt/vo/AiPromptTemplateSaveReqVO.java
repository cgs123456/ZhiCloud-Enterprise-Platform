package cn.iocoder.yudao.module.ai.controller.admin.prompt.vo;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.ai.enums.prompt.AiPromptTemplateCategoryEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI Prompt 模板新增/修改 Request VO")
@Data
public class AiPromptTemplateSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "RAG 问答系统提示")
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "rag_qa_system")
    @NotBlank(message = "模板编码不能为空")
    private String code;

    @Schema(description = "分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "RAG")
    @NotBlank(message = "分类不能为空")
    @InEnum(AiPromptTemplateCategoryEnum.class)
    private String category;

    @Schema(description = "模板内容（含变量占位符 {variableName}）", requiredMode = Schema.RequiredMode.REQUIRED, example = "你是一个专业助手，请基于 {context} 回答 {question}")
    @NotBlank(message = "模板内容不能为空")
    private String content;

    @Schema(description = "变量列表 JSON", example = "[\"context\",\"question\"]")
    private String variables;

    @Schema(description = "描述", example = "用于 RAG 检索增强问答的系统提示模板")
    private String description;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "默认启用")
    private String remark;

}

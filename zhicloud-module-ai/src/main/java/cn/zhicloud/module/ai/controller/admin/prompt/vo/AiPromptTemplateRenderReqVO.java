package cn.zhicloud.module.ai.controller.admin.prompt.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Schema(description = "管理后台 - AI Prompt 模板渲染 Request VO")
@Data
public class AiPromptTemplateRenderReqVO {

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "rag_qa_system")
    @NotBlank(message = "模板编码不能为空")
    private String code;

    @Schema(description = "变量键值对", requiredMode = Schema.RequiredMode.REQUIRED, example = "{\"context\":\"相关上下文\",\"question\":\"用户问题\"}")
    private Map<String, Object> variables;

}

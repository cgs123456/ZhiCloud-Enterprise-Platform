package cn.iocoder.yudao.module.ai.controller.admin.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - AI 工作流节点类型 Response VO")
@Data
public class AiWorkflowNodeTypeRespVO {

    @Schema(description = "节点类型标识", required = true, example = "llmNode")
    private String nodeType;

    @Schema(description = "节点名称", required = true, example = "LLM 调用")
    private String name;

    @Schema(description = "分类（flow 流程控制 / ai AI 能力 / tool 工具）", required = true, example = "ai")
    private String category;

    @Schema(description = "节点描述", example = "调用大语言模型生成回复")
    private String description;

    @Schema(description = "配置项 JSON Schema，前端表单渲染用", example = "{}")
    private Map<String, Object> configSchema;

    @Schema(description = "输入端口定义")
    private List<PortDefinition> inputs;

    @Schema(description = "输出端口定义")
    private List<PortDefinition> outputs;

    /**
     * 端口（输入/输出）定义
     */
    @Schema(description = "端口定义")
    @Data
    public static class PortDefinition {

        @Schema(description = "端口名称", required = true, example = "message")
        private String name;

        @Schema(description = "端口类型（string/number/boolean/object/array）", required = true, example = "string")
        private String type;

        @Schema(description = "端口描述", example = "用户输入消息")
        private String description;

    }

}

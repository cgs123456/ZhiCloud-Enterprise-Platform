package cn.zhicloud.module.ai.controller.admin.workflow.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - AI 工作流校验 Response VO")
@Data
public class AiWorkflowValidateRespVO {

    @Schema(description = "是否校验通过", required = true, example = "true")
    private Boolean valid;

    @Schema(description = "校验错误信息列表（校验通过时为空）", example = "[\"节点 llmNode 缺少 llmId 配置\"]")
    private List<String> errors;

}

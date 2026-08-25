package cn.zhicloud.module.aimultiagent.controller.admin.topology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 多 Agent 拓扑配置新增/修改 Request VO")
@Data
public class MultiAgentTopologySaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "拓扑名称", required = true, example = "默认编排拓扑")
    @NotEmpty(message = "拓扑名称不能为空")
    private String name;

    @Schema(description = "描述", example = "用于生成业务报告的多 Agent 拓扑")
    private String description;

    @Schema(description = "Supervisor 系统提示词", required = true, example = "你是一个专业的任务编排助手")
    @NotEmpty(message = "Supervisor 系统提示词不能为空")
    private String supervisorSystemPrompt;

    @Schema(description = "Worker 配置 JSON", required = true, example = "[{\"name\":\"report-writer\",\"description\":\"报告生成\",\"systemPrompt\":\"\",\"tools\":[]}]")
    @NotEmpty(message = "Worker 配置不能为空")
    private String workerConfig;

    @Schema(description = "最大调用深度", required = true, example = "5")
    @NotNull(message = "最大调用深度不能为空")
    private Integer maxDepth;

    @Schema(description = "Token 预算上限", required = true, example = "10000")
    @NotNull(message = "Token 预算上限不能为空")
    private Integer maxTokenBudget;

    @Schema(description = "状态（0启用 1停用）", required = true, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}

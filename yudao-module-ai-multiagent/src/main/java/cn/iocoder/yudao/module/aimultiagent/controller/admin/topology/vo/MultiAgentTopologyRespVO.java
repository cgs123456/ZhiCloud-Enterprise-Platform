package cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 多 Agent 拓扑配置 Response VO")
@Data
public class MultiAgentTopologyRespVO {

    @Schema(description = "编号", required = true, example = "1")
    private Long id;

    @Schema(description = "拓扑名称", required = true, example = "默认编排拓扑")
    private String name;

    @Schema(description = "描述", example = "用于生成业务报告的多 Agent 拓扑")
    private String description;

    @Schema(description = "Supervisor 系统提示词", required = true, example = "你是一个专业的任务编排助手")
    private String supervisorSystemPrompt;

    @Schema(description = "Worker 配置 JSON", required = true, example = "[{\"name\":\"report-writer\"}]")
    private String workerConfig;

    @Schema(description = "最大调用深度", required = true, example = "5")
    private Integer maxDepth;

    @Schema(description = "Token 预算上限", required = true, example = "10000")
    private Integer maxTokenBudget;

    @Schema(description = "状态（0启用 1停用）", required = true, example = "0")
    private Integer status;

    @Schema(description = "创建时间", required = true, example = "时间戳格式")
    private LocalDateTime createTime;

}

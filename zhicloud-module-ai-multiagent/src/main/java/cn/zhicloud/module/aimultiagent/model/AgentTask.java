package cn.zhicloud.module.aimultiagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent 任务模型
 *
 * 由 Supervisor Agent 拆解生成，描述需要分配给 Worker 执行的子任务。
 *
 * @author zhicloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTask {

    /**
     * 任务 ID（由 Supervisor 生成，用于唯一标识本次编排中的子任务）
     */
    private String taskId;

    /**
     * 任务描述（Worker 执行时作为用户消息传入 LLM）
     */
    private String description;

    /**
     * 分配的 Worker 名称（对应 {@code WorkerAgentRegistry} 中注册的 Worker name）
     */
    private String assignedWorker;

    /**
     * 需要的工具列表（Worker 可据此绑定 MCP Tool）
     */
    private List<String> requiredTools;

    /**
     * 任务参数（透传给 Worker 的上下文信息）
     */
    private Map<String, Object> parameters;

}

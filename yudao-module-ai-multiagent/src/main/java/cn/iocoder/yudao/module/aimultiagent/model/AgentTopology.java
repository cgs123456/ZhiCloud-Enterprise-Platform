package cn.iocoder.yudao.module.aimultiagent.model;

import lombok.Data;

import java.util.List;

/**
 * Agent 拓扑模型
 *
 * 描述一次多 Agent 编排的完整拓扑配置，包括 Supervisor 系统提示词、Worker 列表及熔断参数。
 * 由 {@code MultiAgentTopologyDO.workerConfig}（JSON）反序列化得到。
 *
 * @author yudao
 */
@Data
public class AgentTopology {

    /**
     * Supervisor 系统提示词
     */
    private String supervisorSystemPrompt;

    /**
     * 配置 schema 版本（如 v1），用于拓扑配置向前兼容校验
     */
    private String version;

    /**
     * Worker 配置列表
     */
    private List<WorkerConfig> workers;

    /**
     * 最大调用深度（防死循环，超过则熔断）
     */
    private Integer maxDepth;

    /**
     * Token 预算上限（超过则熔断）
     */
    private Integer maxTokenBudget;

    /**
     * Worker 配置
     */
    @Data
    public static class WorkerConfig {

        /**
         * Worker 名称（对应 {@code WorkerAgentRegistry} 中注册的 Worker name）
         */
        private String name;

        /**
         * Worker 描述（供 Supervisor 选择 Worker 时参考）
         */
        private String description;

        /**
         * Worker 系统提示词
         */
        private String systemPrompt;

        /**
         * 工具名称列表（Worker 可绑定的 MCP Tool 名称）
         */
        private List<String> tools;
    }

}

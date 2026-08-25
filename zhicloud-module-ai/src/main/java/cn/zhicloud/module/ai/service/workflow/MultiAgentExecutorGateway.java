package cn.zhicloud.module.ai.service.workflow;

/**
 * Multi-Agent 执行器 SPI 网关
 *
 * <p>用于解耦 zhicloud-module-ai 与 zhicloud-module-ai-multiagent 模块。
 * ai 模块依赖此接口，multiagent 模块提供实现。
 *
 * <p>背景：zhicloud-module-ai-multiagent 依赖 zhicloud-module-ai（正向依赖），
 * 而 AiWorkflowServiceImpl 的 multiAgentNode 节点需要反向调用多 Agent 编排能力。
 * 通过此 SPI 接口实现依赖反转，避免循环依赖。
 *
 * <p>C8 修复：AiWorkflowServiceImpl 的 multiAgentNode 节点通过此接口调用多 Agent 编排。
 */
public interface MultiAgentExecutorGateway {

    /**
     * 执行多 Agent 编排
     *
     * @param topologyId 拓扑 ID
     * @param task 用户任务/输入
     * @param tenantId 租户 ID
     * @return 最终答案
     */
    String execute(Long topologyId, String task, Long tenantId);

}

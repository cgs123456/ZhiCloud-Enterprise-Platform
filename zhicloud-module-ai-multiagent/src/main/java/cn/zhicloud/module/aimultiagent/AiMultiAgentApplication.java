package cn.zhicloud.module.aimultiagent;

/**
 * AI 多 Agent 编排模块的入口标记类。
 *
 * 本模块作为 zhicloud 项目的可选模块，本身不提供独立的 Spring Boot 启动入口，
 * 由 zhicloud-server 主应用通过 ComponentScan 扫描 {@code cn.zhicloud.module.aimultiagent} 包加载。
 *
 * 核心能力：
 * 1. 多 Agent 拓扑配置管理（aimultiagent_topology）
 * 2. 编排执行引擎（Supervisor 任务拆解 → Worker 分发执行 → Supervisor 汇总）
 * 3. 执行日志记录（aimultiagent_execution_log）
 * 4. 熔断机制（调用深度上限 + Token 预算上限）
 *
 * @author zhicloud
 */
public class AiMultiAgentApplication {
}

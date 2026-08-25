/**
 * AI MultiAgent 模块：多 Agent 编排（Supervisor-Worker + ReAct Agent）
 *
 * <p>Spring Modulith 模块声明（A3）。依赖：ai/system/infra。
 *
 * <p>本模块基于 Spring AI 1.1.8 实现：
 * <ul>
 *   <li>Supervisor-Worker 多 Agent 编排（任务拆解→分发→汇总）</li>
 *   <li>ReAct Agent 框架（Thought→Action→Observation 循环）</li>
 *   <li>调用深度与 Token 预算双重熔断</li>
 * </ul>
 *
 * @author zhicloud
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI MultiAgent 多 Agent 编排模块")
package cn.zhicloud.module.aimultiagent;

package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.model.AgentResult;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTask;
import cn.iocoder.yudao.module.aimultiagent.service.llm.LlmGateway;
import cn.iocoder.yudao.module.aimultiagent.service.llm.PromptInjectionGuard;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Worker Agent 抽象基类
 *
 * 设计要点：
 *  1. 所有 Worker Agent 继承此类，实现 {@link #getName()}、{@link #getDescription()}、
 *     {@link #getSupportedTools()}、{@link #execute(AgentTask, Long)} 方法；
 *  2. {@link #callLlm(String, String)} 委托 {@link LlmGateway} 调用 LLM（统一注入防护 / 重试 / 限流 / 熔断 / 指标）；
 *  3. ChatClient 由 {@link LlmGateway} 内部通过 {@link ChatClientHelper} 按需构建，避免无 LLM API key 时启动失败；
 *  4. 真实业务工具数据通过 {@link PromptInjectionGuard#wrapExternalData} 包裹为惰性数据块，防范 Prompt 注入。
 *
 * @author yudao
 */
@Slf4j
public abstract class AbstractWorkerAgent {

    /**
     * ChatClient 获取助手（由子类通过构造器注入）
     */
    protected final ChatClientHelper chatClientHelper;

    /**
     * LLM 调用统一门户（重试 / 限流 / 熔断 / 注入防护 / 指标）
     */
    protected final LlmGateway llmGateway;

    protected AbstractWorkerAgent(ChatClientHelper chatClientHelper, LlmGateway llmGateway) {
        this.chatClientHelper = chatClientHelper;
        this.llmGateway = llmGateway;
    }

    /**
     * Worker 名称（唯一标识，用于拓扑配置中的 assignedWorker 匹配）
     */
    public abstract String getName();

    /**
     * Worker 描述（供 Supervisor 选择 Worker 时参考）
     */
    public abstract String getDescription();

    /**
     * Worker 支持的工具列表
     */
    public abstract List<String> getSupportedTools();

    /**
     * 执行任务
     *
     * @param task     任务描述
     * @param tenantId 租户 ID
     * @return 执行结果
     */
    public abstract AgentResult execute(AgentTask task, Long tenantId);

    /**
     * 调用 LLM 生成回答（经 {@link LlmGateway}：注入防护 + 重试 + 限流 + 熔断 + 指标）。
     */
    protected String callLlm(String systemPrompt, String userMessage) {
        return llmGateway.call(systemPrompt, userMessage, "worker:" + getName());
    }

    /**
     * 构造带真实业务工具数据的用户消息
     *
     * <p>将任务所需的工具（优先 {@link AgentTask#getRequiredTools()}，缺省时回退到
     * {@link #getSupportedTools()}）逐一通过 {@link WorkerToolExecutor} 调用真实业务系统，
     * 把返回的真实数据快照（经 {@link PromptInjectionGuard#wrapExternalData} 包裹为惰性数据块，防范注入）
     * 注入提示词，供 LLM 基于真实数据作答，而非凭空生成。
     *
     * <p>返回 {@link RealToolResult}，其中 {@code allFailed} 表示全部工具真实数据均拉取失败。
     * 调用方应据此将任务结果标记为失败，<b>而非误报成功</b>。
     *
     * @param task          任务
     * @param tenantId      租户编号（用于工具调用时的数据隔离）
     * @param toolExecutor  真实工具执行器
     * @param taskKind      任务类型描述（仅用于提示词文案，如「采购」「生产」）
     * @return 含真实工具数据的用户消息及工具失败情况
     */
    protected RealToolResult buildPromptWithRealTools(AgentTask task, Long tenantId,
                                                       WorkerToolExecutor toolExecutor, String taskKind) {
        StringBuilder sb = new StringBuilder();
        sb.append("请完成以下").append(taskKind).append("任务：\n").append(task.getDescription());
        if (task.getParameters() != null && !task.getParameters().isEmpty()) {
            sb.append("\n\n【任务参数】\n");
            task.getParameters().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }

        List<String> tools = task.getRequiredTools();
        if (CollUtil.isEmpty(tools)) {
            tools = getSupportedTools();
        }
        List<String> failedTools = new ArrayList<>();
        if (CollUtil.isNotEmpty(tools)) {
            sb.append("\n\n【已调用真实业务工具获取的数据（必须严格基于以下真实数据作答）】\n");
            for (String tool : tools) {
                String data = toolExecutor.execute(tool, tenantId);
                // 不受信任的外部业务数据包裹为惰性数据块，防止 Prompt 注入
                sb.append("\n### 工具 ").append(tool).append("\n")
                        .append(PromptInjectionGuard.wrapExternalData(tool, data)).append("\n");
                if (isToolFailure(data)) {
                    failedTools.add(tool);
                    log.warn("[buildPromptWithRealTools][工具 {} 真实数据拉取失败，tenantId={}]", tool, tenantId);
                }
            }
        }
        boolean allFailed = CollUtil.isNotEmpty(tools) && failedTools.size() == tools.size();
        return new RealToolResult(sb.toString(), allFailed, failedTools);
    }

    /**
     * 判断工具返回内容是否为「失败 / 不支持」说明（而非真实数据快照）。
     */
    private static boolean isToolFailure(String data) {
        return data != null && (data.contains("调用失败") || data.contains("暂无对应真实业务实现"));
    }

    /**
     * 估算 Token 数：中文（含 CJK 标点 / 全角）约 1 token/字，英文约 4 字符/token。
     * 委托 {@link LlmGateway#estimateTokens}（CJK 友好估算）。
     */
    protected int estimateTokens(String text) {
        return llmGateway.estimateTokens(text);
    }

    /**
     * {@link #buildPromptWithRealTools} 的返回结果。
     */
    protected static class RealToolResult {
        public final String prompt;
        public final boolean allFailed;
        public final List<String> failedTools;

        public RealToolResult(String prompt, boolean allFailed, List<String> failedTools) {
            this.prompt = prompt;
            this.allFailed = allFailed;
            this.failedTools = failedTools;
        }
    }

}

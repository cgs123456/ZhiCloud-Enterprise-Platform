package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.model.AgentResult;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTask;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * Worker Agent 抽象基类
 *
 * 设计要点：
 *  1. 所有 Worker Agent 继承此类，实现 {@link #getName()}、{@link #getDescription()}、
 *     {@link #getSupportedTools()}、{@link #execute(AgentTask, Long)} 方法；
 *  2. {@link #callLlm(String, String)} 封装了 ChatClient 调用逻辑，子类直接复用；
 *  3. ChatClient 通过 {@link ChatClientHelper} 按需构建，避免无 LLM API key 时启动失败。
 *
 * @author yudao
 */
public abstract class AbstractWorkerAgent {

    /**
     * ChatClient 获取助手（由子类通过构造器注入）
     */
    protected final ChatClientHelper chatClientHelper;

    protected AbstractWorkerAgent(ChatClientHelper chatClientHelper) {
        this.chatClientHelper = chatClientHelper;
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
     * 调用 LLM 生成回答
     *
     * @param systemPrompt 系统提示词
     * @param userMessage  用户消息
     * @return LLM 生成的文本
     */
    protected String callLlm(String systemPrompt, String userMessage) {
        ChatClient chatClient = chatClientHelper.getChatClient();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

}

package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.model.AgentResult;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTask;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 默认报告生成 Worker
 *
 * 示例 Worker，继承 {@link AbstractWorkerAgent}，负责生成报告类文本。
 * 启动时自动注册到 {@link WorkerAgentRegistry}。
 *
 * @author yudao
 */
@Component
@Slf4j
public class DefaultReportWorker extends AbstractWorkerAgent {

    private static final String WORKER_NAME = "report-writer";
    private static final String WORKER_DESCRIPTION = "生成报告的 Worker，擅长将信息整理为结构化的报告文本";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个专业的报告撰写助手。请基于用户提供的任务描述和上下文，生成清晰、结构化的报告。\n" +
                    "报告应包含标题、摘要、正文和结论。回答请使用中文。";

    private final WorkerAgentRegistry registry;

    public DefaultReportWorker(ChatClientHelper chatClientHelper, WorkerAgentRegistry registry) {
        super(chatClientHelper);
        this.registry = registry;
    }

    /**
     * 启动时自动注册到注册中心
     */
    @PostConstruct
    public void init() {
        registry.register(this);
    }

    @Override
    public String getName() {
        return WORKER_NAME;
    }

    @Override
    public String getDescription() {
        return WORKER_DESCRIPTION;
    }

    @Override
    public List<String> getSupportedTools() {
        return Collections.emptyList();
    }

    @Override
    public AgentResult execute(AgentTask task, Long tenantId) {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 构造用户消息（包含任务描述和参数）
            String userMessage = buildUserMessage(task);

            // 2. 调用 LLM 生成报告
            String output = callLlm(DEFAULT_SYSTEM_PROMPT, userMessage);

            // 3. 返回成功结果
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(true)
                    .output(output)
                    .tokensUsed(estimateTokens(output))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("[execute][报告生成失败，taskId={}]", task.getTaskId(), e);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg(StrUtil.sub(e.getMessage(), 0, 500))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * 构造用户消息
     */
    private String buildUserMessage(AgentTask task) {
        StringBuilder message = new StringBuilder();
        message.append("请完成以下任务：\n").append(task.getDescription());
        if (task.getParameters() != null && !task.getParameters().isEmpty()) {
            message.append("\n\n【任务参数】\n");
            task.getParameters().forEach((k, v) -> message.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        return message.toString();
    }

    /**
     * 粗略估算 Token 数（4 字符 ≈ 1 token）
     */
    private int estimateTokens(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        return text.length() / 4;
    }

}

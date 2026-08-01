package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.model.AgentResult;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTask;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 生产 Worker
 *
 * <p>负责生产域任务：生产排程、产能评估、物料需求分析、生产异常处理等。
 * 启动时自动注册到 {@link WorkerAgentRegistry}。
 *
 * @author yudao
 */
@Component
@Slf4j
public class ProductionWorker extends AbstractWorkerAgent {

    private static final String WORKER_NAME = "production-worker";
    private static final String WORKER_DESCRIPTION =
            "生产域 Worker，擅长生产排程、产能评估、物料需求分析、生产异常处理";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一名资深生产管理顾问。请基于用户提供的需求和上下文，给出专业的生产管理建议。\n" +
                    "回答应包含：现状分析、排程方案、产能评估、物料需求、风险点与应对措施。回答请使用中文。";

    private final WorkerAgentRegistry registry;

    public ProductionWorker(ChatClientHelper chatClientHelper, WorkerAgentRegistry registry) {
        super(chatClientHelper);
        this.registry = registry;
    }

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
        return Arrays.asList("wms_inventory_list", "qms_inspection_order_list");
    }

    @Override
    public AgentResult execute(AgentTask task, Long tenantId) {
        long startTime = System.currentTimeMillis();
        try {
            String userMessage = buildUserMessage(task);
            String output = callLlm(DEFAULT_SYSTEM_PROMPT, userMessage);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(true)
                    .output(output)
                    .tokensUsed(estimateTokens(output))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("[execute][生产任务执行失败 taskId={}]", task.getTaskId(), e);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg(StrUtil.sub(e.getMessage(), 0, 500))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private String buildUserMessage(AgentTask task) {
        StringBuilder sb = new StringBuilder();
        sb.append("请完成以下生产任务：\n").append(task.getDescription());
        if (task.getParameters() != null && !task.getParameters().isEmpty()) {
            sb.append("\n\n【任务参数】\n");
            task.getParameters().forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        }
        return sb.toString();
    }

    private int estimateTokens(String text) {
        return StrUtil.isBlank(text) ? 0 : text.length() / 4;
    }

}

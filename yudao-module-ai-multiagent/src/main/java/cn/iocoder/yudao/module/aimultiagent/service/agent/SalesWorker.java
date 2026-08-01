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
 * 销售 Worker
 *
 * <p>负责销售域任务：客户需求分析、报价建议、销售策略、订单风险评估等。
 * 启动时自动注册到 {@link WorkerAgentRegistry}。
 *
 * @author yudao
 */
@Component
@Slf4j
public class SalesWorker extends AbstractWorkerAgent {

    private static final String WORKER_NAME = "sales-worker";
    private static final String WORKER_DESCRIPTION =
            "销售域 Worker，擅长客户需求分析、报价建议、销售策略制定、订单风险评估";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一名资深销售顾问。请基于用户提供的需求和上下文，给出专业的销售建议。\n" +
                    "回答应包含：客户需求理解、推荐方案、报价建议、潜在风险与应对策略。回答请使用中文。";

    private final WorkerAgentRegistry registry;

    public SalesWorker(ChatClientHelper chatClientHelper, WorkerAgentRegistry registry) {
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
        return Arrays.asList("wms_shipment_order_list", "wms_merchant_list");
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
            log.error("[execute][销售任务执行失败 taskId={}]", task.getTaskId(), e);
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
        sb.append("请完成以下销售任务：\n").append(task.getDescription());
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

package cn.zhicloud.module.aimultiagent.service.agent;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.aimultiagent.config.ChatClientHelper;
import cn.zhicloud.module.aimultiagent.model.AgentResult;
import cn.zhicloud.module.aimultiagent.model.AgentTask;
import cn.zhicloud.module.aimultiagent.service.llm.LlmGateway;
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
 * @author zhicloud
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
    private final WorkerToolExecutor workerToolExecutor;

    public SalesWorker(ChatClientHelper chatClientHelper, WorkerAgentRegistry registry,
                       WorkerToolExecutor workerToolExecutor, LlmGateway llmGateway) {
        super(chatClientHelper, llmGateway);
        this.registry = registry;
        this.workerToolExecutor = workerToolExecutor;
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
            RealToolResult ctx = buildPromptWithRealTools(task, tenantId, workerToolExecutor, "销售");
            String output = callLlm(DEFAULT_SYSTEM_PROMPT, ctx.prompt);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(!ctx.allFailed)
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

}

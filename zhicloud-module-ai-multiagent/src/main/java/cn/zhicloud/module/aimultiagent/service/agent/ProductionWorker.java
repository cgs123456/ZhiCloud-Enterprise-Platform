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
 * 生产 Worker
 *
 * <p>负责生产域任务：生产排程、产能评估、物料需求分析、生产异常处理等。
 * 启动时自动注册到 {@link WorkerAgentRegistry}。
 *
 * @author zhicloud
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
    private final WorkerToolExecutor workerToolExecutor;

    public ProductionWorker(ChatClientHelper chatClientHelper, WorkerAgentRegistry registry,
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
        return Arrays.asList("wms_inventory_list", "qms_inspection_order_list");
    }

    @Override
    public AgentResult execute(AgentTask task, Long tenantId) {
        long startTime = System.currentTimeMillis();
        try {
            RealToolResult ctx = buildPromptWithRealTools(task, tenantId, workerToolExecutor, "生产");
            String output = callLlm(DEFAULT_SYSTEM_PROMPT, ctx.prompt);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(!ctx.allFailed)
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

}

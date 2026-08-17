package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.model.AgentResult;
import cn.iocoder.yudao.module.aimultiagent.service.llm.LlmGateway;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTask;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 采购 Worker
 *
 * <p>负责采购域任务：供应商评估、采购建议、价格谈判策略、采购风险控制等。
 * 启动时自动注册到 {@link WorkerAgentRegistry}。
 *
 * @author yudao
 */
@Component
@Slf4j
public class ProcurementWorker extends AbstractWorkerAgent {

    private static final String WORKER_NAME = "procurement-worker";
    private static final String WORKER_DESCRIPTION =
            "采购域 Worker，擅长供应商评估、采购建议、价格谈判策略、采购风险控制";
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一名资深采购顾问。请基于用户提供的需求和上下文，给出专业的采购建议。\n" +
                    "回答应包含：供应商评估、采购策略、价格谈判要点、风险识别与控制措施。回答请使用中文。";

    private final WorkerAgentRegistry registry;
    private final WorkerToolExecutor workerToolExecutor;

    public ProcurementWorker(ChatClientHelper chatClientHelper, WorkerAgentRegistry registry,
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
        return Arrays.asList("wms_receipt_order_list", "wms_merchant_list");
    }

    @Override
    public AgentResult execute(AgentTask task, Long tenantId) {
        long startTime = System.currentTimeMillis();
        try {
            RealToolResult ctx = buildPromptWithRealTools(task, tenantId, workerToolExecutor, "采购");
            String output = callLlm(DEFAULT_SYSTEM_PROMPT, ctx.prompt);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(!ctx.allFailed)
                    .output(output)
                    .tokensUsed(estimateTokens(output))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("[execute][采购任务执行失败 taskId={}]", task.getTaskId(), e);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg(StrUtil.sub(e.getMessage(), 0, 500))
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

}

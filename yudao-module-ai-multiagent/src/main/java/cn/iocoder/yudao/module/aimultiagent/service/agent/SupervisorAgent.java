package cn.iocoder.yudao.module.aimultiagent.service.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.model.AgentResult;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTask;
import cn.iocoder.yudao.module.aimultiagent.model.AgentTopology;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Supervisor Agent 实现
 *
 * 负责任务拆解（planTasks）和结果汇总（summarize）：
 *  1. {@link #planTasks} — 构造系统提示词，让 LLM 输出 JSON 格式的任务列表，解析为 {@link AgentTask}；
 *  2. {@link #summarize} — 构造包含所有 Worker 结果的上下文，让 LLM 生成最终答案。
 *
 * @author yudao
 */
@Component
@Slf4j
public class SupervisorAgent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ChatClientHelper chatClientHelper;

    public SupervisorAgent(ChatClientHelper chatClientHelper) {
        this.chatClientHelper = chatClientHelper;
    }

    /**
     * 任务拆解
     *
     * 构造系统提示词，让 LLM 基于 {@link AgentTopology} 中的 Worker 列表，
     * 将用户输入拆解为 JSON 格式的任务列表，并解析为 {@link AgentTask}。
     *
     * @param userInput 用户输入
     * @param topology  拓扑配置
     * @return 拆解后的任务列表
     */
    public List<AgentTask> planTasks(String userInput, AgentTopology topology) {
        // 1. 构造系统提示词
        String systemPrompt = buildPlanSystemPrompt(topology);

        // 2. 调用 LLM
        String llmOutput = callLlm(systemPrompt, userInput);

        // 3. 解析 JSON 任务列表
        return parseTasks(llmOutput);
    }

    /**
     * 结果汇总
     *
     * 构造包含所有 Worker 结果的上下文，让 LLM 生成最终答案。
     *
     * @param userInput 用户原始输入
     * @param results   Worker 执行结果列表
     * @return 最终汇总答案
     */
    public String summarize(String userInput, List<AgentResult> results) {
        // 1. 构造系统提示词 + 上下文
        String systemPrompt = buildSummarizeSystemPrompt();
        String context = buildSummarizeContext(userInput, results);

        // 2. 调用 LLM 生成最终答案
        return callLlm(systemPrompt, context);
    }

    // ==================== 内部方法 ====================

    /**
     * 构造任务拆解系统提示词
     */
    private String buildPlanSystemPrompt(AgentTopology topology) {
        StringBuilder workerDesc = new StringBuilder();
        if (CollUtil.isNotEmpty(topology.getWorkers())) {
            for (AgentTopology.WorkerConfig worker : topology.getWorkers()) {
                workerDesc.append(StrUtil.format("- name: {}\n  description: {}\n  tools: {}\n",
                        worker.getName(),
                        worker.getDescription(),
                        CollUtil.isEmpty(worker.getTools()) ? "[]" : worker.getTools()));
            }
        }
        String customPrompt = StrUtil.blankToDefault(topology.getSupervisorSystemPrompt(), "");

        return StrUtil.format(
                "{}\n\n" +
                        "你是一个多 Agent 编排系统中的 Supervisor，负责将用户的请求拆解为多个子任务，并分配给合适的 Worker 执行。\n\n" +
                        "【可用 Worker 列表】\n{}\n\n" +
                        "【输出要求】\n" +
                        "请严格输出 JSON 数组格式，不要包含 markdown 代码块标记（```json），每个元素包含以下字段：\n" +
                        "  - taskId: 任务 ID，字符串，如 \"task-1\"\n" +
                        "  - description: 任务描述，字符串，详细说明 Worker 需要完成的工作\n" +
                        "  - assignedWorker: 分配的 Worker 名称，必须从上方可用 Worker 列表中选择\n" +
                        "  - requiredTools: 需要的工具名称列表，数组\n\n" +
                        "【输出示例】\n" +
                        "[{{\"taskId\":\"task-1\",\"description\":\"查询库存\",\"assignedWorker\":\"report-writer\",\"requiredTools\":[]}}]\n",
                customPrompt, workerDesc.toString());
    }

    /**
     * 构造结果汇总系统提示词
     */
    private String buildSummarizeSystemPrompt() {
        return "你是一个多 Agent 编排系统中的 Supervisor，负责汇总各 Worker 的执行结果，生成最终答案。\n\n" +
                "请基于用户原始输入和各 Worker 的执行结果，综合生成一份清晰、完整的最终回答。\n" +
                "如果某些 Worker 执行失败，请在回答中说明受影响的部分。回答请使用中文。";
    }

    /**
     * 构造结果汇总上下文
     */
    private String buildSummarizeContext(String userInput, List<AgentResult> results) {
        StringBuilder context = new StringBuilder();
        context.append("【用户原始输入】\n").append(userInput).append("\n\n");
        context.append("【Worker 执行结果】\n");
        if (CollUtil.isEmpty(results)) {
            context.append("（无 Worker 执行结果）\n");
        } else {
            for (AgentResult result : results) {
                context.append(StrUtil.format("--- 任务 {} ---\n", result.getTaskId()));
                if (result.isSuccess()) {
                    context.append("状态: 成功\n");
                    context.append("输出: ").append(StrUtil.blankToDefault(result.getOutput(), "(空)")).append("\n\n");
                } else {
                    context.append("状态: 失败\n");
                    context.append("错误: ").append(StrUtil.blankToDefault(result.getErrorMsg(), "(未知错误)")).append("\n\n");
                }
            }
        }
        context.append("请基于以上信息，生成最终的综合回答。");
        return context.toString();
    }

    /**
     * 解析 LLM 输出为任务列表
     */
    private List<AgentTask> parseTasks(String llmOutput) {
        if (StrUtil.isBlank(llmOutput)) {
            log.warn("[parseTasks][LLM 输出为空，返回空任务列表]");
            return Collections.emptyList();
        }
        // 清理可能存在的 markdown 代码块标记
        String json = llmOutput.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        try {
            List<AgentTask> tasks = OBJECT_MAPPER.readValue(json, new TypeReference<List<AgentTask>>() {});
            log.info("[parseTasks][任务拆解完成，共 {} 个子任务]", tasks.size());
            return tasks;
        } catch (Exception e) {
            log.error("[parseTasks][JSON 解析失败，output={}]", json, e);
            // 解析失败时，返回单个兜底任务
            AgentTask fallback = AgentTask.builder()
                    .taskId("task-fallback")
                    .description(llmOutput)
                    .assignedWorker(null)
                    .requiredTools(Collections.emptyList())
                    .build();
            List<AgentTask> fallbackList = new ArrayList<>();
            fallbackList.add(fallback);
            return fallbackList;
        }
    }

    /**
     * 调用 LLM
     */
    private String callLlm(String systemPrompt, String userMessage) {
        ChatClient chatClient = chatClientHelper.getChatClient();
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

}

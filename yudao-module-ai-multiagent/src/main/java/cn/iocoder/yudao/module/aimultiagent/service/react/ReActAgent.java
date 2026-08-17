package cn.iocoder.yudao.module.aimultiagent.service.react;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.aimultiagent.config.ChatClientHelper;
import cn.iocoder.yudao.module.aimultiagent.config.MultiAgentProperties;
import cn.iocoder.yudao.module.aimultiagent.service.llm.LlmGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * ReAct Agent 实现
 *
 * 基于 Spring AI 1.1.8 的 ChatClient 实现 ReAct（Reasoning + Acting）循环：
 *  1. Thought — LLM 推理出下一步该做什么
 *  2. Action  — 选择一个工具调用，或输出 Final Answer
 *  3. Observation — 工具执行结果反馈给 LLM
 *  4. 循环 1-3 直到输出 Final Answer，或触发步数 / Token / 超时熔断
 *
 * 系统提示词参考 ReAct 论文（https://arxiv.org/abs/2210.03629），
 * 要求 LLM 严格输出 JSON：{"thought":"...","action":"...","actionInput":"..."}
 *
 * 工具来源：
 *  - {@link ToolCallbackProvider}（Spring AI 自动收集 @Tool 方法）
 *  - {@link ToolCallback} List（来自 yudao-module-ai 的 AiAutoConfiguration 注册的 MCP / 业务工具）
 *
 * 容器启动安全：
 *  - {@link ChatClient} 通过 {@code @Autowired(required=false)} 注入，无 LLM API key 时不影响启动
 *  - {@link ChatClientHelper} 作为兜底，运行时按需构建 ChatClient
 *  - 所有工具相关 Bean 均为可选注入
 *
 * @author yudao
 */
@Component
@Slf4j
public class ReActAgent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Final Answer 标识（不区分大小写匹配）
     */
    private static final String FINAL_ANSWER = "Final Answer";

    /**
     * 默认步数上限
     */
    private static final int DEFAULT_MAX_STEPS = 10;

    /**
     * 默认 Token 预算
     */
    private static final int DEFAULT_MAX_TOKEN_BUDGET = 4000;

    /**
     * 默认超时（秒）
     */
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * ChatClient Bean（yudao-module-ai 未直接提供时为 null）
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * ChatClient 兜底助手（基于 AiModelService 按需构建）
     */
    @Autowired(required = false)
    private ChatClientHelper chatClientHelper;

    @Autowired
    private LlmGateway llmGateway;

    @Autowired(required = false)
    private MultiAgentProperties properties;

    /**
     * Spring AI 自动注册的 ToolCallbackProvider（如 MethodToolCallbackProvider）
     */
    @Autowired(required = false)
    private List<ToolCallbackProvider> toolCallbackProviders;

    /**
     * yudao-module-ai AiAutoConfiguration 注册的 ToolCallback 列表（MCP / 业务工具）
     */
    @Autowired(required = false)
    private List<ToolCallback> toolCallbacks;

    /**
     * 执行 ReAct 循环（使用默认参数）
     *
     * @param userInput 用户输入
     * @return 执行结果
     */
    public ReActResult run(String userInput) {
        int maxSteps = properties != null ? properties.getReact().getMaxSteps() : DEFAULT_MAX_STEPS;
        int maxTokenBudget = properties != null ? properties.getReact().getMaxTokenBudget() : DEFAULT_MAX_TOKEN_BUDGET;
        int timeoutSeconds = properties != null ? properties.getReact().getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
        return run(userInput, maxSteps, maxTokenBudget, timeoutSeconds);
    }

    /**
     * 执行 ReAct 循环
     *
     * @param userInput       用户输入
     * @param maxSteps        最大步数
     * @param maxTokenBudget  Token 预算上限
     * @param timeoutSeconds  超时秒数
     * @return 执行结果
     */
    public ReActResult run(String userInput, int maxSteps, int maxTokenBudget, int timeoutSeconds) {
        if (StrUtil.isBlank(userInput)) {
            return ReActResult.failure("用户输入不能为空", new ArrayList<>(), 0);
        }
        // 1. 获取 ChatClient
        ChatClient client = obtainChatClient();
        if (client == null) {
            return ReActResult.failure("ChatClient 不可用，请先配置 LLM API Key", new ArrayList<>(), 0);
        }
        // 2. 收集可用工具
        List<ToolCallback> tools = collectToolCallbacks();
        log.info("[run][开始 ReAct 循环，userInput={}, tools={}, maxSteps={}, tokenBudget={}, timeout={}s]",
                StrUtil.sub(userInput, 0, 100), tools.size(), maxSteps, maxTokenBudget, timeoutSeconds);
        // 3. 构造系统提示词
        String systemPrompt = buildSystemPrompt(tools);
        // 4. 执行 ReAct 循环
        long startTime = System.currentTimeMillis();
        long deadlineNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
        long startNanos = System.nanoTime();
        List<ReActStep> steps = new ArrayList<>();
        int totalTokenUsage = 0;
        // 上下文：累积 Thought / Action / Action Input / Observation
        StringBuilder scratchpad = new StringBuilder();
        scratchpad.append("Question: ").append(userInput).append("\n\n");
        for (int stepIndex = 0; stepIndex < maxSteps; stepIndex++) {
            // 4.1 超时检查
            if (System.nanoTime() - startNanos > deadlineNanos) {
                log.warn("[run][执行超时，已完成步数={}]", steps.size());
                return ReActResult.failure("执行超时（" + timeoutSeconds + "s）", steps, totalTokenUsage);
            }
            // 4.2 Token 预算检查
            if (totalTokenUsage > maxTokenBudget) {
                log.warn("[run][Token 预算超限，total={}, budget={}]", totalTokenUsage, maxTokenBudget);
                return ReActResult.failure("Token 预算超限（已消耗 " + totalTokenUsage + " > 上限 " + maxTokenBudget + "）",
                        steps, totalTokenUsage);
            }
            // 4.3 调用 LLM 进行思考（经 LlmGateway：注入防护 + 重试 + 限流 + 熔断 + 指标）
            String llmOutput;
            try {
                llmOutput = llmGateway.call(client, systemPrompt, scratchpad.toString(), "react", timeoutSeconds);
            } catch (Exception e) {
                log.error("[run][LLM 调用失败，step={}]", stepIndex, e);
                return ReActResult.failure("LLM 调用失败：" + StrUtil.sub(e.getMessage(), 0, 200),
                        steps, totalTokenUsage);
            }
            int stepTokens = llmGateway.estimateTokens(llmOutput);
            totalTokenUsage += stepTokens;
            // 4.4 解析 LLM 输出为 JSON
            ParsedAction parsed;
            try {
                parsed = parseLlmOutput(llmOutput);
            } catch (Exception e) {
                log.error("[run][LLM 输出 JSON 解析失败，step={}, output={}]", stepIndex,
                        StrUtil.sub(llmOutput, 0, 200), e);
                // 解析失败：将原始输出作为 Observation 反馈给 LLM，让其修正
                String errorMsg = "JSON 解析失败：" + StrUtil.sub(e.getMessage(), 0, 100);
                ReActStep failStep = new ReActStep(llmOutput, null, null, errorMsg, stepTokens);
                steps.add(failStep);
                scratchpad.append("Thought: ").append(StrUtil.nullToDefault(llmOutput, "(空)")).append("\n")
                        .append("Observation: ").append(errorMsg)
                        .append("。请严格按 JSON 格式输出：{\"thought\":\"...\",\"action\":\"...\",\"actionInput\":\"...\"}\n\n");
                continue;
            }
            // 4.5 检查是否为 Final Answer
            if (isFinalAnswer(parsed.action())) {
                ReActStep finalStep = new ReActStep(parsed.thought(), parsed.action(),
                        parsed.actionInput(), null, stepTokens);
                steps.add(finalStep);
                log.info("[run][ReAct 循环成功，步数={}, tokens={}, 耗时={}ms]",
                        steps.size(), totalTokenUsage, System.currentTimeMillis() - startTime);
                return ReActResult.success(parsed.actionInput(), steps, totalTokenUsage);
            }
            // 4.6 执行工具调用
            String observation;
            try {
                observation = executeTool(parsed.action(), parsed.actionInput(), tools);
            } catch (Exception e) {
                log.error("[run][工具调用异常，action={}, input={}]", parsed.action(),
                        StrUtil.sub(parsed.actionInput(), 0, 100), e);
                observation = "工具调用异常：" + StrUtil.sub(e.getMessage(), 0, 200);
            }
            ReActStep stepRecord = new ReActStep(parsed.thought(), parsed.action(),
                    parsed.actionInput(), observation, stepTokens);
            steps.add(stepRecord);
            // 4.7 累积上下文
            scratchpad.append("Thought: ").append(StrUtil.nullToDefault(parsed.thought(), "(空)")).append("\n")
                    .append("Action: ").append(parsed.action()).append("\n")
                    .append("Action Input: ").append(parsed.actionInput()).append("\n")
                    .append("Observation: ").append(StrUtil.nullToDefault(observation, "(空)")).append("\n\n");
        }
        // 5. 步数超限
        log.warn("[run][步数超限，maxSteps={}]", maxSteps);
        return ReActResult.failure("步数超限（" + maxSteps + "）", steps, totalTokenUsage);
    }

    // ==================== 内部方法 ====================

    /**
     * 获取 ChatClient：优先使用容器注入的 Bean，否则通过 ChatClientHelper 构建
     */
    private ChatClient obtainChatClient() {
        if (chatClient != null) {
            return chatClient;
        }
        if (chatClientHelper != null && chatClientHelper.isAvailable()) {
            try {
                return chatClientHelper.getChatClient();
            } catch (Exception e) {
                log.warn("[obtainChatClient][通过 ChatClientHelper 获取失败：{}]", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 收集所有可用的 ToolCallback
     */
    private List<ToolCallback> collectToolCallbacks() {
        Map<String, ToolCallback> toolMap = new LinkedHashMap<>();
        // 1. 来自 ToolCallbackProvider（Spring AI 自动注册的 @Tool 方法）
        if (CollUtil.isNotEmpty(toolCallbackProviders)) {
            for (ToolCallbackProvider provider : toolCallbackProviders) {
                if (provider == null) {
                    continue;
                }
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks == null) {
                    continue;
                }
                for (ToolCallback cb : callbacks) {
                    if (cb == null) {
                        continue;
                    }
                    toolMap.putIfAbsent(getToolName(cb), cb);
                }
            }
        }
        // 2. 来自 List<ToolCallback>（AiAutoConfiguration 注册的 MCP / 业务工具）
        if (CollUtil.isNotEmpty(toolCallbacks)) {
            for (ToolCallback cb : toolCallbacks) {
                if (cb == null) {
                    continue;
                }
                toolMap.putIfAbsent(getToolName(cb), cb);
            }
        }
        return new ArrayList<>(toolMap.values());
    }

    /**
     * 安全获取工具名称
     */
    private String getToolName(ToolCallback cb) {
        try {
            ToolDefinition def = cb.getToolDefinition();
            if (def != null && StrUtil.isNotBlank(def.name())) {
                return def.name();
            }
        } catch (Exception e) {
            log.warn("[getToolName][获取工具名称失败：{}]", e.getMessage());
        }
        return cb.getClass().getSimpleName() + "@" + System.identityHashCode(cb);
    }

    /**
     * 构造 ReAct 系统提示词
     */
    private String buildSystemPrompt(List<ToolCallback> tools) {
        StringBuilder toolDesc = new StringBuilder();
        if (CollUtil.isEmpty(tools)) {
            toolDesc.append("（无可用工具，请直接给出 Final Answer）\n");
        } else {
            for (ToolCallback cb : tools) {
                String name = getToolName(cb);
                String description = "";
                String inputSchema = "";
                try {
                    ToolDefinition def = cb.getToolDefinition();
                    if (def != null) {
                        description = StrUtil.nullToDefault(def.description(), "");
                        inputSchema = StrUtil.nullToDefault(def.inputSchema(), "");
                    }
                } catch (Exception ignored) {
                    // 忽略，使用默认值
                }
                toolDesc.append("- ").append(name).append(": ").append(description).append("\n");
                if (StrUtil.isNotBlank(inputSchema)) {
                    toolDesc.append("  inputSchema: ").append(inputSchema).append("\n");
                }
            }
        }
        // 使用 Hutool 的 Map 重载：{tools} / {tool_descriptions} 为具名占位符，
        // 不能用变参重载（{}/{} 位置占位符），否则工具清单永远不会注入提示词。
        Map<String, Object> promptParams = new LinkedHashMap<>();
        promptParams.put("tools", toolDesc.toString());
        promptParams.put("tool_descriptions", toolDesc.toString());
        return StrUtil.format("""
                You are a ReAct agent. Answer the user's question using the following tools:
                {tools}

                Format your response as JSON (do NOT wrap in markdown code blocks):
                {"thought": "your reasoning", "action": "tool name or Final Answer", "actionInput": "input for the tool"}

                Rules:
                1. Each step MUST be a single valid JSON object on its own.
                2. If you know the final answer, set "action" to "Final Answer" and put the answer in "actionInput".
                3. "actionInput" for a tool must be a JSON string matching the tool's inputSchema.
                4. After each tool call, you will receive an Observation to inform your next thought.

                Available tools:
                {tool_descriptions}
                """, promptParams);
    }

    /**
     * 解析 LLM 输出为 ParsedAction
     */
    private ParsedAction parseLlmOutput(String llmOutput) {
        if (StrUtil.isBlank(llmOutput)) {
            throw new IllegalArgumentException("LLM 输出为空");
        }
        // 1. 清理 markdown 代码块标记
        String json = llmOutput.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "").trim();
        }
        // 2. 提取第一个 JSON 对象（LLM 可能在 JSON 前后输出多余文本）
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            json = json.substring(start, end + 1);
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String thought = textOrNull(node, "thought");
            String action = textOrNull(node, "action");
            String actionInput = textOrNull(node, "actionInput");
            if (StrUtil.isBlank(action)) {
                throw new IllegalArgumentException("JSON 缺少 action 字段");
            }
            // actionInput 为空时，兼容为空字符串
            if (actionInput == null) {
                actionInput = "";
            }
            return new ParsedAction(thought, action, actionInput);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 JSON 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 安全读取 JSON 字段为文本（兼容字符串 / 对象 / 数字）
     */
    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        if (child.isTextual()) {
            return child.asText();
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(child);
        } catch (Exception e) {
            return child.toString();
        }
    }

    /**
     * 判断是否为 Final Answer（不区分大小写）
     */
    private boolean isFinalAnswer(String action) {
        return action != null && FINAL_ANSWER.equalsIgnoreCase(action.trim());
    }

    /**
     * 执行工具调用
     */
    private String executeTool(String action, String actionInput, List<ToolCallback> tools) {
        if (StrUtil.isBlank(action)) {
            return "工具名为空";
        }
        ToolCallback target = null;
        for (ToolCallback cb : tools) {
            if (action.equals(getToolName(cb))) {
                target = cb;
                break;
            }
        }
        if (target == null) {
            return "找不到工具：" + action;
        }
        // actionInput 必须为 JSON 字符串；若为空则使用 "{}"
        String input = StrUtil.blankToDefault(actionInput, "{}").trim();
        try {
            String result = target.call(input);
            log.info("[executeTool][action={}, input={}, result={}]", action,
                    StrUtil.sub(input, 0, 100), StrUtil.sub(result, 0, 200));
            return StrUtil.nullToDefault(result, "(空)");
        } catch (Exception e) {
            log.error("[executeTool][工具调用失败，action={}, input={}]", action, StrUtil.sub(input, 0, 100), e);
            return "工具调用失败：" + StrUtil.sub(e.getMessage(), 0, 200);
        }
    }

    /**
     * 解析后的 LLM 输出
     */
    private record ParsedAction(String thought, String action, String actionInput) {
    }

}

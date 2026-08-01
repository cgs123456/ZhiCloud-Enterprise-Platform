package cn.iocoder.yudao.module.airag.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG Advisor（P5：把向量检索注入 ChatClient 调用链）
 *
 * <p>本 Advisor 把 RAG 检索从 {@code AiragRagServiceImpl.chat()} 中抽离为可复用的 Advisor 链组件，
 * 任何挂载 RagAdvisor 的 ChatClient 调用，只要在 context 中传入 {@code knowledgeId}，
 * 即可自动获得向量检索增强。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从 {@link ChatClientRequest#context()} 读取 {@code knowledgeId}（key 可配置）</li>
 *   <li>若未传 knowledgeId，直接放行到下一个 Advisor（无 RAG 增强）</li>
 *   <li>提取 UserMessage 作为检索 query</li>
 *   <li>调用 {@link VectorStore#similaritySearch(SearchRequest)} 检索相关文档片段</li>
 *   <li>把检索结果拼接为系统提示词上下文，注入到 prompt 中</li>
 *   <li>调用下一个 Advisor（继续走 PiiMask / PromptInjection 等）</li>
 * </ol>
 *
 * <h3>Advisor 顺序</h3>
 * <p>Order=50，在 PromptInjectionAdvisor（100）和 PiiMaskAdvisor（200）之前执行，
 * 确保检索后的 prompt 也经过安全检测与脱敏。
 *
 * <h3>启用条件</h3>
 * <ul>
 *   <li>{@code yudao.airag.enabled=true}（AiragConfiguration 加载 VectorStore Bean）</li>
 *   <li>{@code yudao.airag.advisor.enabled=true}（本开关）</li>
 *   <li>容器中存在 {@link VectorStore} Bean</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * chatClient.prompt()
 *     .user(question)
 *     .advisors(a -&gt; a.param("knowledgeId", knowledgeId))
 *     .call()
 *     .content();
 * </pre>
 *
 * <p>同时实现 {@link CallAdvisor} 与 {@link StreamAdvisor}，覆盖同步与流式调用。
 * 流式场景下采用简化方案：直接放行到下游 chain，RAG 检索仍由同步路径 {@link #adviseCall} 处理，
 * 当流式调用挂载本 Advisor 时不会被阻断（避免接口未实现导致流式安全防护全失效）。
 *
 * @author yudao
 */
@Component
@ConditionalOnProperty(prefix = "yudao.airag.advisor", name = "enabled", havingValue = "true")
@ConditionalOnBean(VectorStore.class)
@Slf4j
public class RagAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 系统提示模板：注入检索上下文（启用引用溯源）
     *
     * <p>P0-2：要求 LLM 在回答中用 [N] 形式标注引用，并在末尾输出"信息来源"列表，
     * 列出每个 [N] 对应的文档名、页码与 chunk_id，便于前端做引用跳转与可信度展示。
     */
    private static final String SYSTEM_PROMPT_TEMPLATE_WITH_CITATION =
            "你是一个专业的知识库问答助手。请优先基于以下检索到的上下文回答用户问题。\n" +
                    "如果上下文中没有相关信息，请如实告知，并尝试基于通用知识回答。\n" +
                    "回答请使用中文。\n\n" +
                    "【引用规则】\n" +
                    "1. 在回答中引用检索上下文时，请用 [1][2]... 形式标注来源编号，编号对应下方【检索上下文】中的序号。\n" +
                    "2. 回答末尾必须追加一个【信息来源】列表，列出本次回答实际引用到的所有来源，格式如下：\n" +
                    "   【信息来源】\n" +
                    "   [1] 文档名 | 页码: N | chunk_id: xxx\n" +
                    "   [2] 文档名 | 页码: N | chunk_id: xxx\n" +
                    "3. 若未引用任何检索上下文（仅使用通用知识回答），可省略【信息来源】列表。\n" +
                    "4. 最多引用 {maxCitations} 个来源，避免冗长列表。\n\n" +
                    "【检索上下文】\n{context}\n\n【结束】";

    /**
     * 系统提示模板：注入检索上下文（关闭引用溯源，退化为原有行为）
     */
    private static final String SYSTEM_PROMPT_TEMPLATE_WITHOUT_CITATION =
            "你是一个专业的知识库问答助手。请优先基于以下检索到的上下文回答用户问题。\n" +
                    "如果上下文中没有相关信息，请如实告知，并尝试基于通用知识回答。\n" +
                    "回答请使用中文，并在末尾标注信息来源文档（若可识别）。\n\n" +
                    "【检索上下文】\n{context}\n\n【结束】";

    /**
     * 无上下文时的默认系统提示
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个专业的知识库问答助手。请基于用户问题，使用你所知道的知识进行回答。" +
                    "如果无法回答，请明确告知用户。回答请使用中文。";

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private AiragAdvisorProperties properties;

    /**
     * Advisor 名称，用于链路追踪与日志
     */
    @Override
    public String getName() {
        return "RagAdvisor";
    }

    /**
     * 执行顺序：50，在安全检测（100）/脱敏（200）之前执行
     *
     * <p>原因：检索后的 prompt 内容也需要经过注入检测和 PII 脱敏，
     * 避免检索到的文档中包含 PII 或注入模式被泄漏给 LLM。
     */
    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 从 context 读取 knowledgeId
        Long knowledgeId = extractKnowledgeId(request);
        if (knowledgeId == null) {
            // 业务方未传 knowledgeId，跳过 RAG 增强
            return chain.nextCall(request);
        }

        // 2. 提取用户问题
        String question = extractUserQuestion(request);
        if (question == null || question.isEmpty()) {
            return chain.nextCall(request);
        }

        // 3. 向量检索
        List<Document> documents;
        try {
            documents = retrieveDocuments(knowledgeId, question);
        } catch (Exception ex) {
            // 检索失败不应阻断业务，降级为无上下文调用
            log.warn("[adviseCall][向量检索失败，降级为无上下文 knowledgeId={}, err={}]",
                    knowledgeId, ex.toString());
            return chain.nextCall(request);
        }

        if (documents == null || documents.isEmpty()) {
            log.info("[adviseCall][未检索到相关文档 knowledgeId={}, questionHead={}]",
                    knowledgeId, truncate(question, 80));
            return chain.nextCall(request);
        }

        // 4. 构造增强 prompt
        ChatClientRequest enhancedRequest = enhanceRequestWithContext(request, documents);
        log.info("[adviseCall][RAG 增强成功 knowledgeId={}, hitDocs={}, questionHead={}]",
                knowledgeId, documents.size(), truncate(question, 80));
        return chain.nextCall(enhancedRequest);
    }

    /**
     * 流式调用入口（StreamAroundAdvisor 实现）
     *
     * <p>采用简化方案：直接放行到下游 chain，不在流式路径中做 RAG 检索。
     * RAG 检索仍由同步路径 {@link #adviseCall} 处理；流式调用方应自行确保
     * 调用前已通过同步链路完成检索增强，或在调用前把上下文写入 prompt。
     *
     * <p>实现此接口的目的：避免流式调用时本 Advisor 缺失 StreamAroundAdvisor
     * 导致整条流式链路被跳过，使下游安全 Advisor（PromptInjection/PiiMask）失效。
     *
     * @param request ChatClient 请求
     * @param chain   下游 Advisor 链
     * @return Flux 流式响应
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request);
    }

    /**
     * 从 ChatClientRequest.context() 读取 knowledgeId
     *
     * <p>支持 Long、Integer、String 等可转 Long 的类型。
     */
    private Long extractKnowledgeId(ChatClientRequest request) {
        Map<String, Object> context = request.context();
        if (context == null || context.isEmpty()) {
            return null;
        }
        Object value = context.get(properties.getContextKey());
        if (value == null) {
            return null;
        }
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            log.warn("[extractKnowledgeId][knowledgeId 格式非法 value={}]", value);
            return null;
        }
    }

    /**
     * 提取 ChatClientRequest 中的最后一条 UserMessage 文本
     */
    private String extractUserQuestion(ChatClientRequest request) {
        if (request.prompt() == null) {
            return null;
        }
        List<Message> messages = request.prompt().getInstructions();
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage um) {
                return um.getText();
            }
        }
        return null;
    }

    /**
     * 向量检索：按 knowledgeId + tenant_id 复合过滤 + 相似度检索
     *
     * <p>P0-2 修复：向量库不受 MyBatis Plus 多租户拦截器保护，必须显式按 tenant_id 过滤防止跨租户数据泄漏。
     */
    private List<Document> retrieveDocuments(Long knowledgeId, String question) {
        Long tenantId = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filter = builder.and(
                builder.eq("knowledge_id", knowledgeId),
                builder.eq("tenant_id", tenantId)
        ).build();
        SearchRequest searchRequest = SearchRequest.builder()
                .query(question)
                .topK(properties.getTopK())
                .similarityThreshold(properties.getSimilarityThreshold())
                .filterExpression(filter)
                .build();
        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 把检索到的文档拼接为系统提示，注入到 prompt 中
     *
     * <p>P0-2 引用溯源策略：
     * <ul>
     *   <li>开启 citationEnabled：每个 chunk 前加 {@code [N]} 编号，模板要求 LLM 用 [N] 标注引用并在末尾列出信息来源</li>
     *   <li>关闭 citationEnabled：退化为原有行为，仅拼接 chunk 文本</li>
     * </ul>
     *
     * <p>使用 {@code request.mutate().prompt(newPrompt).build()} 构造新请求。
     * 若 builder 失败，回退原始请求，避免影响主链路。
     */
    private ChatClientRequest enhanceRequestWithContext(ChatClientRequest request, List<Document> documents) {
        // 1. 拼接检索上下文（按 citation 开关选择不同模板）
        String contextText;
        String ragSystemPrompt;
        if (properties.isCitationEnabled()) {
            // 启用引用溯源：每个 chunk 前加 [N] 编号
            StringBuilder ctxBuilder = new StringBuilder();
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                ctxBuilder.append("[").append(i + 1).append("] ")
                        .append(formatChunkMetadata(doc)).append("\n")
                        .append(doc.getText()).append("\n\n---\n\n");
            }
            contextText = ctxBuilder.toString();
            ragSystemPrompt = SYSTEM_PROMPT_TEMPLATE_WITH_CITATION
                    .replace("{maxCitations}", String.valueOf(properties.getMaxCitations()))
                    .replace("{context}", contextText);
        } else {
            // 关闭引用溯源：退化为原有行为
            contextText = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));
            ragSystemPrompt = SYSTEM_PROMPT_TEMPLATE_WITHOUT_CITATION.replace("{context}", contextText);
        }

        // 2. 复制原 messages，在末尾追加 RAG 系统提示
        List<Message> originalMessages = request.prompt().getInstructions();
        List<Message> newMessages = new ArrayList<>(originalMessages.size() + 1);
        boolean hasOriginalSystem = false;
        for (Message msg : originalMessages) {
            if (msg instanceof SystemMessage sm) {
                // 把原 SystemMessage 内容与 RAG 上下文合并（避免多个 SystemMessage 冲突）
                String combined = sm.getText() + "\n\n" + ragSystemPrompt;
                newMessages.add(new SystemMessage(combined));
                hasOriginalSystem = true;
            } else {
                newMessages.add(msg);
            }
        }
        if (!hasOriginalSystem) {
            newMessages.add(0, new SystemMessage(ragSystemPrompt));
        }

        // 3. 构造新 Prompt
        try {
            Prompt newPrompt = new Prompt(newMessages, request.prompt().getOptions());
            return request.mutate().prompt(newPrompt).build();
        } catch (Throwable t) {
            log.warn("[enhanceRequestWithContext][构造增强 Prompt 失败，回退原始请求 err={}]",
                    t.toString());
            return request;
        }
    }

    /**
     * 格式化 chunk 元数据为可读字符串（用于在上下文中标注来源）
     *
     * <p>格式：{@code 文档名 | 页码: N | chunk_id: xxx}
     * <p>若 metadata 缺失某字段，会用占位符 "-" 替代，避免 NPE。
     */
    private String formatChunkMetadata(Document doc) {
        Map<String, Object> meta = doc.getMetadata();
        String docName = meta != null ? String.valueOf(meta.getOrDefault("document_name", "-")) : "-";
        Object pageObj = meta != null ? meta.get("page") : null;
        String page = pageObj != null ? String.valueOf(pageObj) : "-";
        String chunkId = meta != null ? String.valueOf(meta.getOrDefault("chunk_id", doc.getId())) : String.valueOf(doc.getId());
        return docName + " | 页码: " + page + " | chunk_id: " + chunkId;
    }

    /**
     * 截断字符串便于日志展示
     */
    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

}

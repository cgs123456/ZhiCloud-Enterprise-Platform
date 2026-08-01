package cn.iocoder.yudao.module.airag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI RAG Advisor 配置类
 *
 * <p>对应 yudao-module-ai-rag 的 Advisor 链集成（P5：RAG Advisor 集成）。
 * 通过 {@code yudao.airag.advisor.*} 控制 RagAdvisor 的行为。
 *
 * <h3>配置项</h3>
 * <ul>
 *   <li>{@code enabled}：是否启用 RagAdvisor（默认 false，需同时满足 VectorStore Bean 存在）</li>
 *   <li>{@code top-k}：检索返回的文档片段数量，默认 4</li>
 *   <li>{@code similarity-threshold}：相似度阈值（0.0~1.0），低于此值的文档不返回，默认 0.5</li>
 *   <li>{@code context-key}：从 ChatClientRequest.context() 读取 knowledgeId 的 key，默认 "knowledgeId"</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * yudao:
 *   airag:
 *     enabled: true                # 启用向量库（AiragConfiguration）
 *     advisor:
 *       enabled: true              # 启用 RagAdvisor
 *       top-k: 4
 *       similarity-threshold: 0.5
 *       context-key: knowledgeId
 * </pre>
 *
 * <p>业务方在调用 ChatClient 时通过 advisor 参数传入 knowledgeId：
 * <pre>
 * chatClient.prompt()
 *     .user(question)
 *     .advisors(advisor -&gt; advisor.param("knowledgeId", 123L))
 *     .call();
 * </pre>
 *
 * @author yudao
 */
@Configuration
@ConfigurationProperties(prefix = "yudao.airag.advisor")
@Data
public class AiragAdvisorProperties {

    /**
     * 是否启用 RagAdvisor
     *
     * <p>启用条件：{@code yudao.airag.enabled=true}（VectorStore Bean 存在） + 本开关为 true。
     * 默认关闭，避免无向量库环境下加载失败。
     */
    private boolean enabled = false;

    /**
     * 检索返回的文档片段数量（topK）
     *
     * <p>值越大检索越全但 LLM 上下文越长、成本越高。建议 3~8。
     */
    private int topK = 4;

    /**
     * 相似度阈值（0.0~1.0）
     *
     * <p>低于此值的文档片段不返回。COSINE_DISTANCE 模式下，Spring AI 会自动转换为距离过滤。
     */
    private double similarityThreshold = 0.5;

    /**
     * 从 ChatClientRequest.context() 读取 knowledgeId 的 key
     *
     * <p>业务方调用时通过 {@code .advisors(a -> a.param(contextKey, knowledgeId))} 传入。
     */
    private String contextKey = "knowledgeId";

    /**
     * 是否启用引用溯源（P0-2）
     *
     * <p>启用后，检索到的每个 chunk 会按序编号（[1][2]...），并在系统提示中要求
     * LLM 在回答时以 [N] 形式标注引用，回答末尾追加"信息来源"列表（chunk_id+文档名+页码）。
     * <p>关闭时退化为原有行为（仅注入上下文，不要求引用标注）。
     */
    private boolean citationEnabled = true;

    /**
     * 单次回答最多引用的来源数量上限
     *
     * <p>避免 LLM 输出过长的来源列表。默认 5。
     */
    private int maxCitations = 5;

}

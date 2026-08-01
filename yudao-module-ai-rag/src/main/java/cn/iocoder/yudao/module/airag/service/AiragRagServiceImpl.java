package cn.iocoder.yudao.module.airag.service;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * AI RAG 检索 Service 实现类
 *
 * <p>整合向量检索、混合检索（BM25+向量）与 Reranker 重排序：
 * <ol>
 *   <li>当 {@code yudao.airag.hybrid-search.enabled=true}：调用 {@link HybridSearchService} 混合检索
 *       （内部已包含 RRF 融合 + Reranker）</li>
 *   <li>否则执行纯向量检索，若 {@code yudao.airag.reranker.enabled=true} 再调用 Reranker 重排序</li>
 * </ol>
 *
 * @author yudao
 */
@Service
@ConditionalOnProperty(prefix = "yudao.airag", name = "enabled", havingValue = "true")
@ConditionalOnBean(VectorStore.class)
@Slf4j
public class AiragRagServiceImpl implements AiragRagService {

    @Autowired
    private VectorStore vectorStore;

    @Autowired(required = false)
    private HybridSearchService hybridSearchService;

    @Autowired(required = false)
    private RerankerService rerankerService;

    @Value("${yudao.airag.hybrid-search.enabled:false}")
    private boolean hybridSearchEnabled;

    @Value("${yudao.airag.reranker.enabled:false}")
    private boolean rerankerEnabled;

    @Override
    public List<Document> retrieve(String query, Long knowledgeId, int topK, double similarityThreshold) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        // 1. 混合检索（已内置 RRF 融合 + Reranker）
        if (hybridSearchEnabled && hybridSearchService != null) {
            try {
                return hybridSearchService.hybridSearch(query, knowledgeId, topK);
            } catch (Exception ex) {
                log.warn("[retrieve][混合检索异常，降级为纯向量检索 err={}]", ex.toString());
            }
        }

        // 2. 纯向量检索
        List<Document> documents = vectorSearch(query, knowledgeId, topK, similarityThreshold);
        if (CollUtil.isEmpty(documents)) {
            return documents;
        }

        // 3. Reranker 重排序（若启用且未走混合检索路径）
        if (rerankerEnabled && rerankerService != null) {
            try {
                documents = rerankerService.rerank(query, documents, topK);
            } catch (Exception ex) {
                log.warn("[retrieve][Reranker 异常，使用向量检索结果 err={}]", ex.toString());
            }
        }
        return documents;
    }

    /**
     * 向量相似度检索
     */
    private List<Document> vectorSearch(String query, Long knowledgeId, int topK, double similarityThreshold) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold);
        if (knowledgeId != null) {
            Filter.Expression filter = new FilterExpressionBuilder()
                    .eq("knowledge_id", knowledgeId).build();
            builder.filterExpression(filter);
        }
        try {
            return vectorStore.similaritySearch(builder.build());
        } catch (Exception ex) {
            log.warn("[vectorSearch][向量检索失败 err={}]", ex.toString());
            return Collections.emptyList();
        }
    }

}

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

import java.util.*;

/**
 * 混合检索 Service 实现类（BM25 + 向量，RRF 融合）
 *
 * <p>实现流程：
 * <ol>
 *   <li>并行执行 BM25（在向量召回的候选集上做关键词重排）+ 向量检索</li>
 *   <li>RRF 融合：score = Σ weight_i / (k + rank_i)</li>
 *   <li>调用 Reranker 对融合结果重排序</li>
 *   <li>返回 topK</li>
 * </ol>
 *
 * <p>权重配置：{@code yudao.airag.hybrid-search.vector-weight}（默认 0.7）、
 * {@code bm25-weight}（默认 0.3）、{@code rrf-k}（默认 60）。
 *
 * @author yudao
 */
@Service
@ConditionalOnProperty(prefix = "yudao.airag.hybrid-search", name = "enabled", havingValue = "true")
@ConditionalOnBean(VectorStore.class)
@Slf4j
public class HybridSearchServiceImpl implements HybridSearchService {

    /**
     * 候选集扩展倍数：向量检索先召回 topK * 该倍数，再融合重排
     */
    private static final int CANDIDATE_FACTOR = 4;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private Bm25SearchService bm25SearchService;

    @Autowired(required = false)
    private RerankerService rerankerService;

    @Value("${yudao.airag.hybrid-search.vector-weight:0.7}")
    private double vectorWeight;

    @Value("${yudao.airag.hybrid-search.bm25-weight:0.3}")
    private double bm25Weight;

    @Value("${yudao.airag.hybrid-search.rrf-k:60}")
    private int rrfK;

    @Override
    public List<Document> hybridSearch(String query, Long knowledgeId, int topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        int candidateK = topK * CANDIDATE_FACTOR;

        // 1. 向量检索（召回 candidateK 篇候选）
        // P0-2 修复：向量库不受 MyBatis Plus 多租户拦截器保护，必须显式按 tenant_id 过滤
        List<Document> vectorDocs;
        try {
            SearchRequest.Builder builder = SearchRequest.builder()
                    .query(query)
                    .topK(candidateK);
            // 复合过滤：knowledge_id + tenant_id
            Long tenantId = cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.getRequiredTenantId();
            FilterExpressionBuilder feb = new FilterExpressionBuilder();
            Filter.Expression filter;
            if (knowledgeId != null) {
                filter = feb.and(feb.eq("knowledge_id", knowledgeId), feb.eq("tenant_id", tenantId)).build();
            } else {
                filter = feb.eq("tenant_id", tenantId).build();
            }
            builder.filterExpression(filter);
            vectorDocs = vectorStore.similaritySearch(builder.build());
        } catch (Exception ex) {
            log.warn("[hybridSearch][向量检索失败，降级为 BM-only err={}]", ex.toString());
            vectorDocs = Collections.emptyList();
        }

        // 2. BM25 检索（在向量召回的候选集上做关键词重排）
        List<Document> bm25Docs;
        try {
            List<Document> candidates = CollUtil.isEmpty(vectorDocs) ? Collections.emptyList() : vectorDocs;
            bm25Docs = bm25SearchService.search(query, candidates, candidateK);
        } catch (Exception ex) {
            log.warn("[hybridSearch][BM25 检索失败，降级为向量-only err={}]", ex.toString());
            bm25Docs = Collections.emptyList();
        }

        // 3. RRF 融合
        List<Document> fused = rrfFusion(vectorDocs, bm25Docs);

        // 4. Reranker 重排序（若启用）
        if (rerankerService != null && !fused.isEmpty()) {
            try {
                fused = rerankerService.rerank(query, fused, topK);
            } catch (Exception ex) {
                log.warn("[hybridSearch][Reranker 异常，使用 RRF 融合结果 err={}]", ex.toString());
            }
        }

        // 5. 取 topK
        if (fused.size() > topK) {
            fused = fused.subList(0, topK);
        }
        log.info("[hybridSearch][query={}, vector={}, bm25={}, fused={}, return={}]",
                truncate(query, 60), vectorDocs.size(), bm25Docs.size(), fused.size(), Math.min(fused.size(), topK));
        return fused;
    }

    /**
     * RRF（Reciprocal Rank Fusion）融合算法
     *
     * <p>公式：score(d) = Σ weight_i / (k + rank_i(d))
     * <ul>
     *   <li>rank_i(d)：文档 d 在第 i 路检索结果中的排名（1-based）</li>
     *   <li>k：平滑常数（默认 60），降低高分文档的权重优势</li>
     * </ul>
     *
     * @param vectorDocs 向量检索结果（已按分数降序）
     * @param bm25Docs   BM25 检索结果（已按分数降序）
     * @return 融合后按 RRF 分数降序的文档列表
     */
    private List<Document> rrfFusion(List<Document> vectorDocs, List<Document> bm25Docs) {
        // 1. 构建文档 ID → 文档 的映射（去重）
        Map<String, Document> docMap = new LinkedHashMap<>();
        for (Document doc : vectorDocs) {
            docMap.putIfAbsent(doc.getId(), doc);
        }
        for (Document doc : bm25Docs) {
            docMap.putIfAbsent(doc.getId(), doc);
        }

        // 2. 计算每篇文档在各路检索中的排名
        Map<String, Double> rrfScores = new HashMap<>();
        for (Map.Entry<String, Integer> entry : buildRankMap(vectorDocs).entrySet()) {
            double score = vectorWeight / (rrfK + entry.getValue());
            rrfScores.merge(entry.getKey(), score, Double::sum);
        }
        for (Map.Entry<String, Integer> entry : buildRankMap(bm25Docs).entrySet()) {
            double score = bm25Weight / (rrfK + entry.getValue());
            rrfScores.merge(entry.getKey(), score, Double::sum);
        }

        // 3. 按 RRF 分数降序排序
        List<Document> result = new ArrayList<>(docMap.size());
        for (Map.Entry<String, Document> entry : docMap.entrySet()) {
            Document doc = entry.getValue();
            double rrfScore = rrfScores.getOrDefault(entry.getKey(), 0.0);
            Document scored = Document.builder()
                    .id(doc.getId())
                    .text(doc.getText())
                    .metadata(doc.getMetadata())
                    .score(rrfScore)
                    .build();
            result.add(scored);
        }
        result.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0));
        return result;
    }

    /**
     * 构建文档 ID → 排名（1-based）的映射
     */
    private Map<String, Integer> buildRankMap(List<Document> documents) {
        Map<String, Integer> rankMap = new HashMap<>();
        if (documents == null) {
            return rankMap;
        }
        for (int i = 0; i < documents.size(); i++) {
            String id = documents.get(i).getId();
            rankMap.putIfAbsent(id, i + 1);
        }
        return rankMap;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

}

package cn.iocoder.yudao.module.airag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * AI RAG 检索 Service
 *
 * <p>RAG 检索的统一入口，整合向量检索、混合检索（BM25+向量）与 Reranker 重排序能力：
 * <ul>
 *   <li>{@code yudao.airag.hybrid-search.enabled=true} → 使用混合检索</li>
 *   <li>{@code yudao.airag.reranker.enabled=true} → 检索后重排序</li>
 *   <li>两者均关闭 → 退化为纯向量检索</li>
 * </ul>
 *
 * @author yudao
 */
public interface AiragRagService {

    /**
     * 检索知识库相关文档
     *
     * @param query              用户查询
     * @param knowledgeId        知识库 ID（用于过滤检索范围）
     * @param topK               返回数量
     * @param similarityThreshold 相似度阈值（0.0~1.0）
     * @return 按相关度降序的文档列表
     */
    List<Document> retrieve(String query, Long knowledgeId, int topK, double similarityThreshold);

}

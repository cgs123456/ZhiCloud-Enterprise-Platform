package cn.iocoder.yudao.module.airag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 混合检索 Service（BM25 + 向量）
 *
 * <p>并行执行 BM25 关键词检索与向量语义检索，通过 RRF（Reciprocal Rank Fusion）融合两路结果，
 * 再调用 Reranker 重排序，返回 topK 文档。
 *
 * <p>混合检索兼顾关键词精确匹配（BM25）与语义相似（向量），在专有名词、错误码、代码标识符等场景下
 * 比纯向量检索召回率更高。
 *
 * @author yudao
 */
public interface HybridSearchService {

    /**
     * 混合检索
     *
     * <p>流程：
     * <ol>
     *   <li>并行执行 BM25 + 向量检索（各取 topK * 4 候选）</li>
     *   <li>RRF 融合两路结果：score = Σ weight_i / (k + rank_i)</li>
     *   <li>对融合后结果调用 Reranker 重排序</li>
     *   <li>返回 topK 文档</li>
     * </ol>
     *
     * @param query        用户查询
     * @param knowledgeId  知识库 ID（用于过滤向量检索范围）
     * @param topK         返回数量
     * @return 按相关度降序的 topK 文档
     */
    List<Document> hybridSearch(String query, Long knowledgeId, int topK);

}

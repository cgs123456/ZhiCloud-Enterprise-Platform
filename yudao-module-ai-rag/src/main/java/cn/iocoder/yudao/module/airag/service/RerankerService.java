package cn.iocoder.yudao.module.airag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Reranker 重排序 Service
 *
 * <p>对向量检索（或混合检索）召回的文档做二次排序，提升最终注入 LLM 上下文的片段精度。
 *
 * <p>实现策略：
 * <ul>
 *   <li>优先使用 BGE-reranker-base（Cross-Encoder，ONNX Runtime 加载）进行精准打分</li>
 *   <li>ONNX 模型不可用时，fallback 到 {@link SimpleReranker}（基于 TF-IDF + query 覆盖率）</li>
 * </ul>
 *
 * @author yudao
 */
public interface RerankerService {

    /**
     * 对检索结果重排序
     *
     * @param query     用户查询
     * @param documents 待排序的文档列表（已通过向量/BM25 召回）
     * @param topK      返回的文档数量
     * @return 按相关度分数降序排列的 topK 文档列表（每个文档的 score 已更新为重排序分数）
     */
    List<Document> rerank(String query, List<Document> documents, int topK);

}

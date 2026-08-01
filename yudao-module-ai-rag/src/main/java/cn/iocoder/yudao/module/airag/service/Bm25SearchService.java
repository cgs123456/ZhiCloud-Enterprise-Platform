package cn.iocoder.yudao.module.airag.service;

import org.springframework.ai.document.Document;

import java.util.List;

/**
 * BM25 关键词检索 Service
 *
 * <p>基于内存 BM25 索引（从文档内容构建），提供关键词召回能力，与向量检索互补。
 *
 * <p>BM25 是基于 TF-IDF 的改进算法，对词频做饱和处理并引入文档长度归一化，
 * 适合精确关键词匹配场景（如专有名词、代码标识符、错误码等）。
 *
 * @author yudao
 */
public interface Bm25SearchService {

    /**
     * 对已索引的文档进行 BM25 检索
     *
     * @param query 用户查询
     * @param topK  返回数量
     * @return 按相关度降序的文档列表（score 已更新）
     */
    List<Document> search(String query, int topK);

    /**
     * 对指定文档集合进行 BM25 检索（用于混合检索：在向量召回的候选集上做关键词重排）
     *
     * @param query     用户查询
     * @param documents 待检索文档集合（在该集合上构建临时 BM25 索引）
     * @param topK      返回数量
     * @return 按相关度降序的文档列表（score 已更新）
     */
    List<Document> search(String query, List<Document> documents, int topK);

    /**
     * 将文档加入内存 BM25 索引
     *
     * @param documents 待索引文档
     */
    void index(List<Document> documents);

    /**
     * 清空内存 BM25 索引
     */
    void clear();

}

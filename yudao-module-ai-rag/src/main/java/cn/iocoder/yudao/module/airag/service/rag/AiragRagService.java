package cn.iocoder.yudao.module.airag.service.rag;

import org.springframework.scheduling.annotation.Async;

/**
 * AI RAG 核心 Service 接口
 *
 * 封装本地化 RAG 的核心链路：文档向量化导入、相似度检索、LLM 回答生成、向量库文档删除。
 *
 * @author yudao
 */
public interface AiragRagService {

    /**
     * 导入文档到向量库
     *
     * 流程：
     * 1. 从数据库获取文档信息
     * 2. 用 TikaDocumentReader 解析文件
     * 3. 用 TokenTextSplitter 分块
     * 4. 调用 EmbeddingModel 生成向量（由 VectorStore 内部完成）
     * 5. 用 PgVectorStore 存储
     * 6. 更新文档状态
     *
     * 通过 {@link Async} 异步执行，由 {@code AiragDocumentService.uploadDocument} 触发。
     * 异常会被内部捕获并写入文档的 errorMsg 字段，状态置为失败。
     *
     * @param knowledgeId 知识库编号
     * @param documentId  文档编号
     */
    @Async
    void importDocument(Long knowledgeId, Long documentId);

    /**
     * RAG 检索 + LLM 回答
     *
     * 流程：
     * 1. 用 PgVectorStore.similaritySearch 检索相关文档
     * 2. 构造 Prompt（系统提示 + 检索到的上下文 + 用户问题）
     * 3. 调用 ChatClient 返回回答
     *
     * @param knowledgeId 知识库编号
     * @param question    用户问题
     * @return RAG 回答
     */
    String chat(Long knowledgeId, String question);

    /**
     * 从向量库删除文档
     *
     * @param documentId 文档编号
     */
    void deleteDocument(Long documentId);

}

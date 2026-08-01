package cn.iocoder.yudao.module.airag.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * AI RAG 错误码枚举类
 *
 * ai-rag 系统，使用 1-041-000-000 段（与 ai 模块 1-040 区分）
 *
 * @author yudao
 */
public interface ErrorCodeConstants {

    // ========== 知识库 1-041-000-000 ==========
    ErrorCode KNOWLEDGE_NOT_EXISTS = new ErrorCode(1_041_000_000, "知识库不存在");
    ErrorCode KNOWLEDGE_DISABLE = new ErrorCode(1_041_000_001, "知识库({})已禁用");

    // ========== 文档 1-041-001-000 ==========
    ErrorCode DOCUMENT_NOT_EXISTS = new ErrorCode(1_041_001_000, "文档不存在");
    ErrorCode DOCUMENT_FILE_EMPTY = new ErrorCode(1_041_001_001, "文档内容为空");
    ErrorCode DOCUMENT_FILE_DOWNLOAD_FAIL = new ErrorCode(1_041_001_002, "文件下载失败");
    ErrorCode DOCUMENT_FILE_READ_FAIL = new ErrorCode(1_041_001_003, "文档解析失败");
    ErrorCode DOCUMENT_STATUS_NOT_READY = new ErrorCode(1_041_001_004, "文档尚未处理完成");

    // ========== RAG 检索 1-041-002-000 ==========
    ErrorCode RAG_VECTOR_STORE_NOT_READY = new ErrorCode(1_041_002_000, "向量存储未启用，请先配置 yudao.airag.enabled=true 并部署 PostgreSQL + pgvector");
    ErrorCode RAG_EMBEDDING_FAIL = new ErrorCode(1_041_002_001, "向量生成失败");
    ErrorCode RAG_RETRIEVE_FAIL = new ErrorCode(1_041_002_002, "向量检索失败");
    ErrorCode RAG_CHAT_ERROR = new ErrorCode(1_041_002_003, "RAG 对话生成异常");

    // ========== RAG 评估 1-041-003-000 ==========
    ErrorCode RAG_EVALUATION_LLM_UNAVAILABLE = new ErrorCode(1_041_003_000, "RAG 评估无可用 LLM（ChatClient 与 AiModelService 均未注入）");
    ErrorCode RAG_EVALUATION_CONTEXTS_EMPTY = new ErrorCode(1_041_003_001, "RAG 评估上下文文档列表为空");
    ErrorCode RAG_EVALUATION_GROUND_TRUTH_EMPTY = new ErrorCode(1_041_003_002, "RAG 评估上下文召回率需要标准答案（groundTruth）");
    ErrorCode RAG_EVALUATION_SCORE_PARSE_FAIL = new ErrorCode(1_041_003_003, "RAG 评估 LLM 评分解析失败：{}");

}

package cn.iocoder.yudao.module.airag;

/**
 * AI RAG 模块的入口标记类。
 *
 * 本模块作为 yudao 项目的可选模块，本身不提供独立的 Spring Boot 启动入口，
 * 由 yudao-server 主应用通过 ComponentScan 扫描 {@code cn.iocoder.yudao.module.airag} 包加载。
 *
 * 核心能力：
 * 1. 知识库管理（airag_knowledge）
 * 2. 文档管理 + Tika 解析 + TokenTextSplitter 分块（airag_document）
 * 3. 向量存储（PostgreSQL + pgvector，BGE-base-zh 768 维）
 * 4. RAG 检索 + LLM 流式回答
 *
 * @author yudao
 */
public class AiragApplication {
}

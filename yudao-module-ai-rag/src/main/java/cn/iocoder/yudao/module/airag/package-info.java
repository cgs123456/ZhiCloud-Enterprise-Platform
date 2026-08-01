/**
 * AI RAG 模块：本地化检索增强生成（pgvector + BGE Embedding + 文档分块检索）
 *
 * <p>Spring Modulith 模块声明（A3）。依赖：ai/system/infra。
 *
 * <p>本模块依赖 PostgreSQL + pgvector 扩展，通过 {@code yudao.airag.enabled=true} 启用。
 *
 * @author yudao
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI RAG 检索增强模块")
package cn.iocoder.yudao.module.airag;

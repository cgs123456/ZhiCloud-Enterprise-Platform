package cn.zhicloud.module.airag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI-RAG 向量库配置。
 *
 * <p>生产环境默认不执行建表 DDL：向量表和索引必须由数据库迁移脚本预先创建，
 * 应用启动时仅由 Spring AI 校验表结构。仅在本地/开发环境需要临时自动建表时，
 * 才显式设置 {@code zhicloud.airag.initialize-schema=true}。
 */
@ConfigurationProperties(prefix = "zhicloud.airag")
@Data
public class AiragProperties {

    /**
     * PgVectorStore 使用的向量表名。
     */
    private String vectorTableName = "airag_vector_store";

    /**
     * 是否允许应用启动时自动创建向量表和索引。
     *
     * <p>生产必须保持 false，由受审计的迁移流程负责 DDL。
     */
    private boolean initializeSchema = false;
}
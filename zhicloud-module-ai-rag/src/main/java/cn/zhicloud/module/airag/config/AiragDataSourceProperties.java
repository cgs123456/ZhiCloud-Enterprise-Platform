package cn.zhicloud.module.airag.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI-RAG 独立 PostgreSQL/pgvector 数据源配置。
 *
 * <p>该数据源只服务向量库与 BM25 词法检索，不能复用业务主库的全局数据源。
 * 配置前缀与 Docker/K8s/Helm 中已经使用的参数保持一致：
 * {@code zhicloud.airag.datasource.*}。
 */
@ConfigurationProperties(prefix = "zhicloud.airag.datasource")
public class AiragDataSourceProperties extends DataSourceProperties {

    public AiragDataSourceProperties() {
        // pgvector starter 已引入 PostgreSQL JDBC 驱动；显式设置避免 URL 推断失败。
        setDriverClassName("org.postgresql.Driver");
    }
}

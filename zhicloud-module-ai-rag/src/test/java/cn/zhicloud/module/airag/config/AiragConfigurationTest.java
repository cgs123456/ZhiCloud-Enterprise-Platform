package cn.zhicloud.module.airag.config;

import cn.zhicloud.module.airag.service.rag.Bm25LexicalRetriever;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * AI-RAG 数据源配置回归测试。
 *
 * <p>核心约束：AI-RAG 的 pgvector 连接必须使用独立的 PostgreSQL 数据源，
 * 不能复用业务主库的全局 JdbcTemplate（当前业务主库为 MySQL）。
 */
class AiragConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(WiringTestConfiguration.class, AiragConfiguration.class)
            .withPropertyValues(
                    "zhicloud.airag.enabled=true",
                    "zhicloud.airag.datasource.url=jdbc:postgresql://127.0.0.1:5432/zhicloud_rag",
                    "zhicloud.airag.datasource.username=zhicloud",
                    "zhicloud.airag.datasource.password=secret");

    @Test
    void airagEnabled_bindsDedicatedPostgresDataSourceProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AiragDataSourceProperties.class);
            AiragDataSourceProperties properties = context.getBean(AiragDataSourceProperties.class);
            assertThat(properties.getUrl())
                    .isEqualTo("jdbc:postgresql://127.0.0.1:5432/zhicloud_rag");
            assertThat(properties.getUsername()).isEqualTo("zhicloud");
            assertThat(properties.getPassword()).isEqualTo("secret");
            assertThat(properties.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        });
    }

    @Test
    void airagEnabled_buildsIsolatedPostgresDataSource() {
        AiragDataSourceProperties properties = new AiragDataSourceProperties();
        properties.setUrl("jdbc:postgresql://127.0.0.1:5432/zhicloud_rag");
        properties.setUsername("zhicloud");
        properties.setPassword("secret");

        DataSource dataSource = new AiragConfiguration().airagDataSource(properties);
        try {
            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
            HikariDataSource hikari = (HikariDataSource) dataSource;
            assertThat(hikari.getJdbcUrl())
                    .isEqualTo("jdbc:postgresql://127.0.0.1:5432/zhicloud_rag");
            assertThat(hikari.getUsername()).isEqualTo("zhicloud");
            assertThat(hikari.getDriverClassName()).isEqualTo("org.postgresql.Driver");
        } finally {
            if (dataSource instanceof HikariDataSource hikari) {
                hikari.close();
            }
        }
    }

    @Test
    void airagEnabled_wiresVectorStoreAndBm25ToDedicatedJdbcTemplate() {
        contextRunner.run(context -> {
            JdbcTemplate airagJdbcTemplate = context.getBean("airagJdbcTemplate", JdbcTemplate.class);
            JdbcTemplate mysqlJdbcTemplate = context.getBean("mysqlJdbcTemplate", JdbcTemplate.class);
            assertThat(airagJdbcTemplate).isNotSameAs(mysqlJdbcTemplate);

            VectorStore vectorStore = context.getBean(VectorStore.class);
            assertThat(vectorStore).isInstanceOf(PgVectorStore.class);
            Object nativeClient = ((PgVectorStore) vectorStore).getNativeClient().orElseThrow();
            assertThat(nativeClient).isSameAs(airagJdbcTemplate);

            Bm25LexicalRetriever bm25 = context.getBean(Bm25LexicalRetriever.class);
            assertThat(ReflectionTestUtils.getField(bm25, "jdbcTemplate"))
                    .isSameAs(airagJdbcTemplate)
                    .isNotSameAs(mysqlJdbcTemplate);
        });
    }

    @Test
    void airagEnabled_usesConfiguredVectorTableName() {
        createContextRunner()
                .withPropertyValues("zhicloud.airag.vector-table-name=tenant_vector_store")
                .run(context -> {
                    PgVectorStore vectorStore = (PgVectorStore) context.getBean(VectorStore.class);
                    assertThat(ReflectionTestUtils.getField(vectorStore, "vectorTableName"))
                            .isEqualTo("tenant_vector_store");

                    Bm25LexicalRetriever bm25 = context.getBean(Bm25LexicalRetriever.class);
                    assertThat(ReflectionTestUtils.getField(bm25, "vectorTableName"))
                            .isEqualTo("tenant_vector_store");
                });
    }

    @Test
    void airagEnabled_defaultsToSchemaValidationWithoutDdl() {
        createContextRunner().run(context -> {
            PgVectorStore vectorStore = (PgVectorStore) context.getBean(VectorStore.class);
            assertThat(ReflectionTestUtils.getField(vectorStore, "initializeSchema"))
                    .isEqualTo(false);
        });
    }

    private ApplicationContextRunner createContextRunner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(WiringTestConfiguration.class, AiragConfiguration.class)
                .withPropertyValues(
                        "zhicloud.airag.enabled=true",
                        "zhicloud.airag.datasource.url=jdbc:postgresql://127.0.0.1:5432/zhicloud_rag",
                        "zhicloud.airag.datasource.username=zhicloud",
                        "zhicloud.airag.datasource.password=secret");
    }
    @Configuration(proxyBeanMethods = false)
    static class WiringTestConfiguration {

        @Bean
        EmbeddingModel embeddingModel() {
            return mock(EmbeddingModel.class);
        }

        @Bean
        JdbcTemplate airagJdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        JdbcTemplate mysqlJdbcTemplate() {
            return mock(JdbcTemplate.class);
        }
    }
}
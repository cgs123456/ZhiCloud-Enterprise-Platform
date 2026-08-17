package cn.iocoder.yudao.module.airag.config;

import cn.iocoder.yudao.module.airag.service.RerankerService;
import cn.iocoder.yudao.module.airag.service.RerankerServiceImpl;
import cn.iocoder.yudao.module.airag.service.rag.Bm25LexicalRetriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.transformers.TransformersEmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AI RAG 模块自动配置
 *
 * 通过 {@code yudao.airag.enabled=true} 控制向量存储相关 Bean 的加载。
 *
 * 当前 PostgreSQL + pgvector 尚未部署，默认不启用，避免启动期因连接 PostgreSQL 失败而报错。
 * 部署完成后再通过配置开启：
 * <pre>
 * yudao:
 *   airag:
 *     enabled: true
 *     vector-table-name: airag_vector_store
 * </pre>
 *
 * 关于 Embedding 模型：本模块选型为 BGE-base-zh（768 维），通过 ONNX Runtime 嵌入应用。
 * 当容器中不存在 {@link EmbeddingModel} Bean 时，本配置类会自动创建一个基于
 * {@link TransformersEmbeddingModel} 的本地 Embedding 模型（默认加载 BGE-base-zh-v1.5），
 * 从而使 RAG 模块可独立使用；若使用方已提供外部 EmbeddingModel（如 OpenAI 兼容 API），
 * 则通过 {@code @ConditionalOnMissingBean} 复用之。
 *
 * @author yudao
 */
@Configuration
@ConditionalOnProperty(prefix = "yudao.airag", name = "enabled", havingValue = "true")
@Slf4j
public class AiragConfiguration {

    /**
     * 默认向量维度：BGE-base-zh 为 768 维
     */
    private static final int DEFAULT_VECTOR_DIMENSION = 768;

    /**
     * 默认向量存储表名
     */
    private static final String DEFAULT_VECTOR_TABLE_NAME = "airag_vector_store";

    /**
     * 本地 Embedding 模型文件所在的 classpath 目录名（位于 {@code airag/} 子目录下）。
     *
     * <p>完整资源路径为：{@code classpath:airag/{model-path}/model.onnx} 与
     * {@code classpath:airag/{model-path}/tokenizer.json}。可通过 {@code yudao.airag.embedding.model-path} 覆盖。
     */
    @Value("${yudao.airag.embedding.model-path:bge-base-zh-v1.5}")
    private String embeddingModelPath;

    /**
     * 创建本地 EmbeddingModel Bean（BGE-base-zh，基于 ONNX Runtime）
     *
     * <p>仅当容器中不存在其他 {@link EmbeddingModel} 时才创建，避免与 yudao-module-ai 的
     * OpenAI / 通义千问等外部 Embedding 自动配置冲突。使用方若已配置外部 Embedding 服务，
     * 则本 Bean 不创建，{@link #airagVectorStore} 将复用外部 EmbeddingModel。
     *
     * <p>模型文件需通过 {@code scripts/download-bge-model.sh|ps1} 预先下载到
     * {@code yudao-server/src/main/resources/airag/{model-path}/} 目录下。
     *
     * @return TransformersEmbeddingModel 实例（Spring 容器会自动调用 {@code afterPropertiesSet} 加载 ONNX 模型）
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel airagEmbeddingModel() {
        String baseDir = "airag/" + embeddingModelPath;
        String modelUri = "classpath:" + baseDir + "/model.onnx";
        String tokenizerUri = "classpath:" + baseDir + "/tokenizer.json";
        log.info("[airagEmbeddingModel][初始化本地 TransformersEmbeddingModel，model-path={}, dimension={}]",
                embeddingModelPath, DEFAULT_VECTOR_DIMENSION);
        TransformersEmbeddingModel embeddingModel = new TransformersEmbeddingModel();
        embeddingModel.setModelResource(modelUri);
        embeddingModel.setTokenizerResource(tokenizerUri);
        return embeddingModel;
    }

    /**
     * 创建 PgVectorStore Bean
     *
     * 当 {@code yudao.airag.enabled=true} 且容器中存在 {@link EmbeddingModel} 时才创建。
     * PgVectorStore 通过 {@code initializeSchema=true} 自动建表（airag_vector_store）。
     *
     * @param jdbcTemplate  Spring 自动注入的 JdbcTemplate（来自 spring-ai-starter-vector-store-pgvector 的数据源配置）
     * @param embeddingModel Embedding 模型（本模块本地 BGE-base-zh 或使用方提供的外部 Embedding）
     * @return VectorStore 实例
     */
    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public VectorStore airagVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        log.info("[airagVectorStore][初始化 PgVectorStore，dimension={}, table={}]", DEFAULT_VECTOR_DIMENSION, DEFAULT_VECTOR_TABLE_NAME);
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(DEFAULT_VECTOR_DIMENSION)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .vectorTableName(DEFAULT_VECTOR_TABLE_NAME)
                .initializeSchema(true)
                .build();
    }

    /**
     * 创建 BM25 词法检索器 Bean（混合召回的词法路径）。
     *
     * <p>仅当 {@code yudao.airag.enabled=true}（向量底表可用）时创建，
     * 直接复用 PgVectorStore 底表 {@code airag_vector_store} 的 chunk 文本做 Okapi BM25 召回，
     * 与向量语义路径相互独立，最终由 {@code AiragRagServiceImpl} 做 RRF 融合。
     *
     * @param jdbcTemplate Spring 自动注入的 JdbcTemplate
     * @return Bm25LexicalRetriever 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "yudao.airag", name = "enabled", havingValue = "true")
    public Bm25LexicalRetriever bm25LexicalRetriever(JdbcTemplate jdbcTemplate) {
        log.info("[bm25LexicalRetriever][初始化 BM25 词法检索器，table={}]", DEFAULT_VECTOR_TABLE_NAME);
        return new Bm25LexicalRetriever(jdbcTemplate, DEFAULT_VECTOR_TABLE_NAME);
    }

    /**
     * 创建 Reranker Service Bean
     *
     * <p>当 {@code yudao.airag.reranker.enabled=true} 时启用。
     * 优先加载 BGE-reranker-base（ONNX Cross-Encoder），模型不可用时 fallback 到 SimpleReranker。
     *
     * @return RerankerService 实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "yudao.airag.reranker", name = "enabled", havingValue = "true")
    public RerankerService rerankerService() {
        log.info("[rerankerService][初始化 RerankerService]");
        return new RerankerServiceImpl();
    }

}

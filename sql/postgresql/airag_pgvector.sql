-- ======================================================================
-- zhicloud-module-ai-rag 建表脚本（PostgreSQL + pgvector）
--
-- 说明：
--   1. 本脚本用于本地化 RAG 模块，需在 PostgreSQL 12+ 上执行
--   2. 需先安装 pgvector 扩展：https://github.com/pgvector/pgvector
--   3. 知识库表（airag_knowledge）、文档表（airag_document）由本脚本创建
--   4. 向量存储表（airag_vector_store）和 HNSW 索引由本脚本显式创建，
--      应用必须配置 zhicloud.airag.initialize-schema=false（生产默认）
--   5. 字段命名与 zhicloud 框架 BaseDO 保持一致（creator/create_time/updater/update_time/deleted/tenant_id）
-- ======================================================================

-- 启用 pgvector 与 hstore 扩展（与 Spring AI PgVectorStore 的 schema 校验保持一致）
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;

-- ----------------------------------------------------------------------
-- 知识库表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS airag_knowledge (
    id               BIGINT       PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    status           SMALLINT     DEFAULT 0,
    embedding_model  VARCHAR(100),
    vector_dimension INT          DEFAULT 768,
    creator          VARCHAR(64),
    create_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updater          VARCHAR(64),
    update_time      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted          BIT(1)       DEFAULT 0,
    tenant_id        BIGINT       DEFAULT 0
);

COMMENT ON TABLE  airag_knowledge IS 'AI RAG 知识库';
COMMENT ON COLUMN airag_knowledge.id IS '编号';
COMMENT ON COLUMN airag_knowledge.name IS '知识库名称';
COMMENT ON COLUMN airag_knowledge.description IS '知识库描述';
COMMENT ON COLUMN airag_knowledge.status IS '状态（0开启 1停用）';
COMMENT ON COLUMN airag_knowledge.embedding_model IS 'Embedding 模型标识，例如 bge-base-zh';
COMMENT ON COLUMN airag_knowledge.vector_dimension IS '向量维度（768/1024）';
COMMENT ON COLUMN airag_knowledge.creator IS '创建者';
COMMENT ON COLUMN airag_knowledge.create_time IS '创建时间';
COMMENT ON COLUMN airag_knowledge.updater IS '更新者';
COMMENT ON COLUMN airag_knowledge.update_time IS '更新时间';
COMMENT ON COLUMN airag_knowledge.deleted IS '是否删除';
COMMENT ON COLUMN airag_knowledge.tenant_id IS '租户编号';

-- ----------------------------------------------------------------------
-- 文档表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS airag_document (
    id            BIGINT       PRIMARY KEY,
    knowledge_id  BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(20),
    url           VARCHAR(500),
    status        SMALLINT     DEFAULT 0,
    chunk_count   INT          DEFAULT 0,
    error_msg     TEXT,
    creator       VARCHAR(64),
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updater       VARCHAR(64),
    update_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    deleted       BIT(1)       DEFAULT 0,
    tenant_id     BIGINT       DEFAULT 0
);

COMMENT ON TABLE  airag_document IS 'AI RAG 文档';
COMMENT ON COLUMN airag_document.id IS '编号';
COMMENT ON COLUMN airag_document.knowledge_id IS '知识库编号（关联 airag_knowledge.id）';
COMMENT ON COLUMN airag_document.name IS '文档名称';
COMMENT ON COLUMN airag_document.type IS '文档类型（pdf/docx/txt/md）';
COMMENT ON COLUMN airag_document.url IS '文件 URL';
COMMENT ON COLUMN airag_document.status IS '处理状态（0待处理 1处理中 2已完成 3失败）';
COMMENT ON COLUMN airag_document.chunk_count IS '分块数量';
COMMENT ON COLUMN airag_document.error_msg IS '错误信息';
COMMENT ON COLUMN airag_document.creator IS '创建者';
COMMENT ON COLUMN airag_document.create_time IS '创建时间';
COMMENT ON COLUMN airag_document.updater IS '更新者';
COMMENT ON COLUMN airag_document.update_time IS '更新时间';
COMMENT ON COLUMN airag_document.deleted IS '是否删除';
COMMENT ON COLUMN airag_document.tenant_id IS '租户编号';

-- 文档表索引
CREATE INDEX IF NOT EXISTS idx_airag_document_knowledge_id ON airag_document (knowledge_id);
CREATE INDEX IF NOT EXISTS idx_airag_document_status       ON airag_document (status);
CREATE INDEX IF NOT EXISTS idx_airag_document_tenant_id    ON airag_document (tenant_id);

-- ----------------------------------------------------------------------
-- 向量存储表（airag_vector_store）
--
-- 与 Spring AI 1.1.8 PgVectorStore 的表结构对齐。生产启动只做 schema 校验，
-- 不在应用启动阶段执行 CREATE TABLE/CREATE EXTENSION；DDL 由本脚本纳入审计和回滚。
-- 维度必须与 AiragConfiguration.DEFAULT_VECTOR_DIMENSION 保持一致。
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS airag_vector_store (
    id        UUID PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding vector(768)
);

-- Spring AI 1.1.8 对自定义表 airag_vector_store 使用该索引名。
CREATE INDEX IF NOT EXISTS airag_vector_store_index
    ON airag_vector_store USING hnsw (embedding vector_cosine_ops);

-- ======================================================================
-- zhicloud-module-ai-rag 建表脚本（PostgreSQL + pgvector）
--
-- 说明：
--   1. 本脚本用于本地化 RAG 模块，需在 PostgreSQL 12+ 上执行
--   2. 需先安装 pgvector 扩展：https://github.com/pgvector/pgvector
--   3. 知识库表（airag_knowledge）、文档表（airag_document）由本脚本创建
--   4. 向量存储表（airag_vector_store）由 Spring AI PgVectorStore 通过
--      initializeSchema=true 自动创建，本脚本仅作声明，不重复建表
--   5. 字段命名与 zhicloud 框架 BaseDO 保持一致（creator/create_time/updater/update_time/deleted/tenant_id）
-- ======================================================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

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
-- 由 Spring AI PgVectorStore 通过 initializeSchema=true 自动创建，
-- 表结构由 PgVectorStore 默认定义（id UUID, content TEXT, metadata JSON, embedding vector）。
-- 这里仅作声明，不手动建表，避免与 Spring AI 版本不兼容。
--
-- HNSW 索引由 Spring AI 启动时自动创建（PgVectorStore 配置 PgIndexType.HNSW），
-- 此脚本仅手动建表场景使用。
--
-- 如需手动建表，可参考以下 SQL（需与 PgVectorStore 配置的 dimensions 保持一致）：
--
-- CREATE TABLE IF NOT EXISTS airag_vector_store (
--     id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     content   TEXT,
--     metadata  JSON,
--     embedding vector(768)
-- );
-- CREATE INDEX IF NOT EXISTS airag_vector_store_embedding_index
--     ON airag_vector_store USING hnsw (embedding vector_cosine_ops);
-- ----------------------------------------------------------------------

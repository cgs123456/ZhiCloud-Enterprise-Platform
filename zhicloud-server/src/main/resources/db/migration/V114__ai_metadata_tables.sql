-- ============================================================
-- V114: AI Prompt 模板与 AI-RAG 元数据表（MySQL 主库）
--
-- 背景：
--   1. zhicloud-module-ai 的 AiPromptTemplateDO 对应 ai_prompt_template，
--      但此前仅有 sql/mysql/ai_prompt_template.sql 独立脚本，未纳入 Flyway；
--   2. zhicloud-module-ai-rag 的 AiragKnowledgeDO / AiragDocumentDO 通过
--      MyBatis-Plus 访问 MySQL 主库，但此前只有 PostgreSQL 脚本
--      sql/postgresql/airag_pgvector.sql，未纳入 MySQL Flyway；
--   3. 向量底表 airag_vector_store 仍由 PostgreSQL + pgvector 独立承载，
--      本迁移只补齐主库元数据表，不创建向量表，也不改变 RAG 开关默认值。
--
-- 安全边界：
--   * 不插入任何模拟/业务数据；
--   * 所有业务表包含 tenant_id，并以 tenant_id 作为业务索引首列；
--   * id 使用 AUTO_INCREMENT，与现有 MySQL Flyway 表及 MyBatis-Plus 配置一致；
--   * CREATE TABLE IF NOT EXISTS，可重复执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(128) NOT NULL COMMENT '模板名称',
    code VARCHAR(64) NOT NULL COMMENT '模板编码',
    category VARCHAR(32) NOT NULL COMMENT '分类（SYSTEM/CHAT/RAG/AGENT/TRANSLATION/SUMMARIZATION/CODE）',
    content TEXT NOT NULL COMMENT '模板内容（含变量占位符 {variableName}）',
    variables VARCHAR(512) COMMENT '变量列表（JSON 格式）',
    description VARCHAR(512) COMMENT '描述',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 开启 1 停用）',
    remark VARCHAR(512) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI Prompt 模板表';

CREATE TABLE IF NOT EXISTS `airag_knowledge` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) NOT NULL COMMENT '知识库名称',
    description TEXT COMMENT '知识库描述',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 开启 1 停用）',
    embedding_model VARCHAR(100) COMMENT 'Embedding 模型标识',
    vector_dimension INT NOT NULL DEFAULT 768 COMMENT '向量维度',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_name` (`tenant_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI RAG 知识库';

CREATE TABLE IF NOT EXISTS `airag_document` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    knowledge_id BIGINT NOT NULL COMMENT '知识库编号（关联 airag_knowledge.id）',
    name VARCHAR(255) NOT NULL COMMENT '文档名称',
    type VARCHAR(20) COMMENT '文档类型（pdf/docx/txt/md）',
    url VARCHAR(500) COMMENT '文件 URL',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态（0 待处理 1 处理中 2 已完成 3 失败）',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '分块数量',
    error_msg TEXT COMMENT '错误信息',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_knowledge` (`tenant_id`, `knowledge_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI RAG 文档';

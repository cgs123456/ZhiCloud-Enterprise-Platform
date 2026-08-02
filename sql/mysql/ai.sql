-- ======================== yudao-module-ai 生产建表脚本（自动生成，上线前需在真实 MySQL 冒烟验证） ========================

CREATE TABLE IF NOT EXISTS ai_api_key (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    api_key VARCHAR(255) DEFAULT NULL COMMENT 'api_key',
    platform VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    url VARCHAR(255) DEFAULT NULL COMMENT 'url',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_api_key';


CREATE TABLE IF NOT EXISTS ai_chat_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    title VARCHAR(255) DEFAULT NULL COMMENT 'title',
    pinned BIT(1) DEFAULT NULL COMMENT 'pinned',
    pinned_time DATETIME DEFAULT NULL COMMENT 'pinned_time',
    role_id BIGINT DEFAULT NULL COMMENT 'role_id',
    model_id BIGINT DEFAULT NULL COMMENT 'model_id',
    model TEXT DEFAULT NULL COMMENT 'model',
    system_message TEXT DEFAULT NULL COMMENT 'system_message',
    temperature DOUBLE DEFAULT NULL COMMENT 'temperature',
    max_tokens INT DEFAULT NULL COMMENT 'max_tokens',
    max_contexts INT DEFAULT NULL COMMENT 'max_contexts',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_conversation';


CREATE TABLE IF NOT EXISTS ai_chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    conversation_id BIGINT DEFAULT NULL COMMENT 'conversation_id',
    reply_id BIGINT DEFAULT NULL COMMENT 'reply_id',
    type VARCHAR(255) DEFAULT NULL COMMENT 'type',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    role_id BIGINT DEFAULT NULL COMMENT 'role_id',
    model TEXT DEFAULT NULL COMMENT 'model',
    model_id BIGINT DEFAULT NULL COMMENT 'model_id',
    content TEXT DEFAULT NULL COMMENT 'content',
    reasoning_content TEXT DEFAULT NULL COMMENT 'reasoning_content',
    use_context BIT(1) DEFAULT NULL COMMENT 'use_context',
    segment_ids VARCHAR(255) DEFAULT NULL COMMENT 'segment_ids',
    web_search_pages VARCHAR(255) DEFAULT NULL COMMENT 'web_search_pages',
    attachment_urls VARCHAR(255) DEFAULT NULL COMMENT 'attachment_urls',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_message';


CREATE TABLE IF NOT EXISTS ai_chat_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    avatar VARCHAR(255) DEFAULT NULL COMMENT 'avatar',
    category VARCHAR(255) DEFAULT NULL COMMENT 'category',
    description TEXT DEFAULT NULL COMMENT 'description',
    system_message TEXT DEFAULT NULL COMMENT 'system_message',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    model_id BIGINT DEFAULT NULL COMMENT 'model_id',
    knowledge_ids VARCHAR(255) DEFAULT NULL COMMENT 'knowledge_ids',
    tool_ids VARCHAR(255) DEFAULT NULL COMMENT 'tool_ids',
    mcp_client_names VARCHAR(255) DEFAULT NULL COMMENT 'mcp_client_names',
    public_status BIT(1) DEFAULT NULL COMMENT 'public_status',
    sort INT DEFAULT NULL COMMENT 'sort',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_chat_role';


CREATE TABLE IF NOT EXISTS ai_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    prompt TEXT DEFAULT NULL COMMENT 'prompt',
    platform VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    model_id BIGINT DEFAULT NULL COMMENT 'model_id',
    model TEXT DEFAULT NULL COMMENT 'model',
    width INT DEFAULT NULL COMMENT 'width',
    height INT DEFAULT NULL COMMENT 'height',
    `status` INT DEFAULT NULL COMMENT 'status',
    finish_time DATETIME DEFAULT NULL COMMENT 'finish_time',
    error_message TEXT DEFAULT NULL COMMENT 'error_message',
    pic_url VARCHAR(255) DEFAULT NULL COMMENT 'pic_url',
    public_status BIT(1) DEFAULT NULL COMMENT 'public_status',
    options VARCHAR(255) DEFAULT NULL COMMENT 'options',
    buttons VARCHAR(255) DEFAULT NULL COMMENT 'buttons',
    task_id VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_image';


CREATE TABLE IF NOT EXISTS ai_knowledge (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    description TEXT DEFAULT NULL COMMENT 'description',
    embedding_model_id BIGINT DEFAULT NULL COMMENT 'embedding_model_id',
    embedding_model TEXT DEFAULT NULL COMMENT 'embedding_model',
    top_k INT DEFAULT NULL COMMENT 'top_k',
    similarity_threshold DOUBLE DEFAULT NULL COMMENT 'similarity_threshold',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_knowledge';


CREATE TABLE IF NOT EXISTS ai_knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    knowledge_id BIGINT DEFAULT NULL COMMENT 'knowledge_id',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    url VARCHAR(255) DEFAULT NULL COMMENT 'url',
    content TEXT DEFAULT NULL COMMENT 'content',
    content_length INT DEFAULT NULL COMMENT 'content_length',
    tokens INT DEFAULT NULL COMMENT 'tokens',
    segment_max_tokens INT DEFAULT NULL COMMENT 'segment_max_tokens',
    retrieval_count INT DEFAULT NULL COMMENT 'retrieval_count',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_knowledge_document';


CREATE TABLE IF NOT EXISTS ai_knowledge_segment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    knowledge_id BIGINT DEFAULT NULL COMMENT 'knowledge_id',
    document_id BIGINT DEFAULT NULL COMMENT 'document_id',
    content TEXT DEFAULT NULL COMMENT 'content',
    content_length INT DEFAULT NULL COMMENT 'content_length',
    vector_id VARCHAR(255) DEFAULT NULL COMMENT 'vector_id',
    tokens INT DEFAULT NULL COMMENT 'tokens',
    retrieval_count INT DEFAULT NULL COMMENT 'retrieval_count',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_knowledge_segment';


CREATE TABLE IF NOT EXISTS ai_mind_map (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    platform VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    model_id BIGINT DEFAULT NULL COMMENT 'model_id',
    model TEXT DEFAULT NULL COMMENT 'model',
    prompt TEXT DEFAULT NULL COMMENT 'prompt',
    generated_content TEXT DEFAULT NULL COMMENT 'generated_content',
    error_message TEXT DEFAULT NULL COMMENT 'error_message',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_mind_map';


CREATE TABLE IF NOT EXISTS ai_model (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    key_id BIGINT DEFAULT NULL COMMENT 'key_id',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    model TEXT DEFAULT NULL COMMENT 'model',
    platform VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    type INT DEFAULT NULL COMMENT 'type',
    sort INT DEFAULT NULL COMMENT 'sort',
    `status` INT DEFAULT NULL COMMENT 'status',
    temperature DOUBLE DEFAULT NULL COMMENT 'temperature',
    max_tokens INT DEFAULT NULL COMMENT 'max_tokens',
    max_contexts INT DEFAULT NULL COMMENT 'max_contexts',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_model';


CREATE TABLE IF NOT EXISTS ai_music (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    title VARCHAR(255) DEFAULT NULL COMMENT 'title',
    lyric VARCHAR(255) DEFAULT NULL COMMENT 'lyric',
    image_url TEXT DEFAULT NULL COMMENT 'image_url',
    audio_url VARCHAR(255) DEFAULT NULL COMMENT 'audio_url',
    video_url VARCHAR(255) DEFAULT NULL COMMENT 'video_url',
    `status` INT DEFAULT NULL COMMENT 'status',
    generate_mode INT DEFAULT NULL COMMENT 'generate_mode',
    description TEXT DEFAULT NULL COMMENT 'description',
    platform VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    model TEXT DEFAULT NULL COMMENT 'model',
    tags VARCHAR(255) DEFAULT NULL COMMENT 'tags',
    duration DOUBLE DEFAULT NULL COMMENT 'duration',
    public_status BIT(1) DEFAULT NULL COMMENT 'public_status',
    task_id VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    error_message TEXT DEFAULT NULL COMMENT 'error_message',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_music';


CREATE TABLE IF NOT EXISTS ai_nl2sql_query_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    natural_language VARCHAR(255) DEFAULT NULL COMMENT 'natural_language',
    `sql` TEXT DEFAULT NULL COMMENT 'sql',
    data_source TEXT DEFAULT NULL COMMENT 'data_source',
    `status` INT DEFAULT NULL COMMENT 'status',
    row_count INT DEFAULT NULL COMMENT 'row_count',
    error_msg VARCHAR(255) DEFAULT NULL COMMENT 'error_msg',
    cost_ms BIGINT DEFAULT NULL COMMENT 'cost_ms',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_nl2sql_query_history';


CREATE TABLE IF NOT EXISTS ai_tool (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    description TEXT DEFAULT NULL COMMENT 'description',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_tool';


CREATE TABLE IF NOT EXISTS ai_workflow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    code VARCHAR(255) DEFAULT NULL COMMENT 'code',
    graph VARCHAR(255) DEFAULT NULL COMMENT 'graph',
    remark TEXT DEFAULT NULL COMMENT 'remark',
    `status` INT DEFAULT NULL COMMENT 'status',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_workflow';


CREATE TABLE IF NOT EXISTS ai_write (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    type INT DEFAULT NULL COMMENT 'type',
    platform VARCHAR(255) DEFAULT NULL COMMENT 'platform',
    model_id BIGINT DEFAULT NULL COMMENT 'model_id',
    model TEXT DEFAULT NULL COMMENT 'model',
    prompt TEXT DEFAULT NULL COMMENT 'prompt',
    generated_content TEXT DEFAULT NULL COMMENT 'generated_content',
    original_content TEXT DEFAULT NULL COMMENT 'original_content',
    length INT DEFAULT NULL COMMENT 'length',
    format INT DEFAULT NULL COMMENT 'format',
    tone INT DEFAULT NULL COMMENT 'tone',
    language INT DEFAULT NULL COMMENT 'language',
    error_message TEXT DEFAULT NULL COMMENT 'error_message',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ai_write';


-- ============================================================
-- V38: AI NL2SQL 报表分析（P1）
--
-- 新增 1 张表：
--   ai_nl2sql_query_history  自然语言查询历史（审计 + 优化）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_nl2sql_query_history (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    natural_language TEXT NOT NULL COMMENT '自然语言问题',
    `sql` TEXT COMMENT '生成的 SQL',
    data_source VARCHAR(64) DEFAULT NULL COMMENT '数据源标识（预留，用于多 schema 切换）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '执行状态（0-成功 1-校验失败 2-执行失败）',
    row_count INT DEFAULT NULL COMMENT '结果行数',
    error_msg VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
    cost_ms BIGINT DEFAULT NULL COMMENT '耗时（毫秒）',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI NL2SQL 查询历史';

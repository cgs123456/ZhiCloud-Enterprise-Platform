-- ============================================================
-- V39: AI RAG 评估（P1）
--
-- 新增 1 张表：
--   ai_rag_evaluation_log  RAG 评估日志（4 项指标 + 综合得分）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_rag_evaluation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    question TEXT NOT NULL COMMENT '用户问题',
    answer TEXT COMMENT 'RAG 生成的回答',
    context_count INT DEFAULT NULL COMMENT '检索到的上下文数量',
    has_ground_truth TINYINT(1) DEFAULT 0 COMMENT '标准答案是否提供',
    faithfulness DOUBLE DEFAULT NULL COMMENT '忠实度（0-1）',
    answer_relevancy DOUBLE DEFAULT NULL COMMENT '回答相关性（0-1）',
    context_precision DOUBLE DEFAULT NULL COMMENT '上下文精确率（0-1）',
    context_recall DOUBLE DEFAULT NULL COMMENT '上下文召回率（0-1）',
    overall_score DOUBLE DEFAULT NULL COMMENT '综合得分（0-1）',
    detail TEXT COMMENT '评估详情（LLM 原始反馈摘要）',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_create_time (create_time),
    KEY idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI RAG 评估日志';

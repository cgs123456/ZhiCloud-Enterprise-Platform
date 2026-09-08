-- ======================== AI RAG 评估数据集 + 批量评估 ========================
-- 作者：zhicloud
-- 说明：评估数据集管理、批量评估执行、聚合报告与历史趋势（P0-C）。
--       趋势接口读 airag_eval_report 历史行；CI 回归缓行（见方案），表结构已预留。
-- 约定：沿用通用列（creator/create_time/updater/update_time/deleted/tenant_id）。

-- ----------------------------
-- RAG 评估数据集表
-- ----------------------------
CREATE TABLE IF NOT EXISTS airag_eval_dataset (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(128) NOT NULL COMMENT '数据集名称',
    description TEXT COMMENT '数据集描述',
    knowledge_id BIGINT NOT NULL COMMENT '关联知识库',
    question_count INT DEFAULT 0 COMMENT '题目数量（冗余）',
    status TINYINT DEFAULT 0 COMMENT '状态（0编辑中 1已发布 2已归档）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT 'RAG评估数据集';

CREATE INDEX idx_eval_dataset_knowledge ON airag_eval_dataset(knowledge_id);

-- ----------------------------
-- RAG 评估问题表
-- ----------------------------
CREATE TABLE IF NOT EXISTS airag_eval_question (
    id BIGINT PRIMARY KEY COMMENT '主键',
    dataset_id BIGINT NOT NULL COMMENT '关联数据集',
    question TEXT NOT NULL COMMENT '问题',
    ground_truth TEXT NOT NULL COMMENT '标准答案',
    expected_keywords VARCHAR(512) COMMENT '期望关键词（逗号分隔）',
    difficulty VARCHAR(16) DEFAULT 'medium' COMMENT '难度（easy/medium/hard）',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    INDEX idx_eval_question_dataset (dataset_id)
) COMMENT 'RAG评估问题';

-- ----------------------------
-- RAG 批量评估报告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS airag_eval_report (
    id BIGINT PRIMARY KEY COMMENT '主键',
    dataset_id BIGINT NOT NULL COMMENT '关联数据集',
    knowledge_id BIGINT NOT NULL COMMENT '评估时的知识库（冗余）',
    status TINYINT DEFAULT 0 COMMENT '状态（0执行中 1已完成 2失败）',
    total_count INT DEFAULT 0 COMMENT '题目总数',
    done_count INT DEFAULT 0 COMMENT '已完成数（进度轮询用）',
    avg_faithfulness DOUBLE COMMENT '忠实度均值',
    avg_answer_relevancy DOUBLE COMMENT '回答相关性均值',
    avg_context_precision DOUBLE COMMENT '上下文精确率均值',
    avg_context_recall DOUBLE COMMENT '上下文召回率均值',
    overall_score DOUBLE COMMENT '综合得分（4 指标加权平均）',
    overall_delta DOUBLE COMMENT '与上次评估的分差',
    regressed_questions TEXT COMMENT '退步问题 JSON（questionId 列表）',
    improved_questions TEXT COMMENT '进步问题 JSON（questionId 列表）',
    results_json MEDIUMTEXT COMMENT '单题结果 JSON（含每题 4 指标）',
    error_msg TEXT COMMENT '失败原因',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    INDEX idx_eval_report_dataset (dataset_id)
) COMMENT 'RAG批量评估报告';

-- ============================================================
-- RAG 评估页面，挂载 AI 大模型 2758 下
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6200, 'RAG评估', '', 2, 8, 2758, 'rag-eval', 'ep:data-analysis', 'ai/eval/index', 'AiRagEval', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (6201, 'RAG评估查询', 'airag:eval:query', 3, 1, 6200, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (6202, 'RAG评估管理', 'airag:eval:manage', 3, 2, 6200, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

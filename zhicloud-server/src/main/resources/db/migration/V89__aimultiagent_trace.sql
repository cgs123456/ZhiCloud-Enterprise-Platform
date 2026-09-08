-- ======================== 多 Agent 编排：执行链路追踪（Span） ========================
-- 作者：zhicloud
-- 说明：P2-D。Langfuse 风格的持久化执行轨迹：一次编排执行（execution_log）拆解为
--       PLAN（任务拆解）→ WORKER（逐任务）→ SUMMARIZE（结果汇总）三类 Span，
--       记录起止时间、耗时、Token、状态与输出摘要，供事后复盘与时间线可视化。
--       埋点走 MultiAgentSpanService（单行追加写 + 降级容错），失败不影响主流程。
-- 约定：沿用 aimultiagent_* 表通用列（creator/create_time/updater/update_time/deleted/tenant_id）。

-- ----------------------------
-- 执行链路 Span 表
-- ----------------------------
CREATE TABLE IF NOT EXISTS aimultiagent_execution_span (
    id BIGINT PRIMARY KEY COMMENT '主键',
    execution_log_id BIGINT NOT NULL COMMENT '关联执行日志 aimultiagent_execution_log.id',
    trace_id VARCHAR(64) COMMENT '全链路追踪 ID（与执行日志 trace_id 一致）',
    span_type VARCHAR(16) NOT NULL COMMENT 'Span 类型（PLAN/WORKER/SUMMARIZE）',
    worker_name VARCHAR(64) COMMENT 'WORKER Span 的 Worker 名称',
    task_index INT COMMENT 'WORKER Span 的任务序号（从 0 开始）',
    task_id VARCHAR(64) COMMENT '任务 ID（与 AgentTask.taskId 对应）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0运行中 1成功 2失败）',
    tokens INT COMMENT '本 Span 消耗的 Token 数',
    duration_ms BIGINT COMMENT '耗时（毫秒）',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    error_msg VARCHAR(500) COMMENT '错误信息（失败时）',
    output_excerpt TEXT COMMENT '输出摘要（截断，避免大文本）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT '多 Agent 执行链路 Span';

CREATE INDEX idx_span_execution ON aimultiagent_execution_span(execution_log_id);
CREATE INDEX idx_span_trace ON aimultiagent_execution_span(trace_id);

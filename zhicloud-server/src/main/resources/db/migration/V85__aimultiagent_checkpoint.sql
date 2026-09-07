-- ======================== 多 Agent 编排：执行检查点（Checkpoint + Resume） ========================
-- 作者：zhicloud
-- 说明：支撑编排中断恢复。执行流程按 PLAN_COMPLETED → WORKER_DONE（逐任务）→ SUMMARIZE_DONE
--       写入检查点；中断后 resume() 从最后检查点继续。租约列用于并发执行保护与超时检测。
-- 约定：沿用 aimultiagent_* 表通用列（creator/create_time/updater/update_time/deleted/tenant_id）。

-- ----------------------------
-- 多 Agent 编排检查点表
-- ----------------------------
CREATE TABLE IF NOT EXISTS aimultiagent_checkpoint (
    id BIGINT PRIMARY KEY COMMENT '主键',
    execution_log_id BIGINT NOT NULL COMMENT '关联执行日志 aimultiagent_execution_log.id',
    topology_id BIGINT NOT NULL COMMENT '拓扑 ID（冗余，便于按拓扑排查）',
    checkpoint_type VARCHAR(32) NOT NULL COMMENT '检查点类型（PLAN_COMPLETED/WORKER_DONE/SUMMARIZE_DONE）',
    worker_name VARCHAR(64) COMMENT 'WORKER_DONE 时的 Worker 名称',
    task_index INT COMMENT '当前任务序号（从 0 开始，WORKER_DONE 时有效）',
    state_json TEXT COMMENT '序列化状态（已完成任务结果列表 JSON）',
    lease_token VARCHAR(64) COMMENT '租约令牌（执行中持有，完成后清空）',
    lease_expire_time DATETIME COMMENT '租约过期时间（超时未释放视为执行者失联）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT '多 Agent 编排检查点';

CREATE INDEX idx_checkpoint_execution ON aimultiagent_checkpoint(execution_log_id);
CREATE INDEX idx_checkpoint_lease_expire ON aimultiagent_checkpoint(lease_expire_time);

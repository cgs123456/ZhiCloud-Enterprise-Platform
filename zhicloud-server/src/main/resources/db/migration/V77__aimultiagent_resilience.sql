-- ======================== 多 Agent 编排模块：韧性增强 DDL ========================
-- 作者：zhicloud
-- 说明：拓扑配置 schema 版本字段 + 执行日志全链路 traceId 字段（DDL 方案①：加列）
-- 兼容已有数据：新列均为可空，旧数据 version 由代码默认填充 v1，trace_id 由执行时生成。

-- 拓扑配置表：新增配置 schema 版本
ALTER TABLE aimultiagent_topology
    ADD COLUMN version VARCHAR(32) NULL COMMENT '配置 schema 版本（如 v1）' AFTER status;

-- 执行日志表：新增全链路追踪 ID
ALTER TABLE aimultiagent_execution_log
    ADD COLUMN trace_id VARCHAR(64) NULL COMMENT '全链路追踪 ID（跨 Worker / Supervisor 排查）' AFTER duration_ms;

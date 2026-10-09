-- ======================== 多 Agent 编排模块：韧性增强 DDL ========================
-- 作者：zhicloud
-- 说明：拓扑配置 schema 版本字段 + 执行日志全链路 traceId 字段（DDL 方案①：加列）
-- 兼容已有数据：新列均为可空，旧数据 version 由代码默认填充 v1，trace_id 由执行时生成。
--
-- 幂等策略（2026-10-09）：
--   MySQL 不支持 ALTER TABLE ... ADD COLUMN IF NOT EXISTS，因此先查 information_schema，
--   仅在列不存在时通过 PREPARE 执行 ALTER。这样首次执行、半失败重跑和重复执行都不会
--   因 Duplicate column 失败；目标表缺失时 ALTER 会自然失败，保持 fail-closed。

-- 拓扑配置表：新增配置 schema 版本
SET @v77_topology_has_version := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'aimultiagent_topology'
      AND column_name = 'version'
);
SET @v77_topology_sql := IF(
    @v77_topology_has_version = 0,
    'ALTER TABLE `aimultiagent_topology` ADD COLUMN `version` VARCHAR(32) NULL COMMENT ''配置 schema 版本（如 v1）'' AFTER `status`',
    'SELECT 1'
);
PREPARE v77_topology_stmt FROM @v77_topology_sql;
EXECUTE v77_topology_stmt;
DEALLOCATE PREPARE v77_topology_stmt;

-- 执行日志表：新增全链路追踪 ID
SET @v77_execution_log_has_trace_id := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'aimultiagent_execution_log'
      AND column_name = 'trace_id'
);
SET @v77_execution_log_sql := IF(
    @v77_execution_log_has_trace_id = 0,
    'ALTER TABLE `aimultiagent_execution_log` ADD COLUMN `trace_id` VARCHAR(64) NULL COMMENT ''全链路追踪 ID（跨 Worker / Supervisor 排查）'' AFTER `duration_ms`',
    'SELECT 1'
);
PREPARE v77_execution_log_stmt FROM @v77_execution_log_sql;
EXECUTE v77_execution_log_stmt;
DEALLOCATE PREPARE v77_execution_log_stmt;
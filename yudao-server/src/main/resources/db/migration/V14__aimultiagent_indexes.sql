-- ============================================================
-- V14: AIMultiAgent 模块业务表二级索引补充（P0-5）
-- ============================================================
-- 背景：aimultiagent.sql 中 2 张业务表完全无二级索引。
-- 覆盖索引：
--   1. (tenant_id, deleted)         —— 多租户 + 软删除复合过滤
--   2. (create_time)                —— 按创建时间范围查询
--   3. (status)                     —— 业务状态过滤
--   4. (topology_id)                —— 执行日志按拓扑查询
-- 复用幂等存储过程 p_add_index_if_not_exists
-- ============================================================

DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;
CREATE PROCEDURE p_add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END;

-- aimultiagent_topology
CALL p_add_index_if_not_exists('aimultiagent_topology', 'idx_aimultiagent_topology_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('aimultiagent_topology', 'idx_aimultiagent_topology_create_time', 'create_time');
CALL p_add_index_if_not_exists('aimultiagent_topology', 'idx_aimultiagent_topology_status', 'status');

-- aimultiagent_execution_log
CALL p_add_index_if_not_exists('aimultiagent_execution_log', 'idx_aimultiagent_execution_log_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('aimultiagent_execution_log', 'idx_aimultiagent_execution_log_create_time', 'create_time');
CALL p_add_index_if_not_exists('aimultiagent_execution_log', 'idx_aimultiagent_execution_log_status', 'status');
CALL p_add_index_if_not_exists('aimultiagent_execution_log', 'idx_aimultiagent_execution_log_topology_id', 'topology_id');

-- ============================================================
-- 清理存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;

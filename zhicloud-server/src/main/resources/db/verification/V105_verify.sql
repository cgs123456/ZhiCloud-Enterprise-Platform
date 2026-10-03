-- ============================================================
-- V105 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. event_dlq_replay_log 的列、类型、可空性必须与 EventDlqReplayLogDO 对齐。
--   2. (tenant_id, event_id) 与 (tenant_id, create_time) 两个查询索引必须存在且顺序正确。
-- ============================================================

WITH tmp_v105_expected_column AS (
    SELECT 'id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS max_length
    UNION ALL SELECT 'tenant_id', 'bigint', 'NO', NULL
    UNION ALL SELECT 'event_id', 'varchar', 'NO', 64
    UNION ALL SELECT 'operator_id', 'bigint', 'NO', NULL
    UNION ALL SELECT 'operator_name', 'varchar', 'NO', 64
    UNION ALL SELECT 'reason', 'varchar', 'NO', 512
    UNION ALL SELECT 'result', 'varchar', 'NO', 16
    UNION ALL SELECT 'outbox_replayed', 'bit', 'NO', NULL
    UNION ALL SELECT 'inbox_replayed', 'int', 'NO', NULL
    UNION ALL SELECT 'error_message', 'varchar', 'YES', 2000
    UNION ALL SELECT 'creator', 'varchar', 'YES', 64
    UNION ALL SELECT 'create_time', 'datetime', 'NO', NULL
    UNION ALL SELECT 'updater', 'varchar', 'YES', 64
    UNION ALL SELECT 'update_time', 'datetime', 'NO', NULL
    UNION ALL SELECT 'deleted', 'bit', 'NO', NULL
),
actual_column AS (
    SELECT COLUMN_NAME AS column_name,
           LOWER(DATA_TYPE) AS data_type,
           UPPER(IS_NULLABLE) AS is_nullable,
           CHARACTER_MAXIMUM_LENGTH AS max_length
    FROM information_schema.columns
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'event_dlq_replay_log'
)
SELECT e.column_name,
       e.data_type AS expected_type,
       a.data_type AS actual_type,
       e.is_nullable AS expected_nullable,
       a.is_nullable AS actual_nullable,
       e.max_length AS expected_length,
       a.max_length AS actual_length,
       CASE
           WHEN a.column_name IS NULL THEN 'MISSING'
           WHEN a.data_type <> e.data_type
                OR a.is_nullable <> e.is_nullable
                OR NOT (a.max_length <=> e.max_length) THEN 'WRONG'
           ELSE 'OK'
       END AS column_verdict
FROM tmp_v105_expected_column e
LEFT JOIN actual_column a ON a.column_name = e.column_name
ORDER BY e.column_name;

WITH tmp_v105_expected_column AS (
    SELECT 'id' AS column_name, 'bigint' AS data_type, 'NO' AS is_nullable, NULL AS max_length
    UNION ALL SELECT 'tenant_id', 'bigint', 'NO', NULL
    UNION ALL SELECT 'event_id', 'varchar', 'NO', 64
    UNION ALL SELECT 'operator_id', 'bigint', 'NO', NULL
    UNION ALL SELECT 'operator_name', 'varchar', 'NO', 64
    UNION ALL SELECT 'reason', 'varchar', 'NO', 512
    UNION ALL SELECT 'result', 'varchar', 'NO', 16
    UNION ALL SELECT 'outbox_replayed', 'bit', 'NO', NULL
    UNION ALL SELECT 'inbox_replayed', 'int', 'NO', NULL
    UNION ALL SELECT 'error_message', 'varchar', 'YES', 2000
    UNION ALL SELECT 'creator', 'varchar', 'YES', 64
    UNION ALL SELECT 'create_time', 'datetime', 'NO', NULL
    UNION ALL SELECT 'updater', 'varchar', 'YES', 64
    UNION ALL SELECT 'update_time', 'datetime', 'NO', NULL
    UNION ALL SELECT 'deleted', 'bit', 'NO', NULL
),
actual_column AS (
    SELECT COLUMN_NAME AS column_name,
           LOWER(DATA_TYPE) AS data_type,
           UPPER(IS_NULLABLE) AS is_nullable,
           CHARACTER_MAXIMUM_LENGTH AS max_length
    FROM information_schema.columns
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'event_dlq_replay_log'
)
SELECT 'V105_COLUMNS' AS check_name,
       CASE WHEN COUNT(*) = 15
                  AND SUM(CASE
                              WHEN a.column_name IS NOT NULL
                                   AND a.data_type = e.data_type
                                   AND a.is_nullable = e.is_nullable
                                   AND (a.max_length <=> e.max_length)
                              THEN 1 ELSE 0 END) = 15
            THEN 'PASS' ELSE 'FAIL' END AS verdict,
       COUNT(*) AS expected_columns,
       SUM(CASE
               WHEN a.column_name IS NOT NULL
                    AND a.data_type = e.data_type
                    AND a.is_nullable = e.is_nullable
                    AND (a.max_length <=> e.max_length)
               THEN 1 ELSE 0 END) AS correct_columns
FROM tmp_v105_expected_column e
LEFT JOIN actual_column a ON a.column_name = e.column_name;

WITH tmp_v105_expected_index AS (
    SELECT 'idx_dlq_replay_tenant_event' AS index_name, 'tenant_id' AS column_name, 1 AS seq_in_index
    UNION ALL SELECT 'idx_dlq_replay_tenant_event', 'event_id', 2
    UNION ALL SELECT 'idx_dlq_replay_tenant_create', 'tenant_id', 1
    UNION ALL SELECT 'idx_dlq_replay_tenant_create', 'create_time', 2
),
expected_index AS (
    SELECT index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS expected_columns
    FROM tmp_v105_expected_index
    GROUP BY index_name
),
actual_index AS (
    SELECT INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns
    FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'event_dlq_replay_log'
      AND INDEX_NAME IN ('idx_dlq_replay_tenant_event', 'idx_dlq_replay_tenant_create')
    GROUP BY INDEX_NAME
)
SELECT e.index_name,
       e.expected_columns,
       a.actual_columns,
       CASE
           WHEN a.index_name IS NULL THEN 'MISSING'
           WHEN LOWER(a.actual_columns) <> LOWER(e.expected_columns) THEN 'WRONG'
           ELSE 'OK'
       END AS index_verdict
FROM expected_index e
LEFT JOIN actual_index a ON a.index_name = e.index_name
ORDER BY e.index_name;

WITH tmp_v105_expected_index AS (
    SELECT 'idx_dlq_replay_tenant_event' AS index_name, 'tenant_id' AS column_name, 1 AS seq_in_index
    UNION ALL SELECT 'idx_dlq_replay_tenant_event', 'event_id', 2
    UNION ALL SELECT 'idx_dlq_replay_tenant_create', 'tenant_id', 1
    UNION ALL SELECT 'idx_dlq_replay_tenant_create', 'create_time', 2
),
expected_index AS (
    SELECT index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS expected_columns
    FROM tmp_v105_expected_index
    GROUP BY index_name
),
actual_index AS (
    SELECT INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns
    FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'event_dlq_replay_log'
      AND INDEX_NAME IN ('idx_dlq_replay_tenant_event', 'idx_dlq_replay_tenant_create')
    GROUP BY INDEX_NAME
)
SELECT 'V105_INDEXES' AS check_name,
       CASE WHEN COUNT(*) = 2
                  AND SUM(CASE WHEN LOWER(a.actual_columns) = LOWER(e.expected_columns) THEN 1 ELSE 0 END) = 2
            THEN 'PASS' ELSE 'FAIL' END AS verdict,
       COUNT(*) AS expected_indexes,
       SUM(CASE WHEN LOWER(a.actual_columns) = LOWER(e.expected_columns) THEN 1 ELSE 0 END) AS correct_indexes
FROM expected_index e
LEFT JOIN actual_index a ON a.index_name = e.index_name;

SELECT 'V105' AS check_name,
       CASE WHEN
           (SELECT COUNT(*) FROM information_schema.columns
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_dlq_replay_log') = 15
           AND (SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.statistics
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event_dlq_replay_log'
                   AND INDEX_NAME IN ('idx_dlq_replay_tenant_event', 'idx_dlq_replay_tenant_create')) = 2
       THEN 'PASS' ELSE 'FAIL' END AS verdict;

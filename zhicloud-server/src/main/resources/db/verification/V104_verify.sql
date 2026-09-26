-- ============================================================
-- V104 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：可靠集成监控查询所需的两个复合索引必须存在且列顺序正确。
-- ============================================================

WITH tmp_v104_verify_expected_index AS (
    SELECT 'event_outbox' AS table_name, 'idx_status_occurred' AS index_name, 'status' AS column_name, 1 AS seq_in_index
    UNION ALL
    SELECT 'event_outbox', 'idx_status_occurred', 'occurred_at', 2
    UNION ALL
    SELECT 'event_inbox', 'idx_status_received', 'status', 1
    UNION ALL
    SELECT 'event_inbox', 'idx_status_received', 'received_at', 2
),
expected AS (
    SELECT table_name,
           index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS expected_columns
    FROM tmp_v104_verify_expected_index
    GROUP BY table_name, index_name
),
actual AS (
    SELECT TABLE_NAME AS table_name,
           INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns
    FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME, INDEX_NAME) IN (
          ('event_outbox', 'idx_status_occurred'),
          ('event_inbox', 'idx_status_received')
      )
    GROUP BY TABLE_NAME, INDEX_NAME
)
SELECT e.table_name,
       e.index_name,
       e.expected_columns,
       a.actual_columns,
       CASE
           WHEN a.index_name IS NULL THEN 'MISSING'
           WHEN LOWER(a.actual_columns) <> LOWER(e.expected_columns) THEN 'WRONG'
           ELSE 'OK'
       END AS index_verdict
FROM expected e
LEFT JOIN actual a
       ON a.table_name = e.table_name
      AND a.index_name = e.index_name
ORDER BY e.table_name, e.index_name;

WITH tmp_v104_verify_expected_index AS (
    SELECT 'event_outbox' AS table_name, 'idx_status_occurred' AS index_name, 'status' AS column_name, 1 AS seq_in_index
    UNION ALL
    SELECT 'event_outbox', 'idx_status_occurred', 'occurred_at', 2
    UNION ALL
    SELECT 'event_inbox', 'idx_status_received', 'status', 1
    UNION ALL
    SELECT 'event_inbox', 'idx_status_received', 'received_at', 2
),
expected AS (
    SELECT table_name,
           index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS expected_columns
    FROM tmp_v104_verify_expected_index
    GROUP BY table_name, index_name
),
actual AS (
    SELECT TABLE_NAME AS table_name,
           INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns
    FROM information_schema.statistics
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME, INDEX_NAME) IN (
          ('event_outbox', 'idx_status_occurred'),
          ('event_inbox', 'idx_status_received')
      )
    GROUP BY TABLE_NAME, INDEX_NAME
)
SELECT 'V104' AS check_name,
       CASE
           WHEN COUNT(*) = 2
                AND SUM(CASE WHEN LOWER(a.actual_columns) = LOWER(e.expected_columns) THEN 1 ELSE 0 END) = 2
           THEN 'PASS' ELSE 'FAIL'
       END AS v104_verify_verdict,
       COUNT(*) AS expected_indexes,
       SUM(CASE WHEN LOWER(a.actual_columns) = LOWER(e.expected_columns) THEN 1 ELSE 0 END) AS correct_indexes
FROM expected e
LEFT JOIN actual a
       ON a.table_name = e.table_name
      AND a.index_name = e.index_name;

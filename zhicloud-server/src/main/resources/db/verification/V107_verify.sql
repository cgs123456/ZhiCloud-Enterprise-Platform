-- ============================================================
-- V107 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 旧全局唯一键 uk_event_id / uk_consumer_event 不存在；
--   2. 新租户复合唯一键存在、为 UNIQUE 且列顺序正确；
--   3. event_outbox 同租户重复 event_id 为 0；
--   4. event_inbox 同租户同消费者重复 event_id 为 0。
-- ============================================================

WITH tmp_v107_expected_index AS (
    SELECT 'event_outbox' AS table_name, 'uk_event_tenant_event' AS index_name,
           'tenant_id,event_id' AS expected_columns
    UNION ALL
    SELECT 'event_inbox', 'uk_consumer_tenant_event', 'tenant_id,consumer_name,event_id'
),
expected_index AS (
    SELECT table_name, index_name, expected_columns
    FROM tmp_v107_expected_index
),
actual_index AS (
    SELECT table_name,
           index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS actual_columns,
           MIN(non_unique) AS non_unique
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND ((table_name = 'event_outbox' AND index_name = 'uk_event_tenant_event')
        OR (table_name = 'event_inbox' AND index_name = 'uk_consumer_tenant_event'))
    GROUP BY table_name, index_name
)
SELECT e.table_name,
       e.index_name,
       e.expected_columns,
       a.actual_columns,
       a.non_unique,
       CASE
           WHEN a.index_name IS NULL THEN 'MISSING'
           WHEN a.non_unique <> 0 THEN 'NON_UNIQUE'
           WHEN LOWER(a.actual_columns) <> LOWER(e.expected_columns) THEN 'WRONG_COLUMNS'
           ELSE 'OK'
       END AS index_verdict
FROM expected_index e
LEFT JOIN actual_index a
       ON a.table_name = e.table_name
      AND a.index_name = e.index_name
ORDER BY e.table_name, e.index_name;

SELECT 'V107_LEGACY_INDEXES' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'event_outbox'
                    AND index_name = 'uk_event_id') = 0
            AND (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'event_inbox'
                    AND index_name = 'uk_consumer_event') = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V107_DUPLICATES' AS check_name,
       (SELECT COUNT(*) FROM (
           SELECT 1 FROM event_outbox
           GROUP BY tenant_id, event_id
           HAVING COUNT(*) > 1
       ) AS outbox_dups) AS outbox_duplicate_groups,
       (SELECT COUNT(*) FROM (
           SELECT 1 FROM event_inbox
           GROUP BY tenant_id, consumer_name, event_id
           HAVING COUNT(*) > 1
       ) AS inbox_dups) AS inbox_duplicate_groups,
       CASE
           WHEN (SELECT COUNT(*) FROM (
               SELECT 1 FROM event_outbox
               GROUP BY tenant_id, event_id
               HAVING COUNT(*) > 1
           ) AS outbox_dups) = 0
            AND (SELECT COUNT(*) FROM (
               SELECT 1 FROM event_inbox
               GROUP BY tenant_id, consumer_name, event_id
               HAVING COUNT(*) > 1
           ) AS inbox_dups) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

WITH expected_index AS (
    SELECT 'event_outbox' AS table_name, 'uk_event_tenant_event' AS index_name,
           'tenant_id,event_id' AS expected_columns
    UNION ALL
    SELECT 'event_inbox', 'uk_consumer_tenant_event', 'tenant_id,consumer_name,event_id'
),
actual_index AS (
    SELECT table_name,
           index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS actual_columns,
           MIN(non_unique) AS non_unique
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND ((table_name = 'event_outbox' AND index_name = 'uk_event_tenant_event')
        OR (table_name = 'event_inbox' AND index_name = 'uk_consumer_tenant_event'))
    GROUP BY table_name, index_name
)
SELECT 'V107' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'event_outbox'
                    AND index_name = 'uk_event_id') = 0
            AND (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'event_inbox'
                    AND index_name = 'uk_consumer_event') = 0
            AND (SELECT COUNT(*) FROM actual_index
                  WHERE non_unique = 0
                    AND LOWER(actual_columns) = 'tenant_id,event_id') = 1
            AND (SELECT COUNT(*) FROM actual_index
                  WHERE non_unique = 0
                    AND LOWER(actual_columns) = 'tenant_id,consumer_name,event_id') = 1
            AND (SELECT COUNT(*) FROM (
                SELECT 1 FROM event_outbox
                GROUP BY tenant_id, event_id
                HAVING COUNT(*) > 1
            ) AS outbox_dups) = 0
            AND (SELECT COUNT(*) FROM (
                SELECT 1 FROM event_inbox
                GROUP BY tenant_id, consumer_name, event_id
                HAVING COUNT(*) > 1
            ) AS inbox_dups) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

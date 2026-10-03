-- ============================================================
-- V120 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. event_inbox 存在，且 processing_owner、lease_until、claimed_at 三列
--      类型和 nullable 与 V120 定义一致；
--   2. event_outbox 存在，且 processing_owner、lease_until 两列类型和
--      nullable 与 V120 定义一致；
--   3. event_inbox 的 (status, lease_until, id) 为普通 BTREE 索引，列顺序精确；
--   4. event_outbox 的 (status, lease_until, id) 为普通 BTREE 索引，列顺序精确。
-- ============================================================

SELECT 'V120_EVENT_INBOX_OWNER_COLUMNS' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_inbox'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_inbox'
                  AND column_name = 'processing_owner'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 128
                  AND is_nullable = 'YES'
                  AND column_default IS NULL
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_inbox'
                  AND column_name = 'lease_until'
                  AND LOWER(data_type) = 'datetime'
                  AND is_nullable = 'YES'
                  AND column_default IS NULL
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_inbox'
                  AND column_name = 'claimed_at'
                  AND LOWER(data_type) = 'datetime'
                  AND is_nullable = 'YES'
                  AND column_default IS NULL
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V120_EVENT_OUTBOX_OWNER_COLUMNS' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_outbox'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_outbox'
                  AND column_name = 'processing_owner'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 128
                  AND is_nullable = 'YES'
                  AND column_default IS NULL
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_outbox'
                  AND column_name = 'lease_until'
                  AND LOWER(data_type) = 'datetime'
                  AND is_nullable = 'YES'
                  AND column_default IS NULL
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V120_EVENT_INBOX_LEASE_INDEX' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_inbox'
                  AND index_name = 'idx_event_inbox_status_lease'
                GROUP BY table_name, index_name
               HAVING COUNT(*) = 3
                  AND MAX(non_unique) = 1
                  AND SUM(CASE WHEN column_name = 'status' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'lease_until' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'id' AND seq_in_index = 3 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 3
                  AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 3
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V120_EVENT_OUTBOX_LEASE_INDEX' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'event_outbox'
                  AND index_name = 'idx_event_outbox_status_lease'
                GROUP BY table_name, index_name
               HAVING COUNT(*) = 3
                  AND MAX(non_unique) = 1
                  AND SUM(CASE WHEN column_name = 'status' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'lease_until' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'id' AND seq_in_index = 3 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 3
                  AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 3
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

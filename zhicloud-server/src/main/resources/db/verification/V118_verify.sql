-- ============================================================
-- V118 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. bpm_process_instance_idempotency 必须存在且具备关键列；
--   2. uk_bpm_process_instance_idempotency 必须精确为
--      (tenant_id, process_definition_key, business_key) 唯一 BTREE 索引。
-- ============================================================

SELECT 'V118_PROCESS_INSTANCE_IDEMPOTENCY_TABLE' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
                  AND column_name = 'tenant_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
                  AND column_name = 'process_definition_key'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 64
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
                  AND column_name = 'business_key'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 128
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
                  AND column_name = 'process_instance_id'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 255
                  AND is_nullable = 'YES'
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V118_PROCESS_INSTANCE_IDEMPOTENCY_INDEX' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
                  AND index_name = 'uk_bpm_process_instance_idempotency'
                GROUP BY table_name, index_name
               HAVING COUNT(*) = 3
                  AND MAX(non_unique) = 0
                  AND SUM(CASE WHEN column_name = 'tenant_id' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'process_definition_key' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'business_key' AND seq_in_index = 3 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 3
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

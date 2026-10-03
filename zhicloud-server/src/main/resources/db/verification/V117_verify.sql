-- ============================================================
-- V117 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. crm_receivable.idempotency_key 为 utf8mb4_bin 的 VARCHAR(128) NULL；
--   2. crm_receivable.payload_hash 为 ascii_bin 的 CHAR(64) NULL；
--   3. uk_crm_receivable_idempotency 必须精确为 (tenant_id, idempotency_key)
--      唯一 BTREE 索引，不能包含 deleted 或其他列；
--   4. 同租户不得存在重复非空幂等键、键/指纹不成对或非法指纹。
-- ============================================================

SELECT 'V117_CRM_RECEIVABLE_IDEMPOTENCY_COLUMNS' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'crm_receivable'
                  AND column_name = 'idempotency_key'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 128
                  AND is_nullable = 'YES'
                  AND LOWER(character_set_name) = 'utf8mb4'
                  AND LOWER(collation_name) = 'utf8mb4_bin'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'crm_receivable'
                  AND column_name = 'payload_hash'
                  AND LOWER(data_type) = 'char'
                  AND character_maximum_length = 64
                  AND is_nullable = 'YES'
                  AND LOWER(character_set_name) = 'ascii'
                  AND LOWER(collation_name) = 'ascii_bin'
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

WITH expected_index AS (
    SELECT 'crm_receivable' AS table_name,
           'uk_crm_receivable_idempotency' AS index_name,
           'tenant_id,idempotency_key' AS expected_columns,
           2 AS expected_column_count
),
actual_index AS (
    SELECT TABLE_NAME AS table_name,
           INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns,
           COUNT(*) AS actual_column_count,
           MAX(NON_UNIQUE) AS non_unique,
           SUM(CASE WHEN NON_UNIQUE = 0 THEN 1 ELSE 0 END) AS unique_column_rows,
           SUM(CASE WHEN SUB_PART IS NULL THEN 1 ELSE 0 END) AS full_column_rows,
           SUM(CASE WHEN UPPER(INDEX_TYPE) = 'BTREE' THEN 1 ELSE 0 END) AS btree_rows
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'crm_receivable'
       AND index_name = 'uk_crm_receivable_idempotency'
     GROUP BY TABLE_NAME, INDEX_NAME
)
SELECT e.table_name,
       e.index_name,
       e.expected_columns,
       a.actual_columns,
       a.non_unique,
       CASE
           WHEN a.index_name IS NULL THEN 'MISSING'
           WHEN a.actual_column_count <> e.expected_column_count THEN 'WRONG_COLUMN_COUNT'
           WHEN a.non_unique <> 0 OR a.unique_column_rows <> a.actual_column_count THEN 'WRONG_UNIQUENESS'
           WHEN LOWER(a.actual_columns) <> LOWER(e.expected_columns) THEN 'WRONG_COLUMNS'
           WHEN a.full_column_rows <> a.actual_column_count THEN 'WRONG_PREFIX'
           WHEN a.btree_rows <> a.actual_column_count THEN 'WRONG_INDEX_TYPE'
           ELSE 'OK'
       END AS index_verdict
  FROM expected_index e
  LEFT JOIN actual_index a
    ON a.table_name = e.table_name
   AND a.index_name = e.index_name;

SELECT 'V117_CRM_RECEIVABLE_IDEMPOTENCY_INDEX' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'crm_receivable'
                  AND index_name = 'uk_crm_receivable_idempotency'
                GROUP BY table_name, index_name
               HAVING COUNT(*) = 2
                  AND MAX(non_unique) = 0
                  AND SUM(CASE WHEN column_name = 'tenant_id' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'idempotency_key' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 2
                  AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 2
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V117_CRM_RECEIVABLE_IDEMPOTENCY_DATA' AS check_name,
       CASE
           WHEN NOT EXISTS (
               SELECT 1
                 FROM crm_receivable
                WHERE idempotency_key IS NOT NULL
                  AND (CHAR_LENGTH(idempotency_key) = 0 OR idempotency_key <> TRIM(idempotency_key))
           )
           AND NOT EXISTS (
               SELECT 1
                 FROM crm_receivable
                WHERE idempotency_key IS NULL
                  AND payload_hash IS NOT NULL
           )
           AND NOT EXISTS (
               SELECT 1
                 FROM crm_receivable
                WHERE idempotency_key IS NOT NULL
                  AND (payload_hash IS NULL OR CHAR_LENGTH(payload_hash) <> 64
                       OR payload_hash NOT REGEXP '^[0-9a-fA-F]{64}$')
           )
           AND NOT EXISTS (
               SELECT 1
                 FROM crm_receivable
                WHERE idempotency_key IS NOT NULL
                GROUP BY tenant_id, idempotency_key
               HAVING COUNT(*) > 1
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;
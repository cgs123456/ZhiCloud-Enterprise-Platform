-- ============================================================
-- V116 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. system_users.active_username 必须是 STORED 生成列，表达式按 deleted=0
--      取 username，软删除行为 NULL；
--   2. uk_system_users_tenant_active_username 必须是 (tenant_id, active_username)
--      的唯一索引；
--   3. 当前库不得存在同一 tenant_id 下的活跃重复 username。
-- ============================================================

SELECT 'V116_ACTIVE_USERNAME_COLUMN' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'system_users'
                  AND column_name = 'active_username'
                  AND LOWER(data_type) = 'varchar'
                  AND extra LIKE '%STORED GENERATED%'
                  AND generation_expression IS NOT NULL
                  AND character_maximum_length = 30
                  AND LOWER(
                      REPLACE(
                        REPLACE(
                          REPLACE(
                            REPLACE(
                              REPLACE(
                                REPLACE(
                                  REPLACE(generation_expression, '`', ''),
                                  ' ', ''),
                                CHAR(9), ''),
                              CHAR(10), ''),
                            CHAR(13), ''),
                          '(', ''),
                        ')', '')
                  ) = 'casewhendeleted=0thenusernameelsenullend'
           ) THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

WITH expected_index AS (
    SELECT 'system_users' AS table_name,
           'uk_system_users_tenant_active_username' AS index_name,
           'tenant_id,active_username' AS expected_columns,
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
       AND table_name = 'system_users'
       AND index_name = 'uk_system_users_tenant_active_username'
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

SELECT 'V116_ACTIVE_USERNAME_UNIQUE_INDEX' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'system_users'
                  AND index_name = 'uk_system_users_tenant_active_username'
                GROUP BY table_name, index_name
               HAVING COUNT(*) = 2
                  AND MAX(non_unique) = 0
                  AND SUM(CASE WHEN column_name = 'tenant_id' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'active_username' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 2
                  AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 2
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V116_ACTIVE_USERNAME_NO_DUPLICATES' AS check_name,
       CASE WHEN (
           SELECT COUNT(*)
             FROM (
                 SELECT tenant_id, username
                   FROM system_users
                  WHERE deleted = 0
                  GROUP BY tenant_id, username
                 HAVING COUNT(*) > 1
             ) duplicates
       ) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict;

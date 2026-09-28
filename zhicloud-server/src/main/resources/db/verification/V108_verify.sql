-- ============================================================
-- V108 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. crm_clue_pool_config.tenant_id 必须存在、bigint、NOT NULL、默认 0。
--   2. crm_clue_pool_config 的租户索引必须精确为 (tenant_id, deleted) 普通索引。
--   3. system_sms_code 的验证码查询索引必须精确为 (tenant_id, mobile, id) 普通索引。
--   4. crm_clue_pool_config 不允许残留 tenant_id IS NULL 的历史行。
--
-- 最终 V108 汇总与上方明细使用同一组精确索引断言，避免“首列正确但列顺序错误”
-- 或“索引被改成唯一索引”仍然误报 PASS。
-- ============================================================

SELECT 'V108_CRM_CLUE_POOL_CONFIG_COLUMN' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'crm_clue_pool_config'
                  AND column_name = 'tenant_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
                  AND CAST(column_default AS CHAR) = '0'
           ) THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V108_CRM_CLUE_POOL_CONFIG_NULLS' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM crm_clue_pool_config WHERE tenant_id IS NULL) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

WITH expected_index AS (
    SELECT 'crm_clue_pool_config' AS table_name,
           'idx_crm_clue_pool_config_tenant' AS index_name,
           'tenant_id,deleted' AS expected_columns,
           2 AS expected_column_count,
           1 AS expected_non_unique
    UNION ALL
    SELECT 'system_sms_code',
           'idx_sms_code_tenant_mobile_id',
           'tenant_id,mobile,id',
           3,
           1
),
actual_index AS (
    SELECT TABLE_NAME AS table_name,
           INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns,
           COUNT(*) AS actual_column_count,
           MAX(NON_UNIQUE) AS non_unique,
           SUM(CASE WHEN NON_UNIQUE = 1 THEN 1 ELSE 0 END) AS non_unique_column_rows
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND ((table_name = 'crm_clue_pool_config'
             AND index_name = 'idx_crm_clue_pool_config_tenant')
         OR (table_name = 'system_sms_code'
             AND index_name = 'idx_sms_code_tenant_mobile_id'))
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
           WHEN a.non_unique <> e.expected_non_unique
                OR a.non_unique_column_rows <> a.actual_column_count THEN 'WRONG_UNIQUENESS'
           WHEN LOWER(a.actual_columns) <> LOWER(e.expected_columns) THEN 'WRONG_COLUMNS'
           ELSE 'OK'
       END AS index_verdict
  FROM expected_index e
  LEFT JOIN actual_index a
    ON a.table_name = e.table_name
   AND a.index_name = e.index_name
 ORDER BY e.table_name, e.index_name;

WITH expected_index AS (
    SELECT 'crm_clue_pool_config' AS table_name,
           'idx_crm_clue_pool_config_tenant' AS index_name,
           'tenant_id,deleted' AS expected_columns,
           2 AS expected_column_count,
           1 AS expected_non_unique
    UNION ALL
    SELECT 'system_sms_code',
           'idx_sms_code_tenant_mobile_id',
           'tenant_id,mobile,id',
           3,
           1
),
actual_index AS (
    SELECT TABLE_NAME AS table_name,
           INDEX_NAME AS index_name,
           GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS actual_columns,
           COUNT(*) AS actual_column_count,
           MAX(NON_UNIQUE) AS non_unique,
           SUM(CASE WHEN NON_UNIQUE = 1 THEN 1 ELSE 0 END) AS non_unique_column_rows
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND ((table_name = 'crm_clue_pool_config'
             AND index_name = 'idx_crm_clue_pool_config_tenant')
         OR (table_name = 'system_sms_code'
             AND index_name = 'idx_sms_code_tenant_mobile_id'))
     GROUP BY TABLE_NAME, INDEX_NAME
)
SELECT 'V108' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'crm_clue_pool_config'
                  AND column_name = 'tenant_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
                  AND CAST(column_default AS CHAR) = '0'
           )
            AND (SELECT COUNT(*) FROM crm_clue_pool_config WHERE tenant_id IS NULL) = 0
            AND (SELECT COUNT(*)
                   FROM expected_index e
                   JOIN actual_index a
                     ON a.table_name = e.table_name
                    AND a.index_name = e.index_name
                  WHERE LOWER(a.actual_columns) = LOWER(e.expected_columns)
                    AND a.actual_column_count = e.expected_column_count
                    AND a.non_unique = e.expected_non_unique
                    AND a.non_unique_column_rows = a.actual_column_count) = 2
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

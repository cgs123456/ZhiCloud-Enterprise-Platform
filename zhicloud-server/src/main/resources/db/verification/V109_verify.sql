-- ============================================================
-- V109 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 旧全局唯一键 uk_no 不存在；
--   2. 不存在其他仅以 no 为唯一列的 UNIQUE 索引；
--   3. uk_tenant_no 存在、为 UNIQUE 且列顺序精确为 (tenant_id, no)；
--   4. (tenant_id, no) 无重复值。
-- ============================================================

WITH expected_index AS (
    SELECT 'wms_shipment_order' AS table_name,
           'uk_tenant_no' AS index_name,
           'tenant_id,no' AS expected_columns,
           2 AS expected_column_count,
           0 AS expected_non_unique
),
actual_index AS (
    SELECT table_name,
           index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS actual_columns,
           COUNT(*) AS actual_column_count,
           MAX(non_unique) AS actual_non_unique,
           SUM(CASE WHEN non_unique = 0 THEN 1 ELSE 0 END) AS unique_column_rows
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'wms_shipment_order'
       AND index_name = 'uk_tenant_no'
     GROUP BY table_name, index_name
)
SELECT e.table_name,
       e.index_name,
       e.expected_columns,
       a.actual_columns,
       a.actual_non_unique,
       CASE
           WHEN a.index_name IS NULL THEN 'MISSING'
           WHEN a.actual_column_count <> e.expected_column_count THEN 'WRONG_COLUMN_COUNT'
           WHEN a.actual_non_unique <> e.expected_non_unique
                OR a.unique_column_rows <> a.actual_column_count THEN 'WRONG_UNIQUENESS'
           WHEN LOWER(a.actual_columns) <> LOWER(e.expected_columns) THEN 'WRONG_COLUMNS'
           ELSE 'OK'
       END AS index_verdict
  FROM expected_index e
  LEFT JOIN actual_index a
    ON a.table_name = e.table_name
   AND a.index_name = e.index_name;

SELECT 'V109_LEGACY_INDEXES' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'wms_shipment_order'
                    AND index_name = 'uk_no') = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V109_OTHER_GLOBAL_NO_UNIQUE_INDEXES' AS check_name,
       COUNT(*) AS offending_index_count,
       CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS verdict
  FROM (
      SELECT index_name
        FROM information_schema.statistics
       WHERE table_schema = DATABASE()
         AND table_name = 'wms_shipment_order'
         AND non_unique = 0
       GROUP BY index_name
      HAVING COUNT(*) = 1
         AND LOWER(GROUP_CONCAT(column_name ORDER BY seq_in_index)) = 'no'
  ) AS global_no_indexes;

SELECT 'V109_DUPLICATES' AS check_name,
       (SELECT COUNT(*) FROM (
           SELECT 1
             FROM wms_shipment_order
            GROUP BY tenant_id, no
           HAVING COUNT(*) > 1
       ) AS duplicate_groups) AS duplicate_groups,
       CASE
           WHEN (SELECT COUNT(*) FROM (
               SELECT 1
                 FROM wms_shipment_order
                GROUP BY tenant_id, no
               HAVING COUNT(*) > 1
           ) AS duplicate_groups) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

WITH expected_index AS (
    SELECT 'wms_shipment_order' AS table_name,
           'uk_tenant_no' AS index_name,
           'tenant_id,no' AS expected_columns
),
actual_index AS (
    SELECT table_name,
           index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS actual_columns,
           COUNT(*) AS actual_column_count,
           MAX(non_unique) AS actual_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = 'wms_shipment_order'
       AND index_name = 'uk_tenant_no'
     GROUP BY table_name, index_name
)
SELECT 'V109' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'wms_shipment_order'
                    AND index_name = 'uk_no') = 0
            AND (SELECT COUNT(*)
                   FROM (
                       SELECT index_name
                         FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = 'wms_shipment_order'
                          AND non_unique = 0
                        GROUP BY index_name
                       HAVING COUNT(*) = 1
                          AND LOWER(GROUP_CONCAT(column_name ORDER BY seq_in_index)) = 'no'
                   ) AS global_no_indexes) = 0
            AND (SELECT COUNT(*)
                   FROM expected_index e
                   JOIN actual_index a
                     ON a.table_name = e.table_name
                    AND a.index_name = e.index_name
                  WHERE LOWER(a.actual_columns) = LOWER(e.expected_columns)
                    AND a.actual_column_count = 2
                    AND a.actual_non_unique = 0) = 1
            AND (SELECT COUNT(*) FROM (
                SELECT 1
                  FROM wms_shipment_order
                 GROUP BY tenant_id, no
                HAVING COUNT(*) > 1
            ) AS duplicate_groups) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

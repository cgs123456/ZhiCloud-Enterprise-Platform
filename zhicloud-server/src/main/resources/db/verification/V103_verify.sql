-- ============================================================
-- V103 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：wms_receipt_order.type / wms_shipment_order.type
--       必须是 SMALLINT 且 NOT NULL，避免 100/200 段类型再次被 TINYINT 截断。
-- ============================================================

WITH expected AS (
    SELECT 'wms_receipt_order'   AS table_name, 'type' AS column_name, 'smallint' AS expected_type, 'NO' AS expected_nullable
    UNION ALL
    SELECT 'wms_shipment_order' AS table_name, 'type' AS column_name, 'smallint' AS expected_type, 'NO' AS expected_nullable
)
SELECT e.table_name,
       e.column_name,
       e.expected_type,
       c.COLUMN_TYPE AS actual_type,
       c.IS_NULLABLE AS actual_nullable,
       CASE
           WHEN c.COLUMN_NAME IS NULL THEN 'MISSING'
           WHEN LOWER(c.DATA_TYPE) <> LOWER(e.expected_type)
                OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG'
           ELSE 'OK'
       END AS column_verdict
FROM expected e
LEFT JOIN information_schema.columns c
       ON c.TABLE_SCHEMA = DATABASE()
      AND c.TABLE_NAME = e.table_name
      AND c.COLUMN_NAME = e.column_name
ORDER BY e.table_name;

WITH expected AS (
    SELECT 'wms_receipt_order'   AS table_name, 'type' AS column_name, 'smallint' AS expected_type, 'NO' AS expected_nullable
    UNION ALL
    SELECT 'wms_shipment_order' AS table_name, 'type' AS column_name, 'smallint' AS expected_type, 'NO' AS expected_nullable
)
SELECT 'V103' AS check_name,
       CASE WHEN COUNT(*) = 2
                  AND SUM(CASE
                              WHEN c.COLUMN_NAME IS NOT NULL
                                   AND LOWER(c.DATA_TYPE) = LOWER(e.expected_type)
                                   AND UPPER(c.IS_NULLABLE) = UPPER(e.expected_nullable)
                              THEN 1 ELSE 0 END) = 2
            THEN 'PASS' ELSE 'FAIL' END AS verdict,
       COUNT(*) AS expected_columns,
       SUM(CASE
               WHEN c.COLUMN_NAME IS NOT NULL
                    AND LOWER(c.DATA_TYPE) = LOWER(e.expected_type)
                    AND UPPER(c.IS_NULLABLE) = UPPER(e.expected_nullable)
               THEN 1 ELSE 0 END) AS correct_columns
FROM expected e
LEFT JOIN information_schema.columns c
       ON c.TABLE_SCHEMA = DATABASE()
      AND c.TABLE_NAME = e.table_name
      AND c.COLUMN_NAME = e.column_name;

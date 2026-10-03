-- ============================================================
-- V112 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 9 张表必须存在；
--   2. 每张表的 id 必须存在且为 AUTO_INCREMENT，tenant_id 必须存在；
--   3. 本迁移声明的唯一键/普通索引必须存在、列顺序正确且唯一性正确。
-- ============================================================
WITH expected_table AS (
    SELECT 'qms_document' AS table_name UNION ALL
    SELECT 'qms_document_distribute' AS table_name UNION ALL
    SELECT 'qms_document_change_request' AS table_name UNION ALL
    SELECT 'qms_audit_plan' AS table_name UNION ALL
    SELECT 'qms_audit_plan_auditor' AS table_name UNION ALL
    SELECT 'qms_audit_report' AS table_name UNION ALL
    SELECT 'qms_audit_nonconformity' AS table_name UNION ALL
    SELECT 'qms_instrument' AS table_name UNION ALL
    SELECT 'qms_instrument_calibration' AS table_name
)
,
actual_table AS (
    SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()
),
expected_column AS (
    SELECT 'qms_document' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_document' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_document_distribute' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_document_distribute' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_document_change_request' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_document_change_request' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_plan' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_plan' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_plan_auditor' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_plan_auditor' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_report' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_report' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_nonconformity' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_audit_nonconformity' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_instrument' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_instrument' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'qms_instrument_calibration' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'qms_instrument_calibration' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment
)
,
actual_column AS (
    SELECT table_name, column_name,
           CASE WHEN LOWER(extra) LIKE '%auto_increment%' THEN 1 ELSE 0 END AS is_auto_increment
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
),
expected_index AS (
    SELECT 'qms_document' AS table_name, 'uk_tenant_doc_no' AS index_name, 'tenant_id,doc_no' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'qms_document' AS table_name, 'idx_tenant_doc_type' AS index_name, 'tenant_id,doc_type' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_document' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_document_distribute' AS table_name, 'idx_tenant_document' AS index_name, 'tenant_id,document_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_document_change_request' AS table_name, 'idx_tenant_document' AS index_name, 'tenant_id,document_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_document_change_request' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_plan' AS table_name, 'uk_tenant_plan_no' AS index_name, 'tenant_id,plan_no' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_plan' AS table_name, 'idx_tenant_audit_type' AS index_name, 'tenant_id,audit_type' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_plan' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_plan_auditor' AS table_name, 'idx_tenant_plan' AS index_name, 'tenant_id,plan_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_plan_auditor' AS table_name, 'idx_tenant_auditor' AS index_name, 'tenant_id,auditor_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_report' AS table_name, 'uk_tenant_report_no' AS index_name, 'tenant_id,report_no' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_report' AS table_name, 'idx_tenant_plan' AS index_name, 'tenant_id,plan_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_nonconformity' AS table_name, 'uk_tenant_nc_no' AS index_name, 'tenant_id,nc_no' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_nonconformity' AS table_name, 'idx_tenant_report' AS index_name, 'tenant_id,report_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_audit_nonconformity' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_instrument' AS table_name, 'uk_tenant_code' AS index_name, 'tenant_id,code' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'qms_instrument' AS table_name, 'idx_tenant_next_calibration' AS index_name, 'tenant_id,next_calibration_date' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_instrument' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_instrument_calibration' AS table_name, 'uk_tenant_calibration_no' AS index_name, 'tenant_id,calibration_no' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'qms_instrument_calibration' AS table_name, 'idx_tenant_instrument' AS index_name, 'tenant_id,instrument_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'qms_instrument_calibration' AS table_name, 'idx_tenant_calibration_date' AS index_name, 'tenant_id,calibration_date' AS expected_columns, 1 AS expected_non_unique
)
,
actual_index AS (
    SELECT table_name, index_name,
           GROUP_CONCAT(column_name ORDER BY seq_in_index) AS actual_columns,
           MAX(non_unique) AS actual_non_unique
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
     GROUP BY table_name, index_name
)
SELECT 'V112' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM expected_table e JOIN actual_table a ON a.table_name = e.table_name) <> 9 THEN 'FAIL'
           WHEN (SELECT COUNT(*) FROM expected_column e JOIN actual_column a ON a.table_name = e.table_name AND a.column_name = e.column_name WHERE e.require_auto_increment = 0 OR a.is_auto_increment = 1) <> 18 THEN 'FAIL'
           WHEN (SELECT COUNT(*) FROM expected_index e JOIN actual_index a ON a.table_name = e.table_name AND a.index_name = e.index_name WHERE LOWER(a.actual_columns) = LOWER(e.expected_columns) AND a.actual_non_unique = e.expected_non_unique) <> 22 THEN 'FAIL'
           ELSE 'PASS'
       END AS verdict,
       9 AS expected_tables,
       18 AS expected_columns,
       22 AS expected_indexes;

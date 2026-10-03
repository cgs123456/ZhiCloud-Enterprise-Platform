-- ============================================================
-- V114 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 3 张 AI 元数据表必须存在；
--   2. 每张表的 id 必须存在且为 AUTO_INCREMENT，tenant_id 必须存在；
--   3. 本迁移声明的唯一键/普通索引必须存在、列顺序正确且唯一性正确。
-- ============================================================
WITH expected_table AS (
    SELECT 'ai_prompt_template' AS table_name UNION ALL
    SELECT 'airag_knowledge' AS table_name UNION ALL
    SELECT 'airag_document' AS table_name
)
,
actual_table AS (
    SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()
),
expected_column AS (
    SELECT 'ai_prompt_template' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'ai_prompt_template' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'airag_knowledge' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'airag_knowledge' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment UNION ALL
    SELECT 'airag_document' AS table_name, 'id' AS column_name, 1 AS require_auto_increment UNION ALL
    SELECT 'airag_document' AS table_name, 'tenant_id' AS column_name, 0 AS require_auto_increment
)
,
actual_column AS (
    SELECT table_name, column_name,
           CASE WHEN LOWER(extra) LIKE '%auto_increment%' THEN 1 ELSE 0 END AS is_auto_increment
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
),
expected_index AS (
    SELECT 'ai_prompt_template' AS table_name, 'uk_tenant_code' AS index_name, 'tenant_id,code' AS expected_columns, 0 AS expected_non_unique UNION ALL
    SELECT 'airag_knowledge' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'airag_knowledge' AS table_name, 'idx_tenant_name' AS index_name, 'tenant_id,name' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'airag_document' AS table_name, 'idx_tenant_knowledge' AS index_name, 'tenant_id,knowledge_id' AS expected_columns, 1 AS expected_non_unique UNION ALL
    SELECT 'airag_document' AS table_name, 'idx_tenant_status' AS index_name, 'tenant_id,status' AS expected_columns, 1 AS expected_non_unique
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
SELECT 'V114' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM expected_table e JOIN actual_table a ON a.table_name = e.table_name) <> 3 THEN 'FAIL'
           WHEN (SELECT COUNT(*) FROM expected_column e JOIN actual_column a ON a.table_name = e.table_name AND a.column_name = e.column_name WHERE e.require_auto_increment = 0 OR a.is_auto_increment = 1) <> 6 THEN 'FAIL'
           WHEN (SELECT COUNT(*) FROM expected_index e JOIN actual_index a ON a.table_name = e.table_name AND a.index_name = e.index_name WHERE LOWER(a.actual_columns) = LOWER(e.expected_columns) AND a.actual_non_unique = e.expected_non_unique) <> 5 THEN 'FAIL'
           ELSE 'PASS'
       END AS verdict,
       3 AS expected_tables,
       6 AS expected_columns,
       5 AS expected_indexes;

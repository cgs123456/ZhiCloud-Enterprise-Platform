-- ============================================================
-- V125 人工复核脚本（只读）
--
-- 门禁：会员中心 11 张表齐备，且都有 tenant_id 与自增主键
--       （租户拦截器对全部表追加 tenant_id；id-type 对 MySQL 解析为 AUTO）。
-- ============================================================

SELECT 'V125_MEMBER_TABLE_COUNT' AS check_name,
       CASE WHEN COUNT(*) = 11 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(*), ' / 11') AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'member\_%';

SELECT 'V125_MEMBER_TENANT_COVERAGE' AS check_name,
       CASE WHEN COUNT(DISTINCT table_name) = 11 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(DISTINCT table_name), ' / 11') AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'member\_%'
   AND column_name = 'tenant_id';

SELECT 'V125_MEMBER_AUTO_INCREMENT' AS check_name,
       CASE WHEN COUNT(DISTINCT table_name) = 11 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(DISTINCT table_name), ' / 11') AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'member\_%'
   AND column_name = 'id'
   AND EXTRA LIKE '%auto_increment%';

-- ============================================================
-- V129 人工复核脚本（只读）
--
-- 门禁：GoView 项目表必须存在且自增主键齐备
--       （report_go_view_project 此前只有模块内 H2 脚本，没有 Flyway 迁移）。
-- ============================================================

SELECT 'V129_GO_VIEW_PROJECT_PRESENT' AS check_name,
       CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(*), ' / 1') AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name = 'report_go_view_project';

SELECT 'V129_GO_VIEW_PROJECT_AUTO_INCREMENT' AS check_name,
       CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(*), ' / 1') AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'report_go_view_project'
   AND column_name = 'id'
   AND EXTRA LIKE '%auto_increment%';

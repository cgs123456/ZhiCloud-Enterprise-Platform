-- ============================================================
-- V217 验证脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 迁移 V217 已在 flyway_schema_history 成功记录；
--   2. MES BOM 菜单（id=9560）的组件路径与前端视图目录大小写一致（mes/md/Bom/index）；
--   3. 不再存在旧的小写写法 mes/md/bom/index。
--
-- 注意：库表默认排序规则 utf8mb4_unicode_ci 大小写不敏感，
--      这里必须用 BINARY 比较，否则"大小写已修正"会被误判为通过。
-- ============================================================

SELECT 'V217_FLYWAY_APPLIED' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM flyway_schema_history
                  WHERE version = '217' AND success = 1) = 1
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V217_MES_BOM_COMPONENT' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM `system_menu`
                  WHERE `id` = 9560
                    AND BINARY `component` = 'mes/md/Bom/index') = 1
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V217_MES_BOM_NO_LOWERCASE' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM `system_menu`
                   WHERE BINARY `component` = 'mes/md/bom/index') = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

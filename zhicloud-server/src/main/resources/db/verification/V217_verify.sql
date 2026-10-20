-- V217 验证：MES BOM 菜单的组件路径与前端视图目录大小写一致
-- 迁移执行自检；生产仍需要备份、回滚演练与独立复核。
-- 注意：库表默认排序规则 utf8mb4_unicode_ci 大小写不敏感，这里必须用 BINARY 比较才能验出大小写差异。

-- 1. Flyway 成功记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '217' AND success = 1) = 1
            THEN 'V217_FLYWAY_APPLIED PASS'
            ELSE 'V217_FLYWAY_APPLIED FAIL 版本 217 不在成功记录中' END AS check_result;

-- 2. 菜单组件已指向存在的视图目录（大小写一致）
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_menu`
                   WHERE `id` = 9560
                     AND BINARY `component` = 'mes/md/Bom/index') = 1
            THEN 'V217_MES_BOM_COMPONENT PASS'
            ELSE 'V217_MES_BOM_COMPONENT FAIL 菜单 9560 的组件路径未修正' END AS check_result;

-- 3. 旧的大小写错误写法已不存在
SELECT CASE WHEN COUNT(*) = 0
            THEN 'V217_MES_BOM_NO_LOWERCASE PASS'
            ELSE CONCAT('V217_MES_BOM_NO_LOWERCASE FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE BINARY `component` = 'mes/md/bom/index';

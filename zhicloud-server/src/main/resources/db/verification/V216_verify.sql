-- ============================================================
-- V216 验证脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 迁移 V216 已在 flyway_schema_history 成功记录；
--   2. crm_admin（role_id=3）不再持有 V1.1 种子误绑的 MES 按钮 5811；
--   3. CRM 根菜单已绑定到 role 3（若实例已裁剪掉 CRM 菜单则不做要求）；
--   4. CRM 菜单树在 role 3 上没有缺失绑定；
--   5. role 3 没有重复的有效菜单绑定。
--
-- 说明：部署期由 DatabaseMigrationRunner 的迁移后语义校验执行，
--      任何一项非 PASS 都会让 PreSync 迁移 Job 失败并阻断业务发布。
-- ============================================================

SELECT 'V216_FLYWAY_APPLIED' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM flyway_schema_history
                  WHERE version = '216' AND success = 1) = 1
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V216_CRM_ROLE_MES_LEAK' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM `system_role_menu`
                  WHERE `role_id` = 3
                    AND `menu_id` = 5811
                    AND `deleted` = b'0') = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V216_CRM_ROOT' AS check_name,
       CASE
           WHEN NOT EXISTS (SELECT 1 FROM `system_menu`
                             WHERE `path` = '/crm'
                               AND `parent_id` = 0
                               AND `deleted` = b'0')
           THEN 'PASS'
           WHEN EXISTS (SELECT 1
                          FROM `system_role_menu` rm
                          JOIN `system_menu` m ON m.`id` = rm.`menu_id`
                         WHERE rm.`role_id` = 3
                           AND rm.`deleted` = b'0'
                           AND m.`path` = '/crm'
                           AND m.`parent_id` = 0
                           AND m.`deleted` = b'0')
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

WITH RECURSIVE `crm_menu` (`id`) AS (
    SELECT `id`
      FROM `system_menu`
     WHERE `path` = '/crm'
       AND `parent_id` = 0
       AND `deleted` = b'0'
    UNION ALL
    SELECT m.`id`
      FROM `system_menu` m
      JOIN `crm_menu` c ON m.`parent_id` = c.`id`
     WHERE m.`deleted` = b'0'
)
SELECT 'V216_CRM_MENUS_COMPLETE' AS check_name,
       CASE
           WHEN (SELECT COUNT(*)
                   FROM `crm_menu` c
                  WHERE NOT EXISTS (SELECT 1
                                      FROM `system_role_menu` rm
                                     WHERE rm.`role_id` = 3
                                       AND rm.`menu_id` = c.`id`
                                       AND rm.`deleted` = b'0')) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V216_CRM_ROLE_NO_DUPLICATE' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM (
                     SELECT `menu_id`
                       FROM `system_role_menu`
                      WHERE `role_id` = 3
                        AND `deleted` = b'0'
                      GROUP BY `menu_id`
                     HAVING COUNT(*) > 1
                   ) dup) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

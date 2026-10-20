-- V216 验证：crm_admin 角色已获得完整 CRM 菜单树，且不再持有 MES 按钮
-- 迁移执行自检；生产仍需要备份、回滚演练与独立复核。

-- 1. Flyway 成功记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '216' AND success = 1) = 1
            THEN 'V216_FLYWAY_APPLIED PASS'
            ELSE 'V216_FLYWAY_APPLIED FAIL 版本 216 不在成功记录中' END AS check_result;

-- 2. role 3 不再绑定 MES 领料出库单完成按钮
SELECT CASE WHEN COUNT(*) = 0
            THEN 'V216_CRM_ROLE_MES_LEAK PASS'
            ELSE CONCAT('V216_CRM_ROLE_MES_LEAK FAIL ', COUNT(*)) END AS check_result
  FROM `system_role_menu`
 WHERE `role_id` = 3
   AND `menu_id` = 5811
   AND `deleted` = b'0';

-- 3. CRM 根菜单已绑定
SELECT CASE WHEN COUNT(*) = 1
            THEN 'V216_CRM_ROOT PASS'
            ELSE CONCAT('V216_CRM_ROOT FAIL ', COUNT(*)) END AS check_result
  FROM `system_role_menu` rm
  JOIN `system_menu` m ON m.`id` = rm.`menu_id`
 WHERE rm.`role_id` = 3
   AND rm.`deleted` = b'0'
   AND m.`path` = '/crm'
   AND m.`parent_id` = 0;

-- 4. CRM 菜单树没有缺失绑定
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
SELECT CASE WHEN COUNT(*) = 0
            THEN 'V216_CRM_MENUS COMPLETE PASS'
            ELSE CONCAT('V216_CRM_MENUS COMPLETE FAIL missing=', COUNT(*)) END AS check_result
  FROM `crm_menu` c
 WHERE NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` rm
        WHERE rm.`role_id` = 3
          AND rm.`menu_id` = c.`id`
          AND rm.`deleted` = b'0'
   );

-- 5. role 3 没有重复的有效菜单绑定
SELECT CASE WHEN COUNT(*) = 0
            THEN 'V216_CRM_ROLE_NO_DUPLICATE PASS'
            ELSE CONCAT('V216_CRM_ROLE_NO_DUPLICATE FAIL groups=', COUNT(*)) END AS check_result
  FROM (
       SELECT `menu_id`, COUNT(*) AS cnt
         FROM `system_role_menu`
        WHERE `role_id` = 3
          AND `deleted` = b'0'
        GROUP BY `menu_id`
       HAVING cnt > 1
   ) dup;

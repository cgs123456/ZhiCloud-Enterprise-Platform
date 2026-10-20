-- V216 修复 crm_admin 种子角色的菜单绑定
--
-- 背景：V1.1 基线中 role_id=3（crm_admin，CRM 管理员）只有一条角色菜单绑定
--       (menu_id=5811, mes:wm-product-issue:finish)，属于 MES 按钮，不是 CRM 菜单。
--       该角色登录后菜单为空，无法进入 CRM 任何页面。
--
-- 处理：
--   1. 删除 role 3 误绑定的 MES 按钮 5811（仅当该菜单仍是该 MES 权限时）。
--   2. 以 path='/crm' 的根菜单为锚点，递归授予整棵 CRM 菜单树。
--   3. 仅补齐缺失绑定，不覆盖客户后续对 role 3 的其他定制；重复执行安全。
--
-- 回滚：删除本迁移为 role 3 新增的 CRM 菜单绑定，并恢复 role 3 的 5811 绑定。

DELETE rm
  FROM `system_role_menu` rm
  JOIN `system_menu` m ON m.`id` = rm.`menu_id`
 WHERE rm.`role_id` = 3
   AND rm.`menu_id` = 5811
   AND rm.`deleted` = b'0'
   AND m.`permission` = 'mes:wm-product-issue:finish';

INSERT INTO `system_role_menu`
    (`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
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
SELECT 3,
       c.`id`,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       1
  FROM `crm_menu` c
 WHERE EXISTS (
       SELECT 1
         FROM `system_role` r
        WHERE r.`id` = 3
          AND r.`deleted` = b'0'
   )
   AND NOT EXISTS (
       SELECT 1
         FROM `system_role_menu` rm
        WHERE rm.`role_id` = 3
          AND rm.`menu_id` = c.`id`
          AND rm.`deleted` = b'0'
   );

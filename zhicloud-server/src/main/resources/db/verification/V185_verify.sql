-- V185 验证：旧 /hr 菜单退役、HRM 树与企业域菜单完好
-- 1. 旧 /hr 菜单（目录 / 旧权限点 / 旧组件）清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V185_NO_HR_MENUS PASS'
            ELSE CONCAT('V185_NO_HR_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND ( `path` = '/hr' OR `permission` LIKE 'hr:%' OR `component` LIKE 'hr/%' );

-- 2. HRM 根目录与职位菜单仍在
SELECT CASE WHEN COUNT(*) >= 2 THEN 'V185_HRM_MENUS_INTACT PASS'
            ELSE CONCAT('V185_HRM_MENUS_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND ( `path` = '/hrm' OR `component` = 'hrm/position/index' );

-- 3. 菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V185_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V185_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m
 WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

-- 4. 企业域菜单总量仍在合理量级
SELECT CASE WHEN COUNT(*) > 200 THEN CONCAT('V185_ENTERPRISE_MENUS_INTACT PASS ', COUNT(*))
            ELSE CONCAT('V185_ENTERPRISE_MENUS_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0';

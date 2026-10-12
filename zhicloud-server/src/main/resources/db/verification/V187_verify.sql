-- V187 验证：纯动作/分析型域菜单与按钮权限落库

-- 自检 1：本次新增菜单/按钮全部落库
SELECT CASE WHEN COUNT(*) = 27 THEN 'V187_ACTION_MENUS PASS'
            ELSE CONCAT('V187_ACTION_MENUS FAIL ', COUNT(*), '/27') END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0' AND `id` BETWEEN 9700 AND 9726;

-- 自检 2：菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V187_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V187_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

-- 自检 3：企业域菜单总量持续增长（防误删）
SELECT CASE WHEN COUNT(*) > 1930 THEN CONCAT('V187_ENTERPRISE_MENUS PASS ', COUNT(*))
            ELSE CONCAT('V187_ENTERPRISE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0';

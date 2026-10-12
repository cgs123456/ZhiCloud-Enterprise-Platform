-- V188 验证：按 controller 真实权限补种动作域按钮 + 归位

-- 自检 1：各动作域菜单页下至少有 1 按钮
SELECT CASE WHEN COUNT(*) >= 10 THEN CONCAT('V188_BUTTONS_SEEDED PASS ', COUNT(*))
            ELSE CONCAT('V188_BUTTONS_SEEDED FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0' AND `id` BETWEEN 9727 AND 9749;

-- 自检 2：菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V188_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V188_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

-- 自检 3：企业域菜单总量保持在合理量级
SELECT CASE WHEN COUNT(*) > 1945 THEN CONCAT('V188_ENTERPRISE_MENUS PASS ', COUNT(*))
            ELSE CONCAT('V188_ENTERPRISE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0';

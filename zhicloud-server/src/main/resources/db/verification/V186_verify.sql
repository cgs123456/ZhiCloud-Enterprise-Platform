-- V186 验证：ERP/WMS/MES 无前端域的菜单与按钮权限落库

-- 自检 1：新增菜单/按钮全部落库
SELECT CASE WHEN COUNT(*) = 305 THEN 'V186_MENUS_SEEDED PASS'
            ELSE CONCAT('V186_MENUS_SEEDED FAIL ', COUNT(*), '/305') END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0' AND `id` BETWEEN 9300 AND 9604;

-- 自检 2：菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V186_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V186_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

-- 自检 3：企业域菜单总量增长（防误删）
SELECT CASE WHEN COUNT(*) > 1600 THEN CONCAT('V186_ENTERPRISE_MENUS PASS ', COUNT(*))
            ELSE CONCAT('V186_ENTERPRISE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0';

-- V190 验证：FMS 与 ERP 财务融合（删 fms_currency、本位币并入 ERP）

-- 自检 1：fms_currency 表已删
SELECT CASE WHEN COUNT(*) = 0 THEN 'V190_FMS_CURRENCY_DROPPED PASS'
            ELSE CONCAT('V190_FMS_CURRENCY_DROPPED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'fms_currency';

-- 自检 2：本位币编码已回填（未初始化的账套允许为空，已初始化的必须有值）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V190_CURRENCY_CODE_BACKFILL PASS'
            ELSE CONCAT('V190_CURRENCY_CODE_BACKFILL FAIL ', COUNT(*)) END AS check_result
  FROM `fms_account_set`
 WHERE `deleted` = b'0' AND `initialized` = b'1'
   AND (`currency_code` IS NULL OR `currency_code` = '');

-- 自检 3：币别菜单已清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V190_CURRENCY_MENU_GONE PASS'
            ELSE CONCAT('V190_CURRENCY_MENU_GONE FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0' AND `permission` LIKE 'fms:config:currency:%';

-- 自检 4：FMS 配置域菜单仍在（防止误删财务配置层）
SELECT CASE WHEN COUNT(*) >= 28 THEN CONCAT('V190_FMS_CONFIG_MENUS_INTACT PASS ', COUNT(*))
            ELSE CONCAT('V190_FMS_CONFIG_MENUS_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0' AND `permission` LIKE 'fms:config:%';

-- 自检 5：ERP 币别表仍在（本位币的单一真相所在）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V190_ERP_CURRENCY_TABLE_INTACT PASS'
            ELSE CONCAT('V190_ERP_CURRENCY_TABLE_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_currency';

-- 自检 6：菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V190_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V190_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

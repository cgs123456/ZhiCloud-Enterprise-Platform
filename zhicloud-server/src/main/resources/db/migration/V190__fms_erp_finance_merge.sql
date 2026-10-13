-- ============================================================
-- V190：FMS 与 ERP 财务融合收尾——删 fms_currency（本位币并入 ERP 单一真相）
-- ============================================================
-- 背景（台账 §12.210，所有者 2026-10-13 批准「直接融合」）：
-- FMS 与 ERP 财务是两套平行实现。核对后确认真实重叠只有「币别」一处：
--   - FMS：fms_currency（账套内币别，含本位币标记）
--   - ERP：erp_currency（全局币别，含 is_base/有效期）
-- 本位币是财务唯一必须单一真相的字段，故删 fms_currency，本位币编码直接记在
-- fms_account_set.currency_code。其余 FMS 能力（账套/辅助核算/凭证字/摘要/凭证
-- 模板/财务指标/账套参数）在 ERP 无对应物，保留为「ERP 财务的配置层」，
-- 不搬代码（Modulith 模块边界不可跨越，搬集会触发 ModularityTests 跨模块访问告警）。
--
-- 已删除的 FMS 前端死页（后端无对应实现）：voucher / ledger / report / closing /
-- home / subject / initial-balance —— 这些本就是照 ERP 已有能力写的重复页面。
-- ============================================================

-- 1. fms_account_set 增加本位币编码列（先加列后灌数，保证不丢历史本位币信息）
ALTER TABLE `fms_account_set`
  ADD COLUMN `currency_code` varchar(16) DEFAULT NULL COMMENT '本位币编码（融合后对应 erp_currency.code；初始化时写入）' AFTER `currency_id`;

-- 2. 把历史本位币编码从 fms_currency 回填到 account_set
UPDATE `fms_account_set` s
  JOIN `fms_currency` c ON c.`id` = s.`currency_id`
   SET s.`currency_code` = c.`code`
 WHERE s.`deleted` = b'0' AND c.`deleted` = b'0' AND (s.`currency_code` IS NULL OR s.`currency_code` = '');

-- 3. 删除 FMS 币别菜单（9020 币别管理 + 3 个按钮及其角色关联
DELETE rm FROM `system_role_menu` rm
  JOIN `system_menu` m ON m.`id` = rm.`menu_id`
 WHERE m.`permission` LIKE 'fms:config:currency:%';
DELETE m FROM `system_menu` m
 WHERE `permission` LIKE 'fms:config:currency:%';

-- 4. 删除 fms_currency 表（本位币已并入 ERP erp_currency 单一真相）
DROP TABLE IF EXISTS `fms_currency`;

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

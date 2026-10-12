-- ============================================================
-- V189：修正 V188 按钮命名重复（「成本计算计算」「设备数据采集采集」）
-- ============================================================
-- 背景（台账 §12.203）：V188 生成按钮名时用「菜单页名 + 动作中文」拼接，
-- 当菜单页名本身以动作词结尾时出现叠字，如
--   成本计算 + 计算  -> 「成本计算计算」
--   设备数据采集 + 采集 -> 「设备数据采集采集」
-- 仅改展示名，不动 permission / parent（不影响鉴权）。
-- ============================================================
UPDATE `system_menu` SET `name` = '执行成本计算' WHERE `deleted` = b'0' AND `id` = 9730
  AND `permission` = 'erp:cost-calculation:calculate';
UPDATE `system_menu` SET `name` = '采集设备数据' WHERE `deleted` = b'0' AND `id` = 9739
  AND `permission` = 'mes:dv-device-data:collect';

-- 自检：两个名称已修正且不再叠字
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_menu` WHERE `deleted` = b'0' AND `id` IN (9730, 9739)
                   AND `name` IN ('执行成本计算', '采集设备数据')) = 2
            THEN 'V189_BUTTON_NAMES FIXED PASS'
            ELSE 'V189_BUTTON_NAMES FIXED FAIL' END AS check_result;

-- 自检：菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V189_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V189_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

-- V189 验证：按钮展示名修正

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

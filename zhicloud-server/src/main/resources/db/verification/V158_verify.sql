-- V158 验证：FMS config 域菜单种子落库（目录 1 + 菜单 8 + 按钮 27 = 36 行）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V158_FMS_DIR PASS'
            ELSE CONCAT('V158_FMS_DIR FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 9000 AND `type` = 1 AND `path` = '/fms';

SELECT CASE WHEN COUNT(*) = 8 THEN 'V158_FMS_MENUS PASS'
            ELSE CONCAT('V158_FMS_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9000;

SELECT CASE WHEN COUNT(*) = 27 THEN 'V158_FMS_BUTTONS PASS'
            ELSE CONCAT('V158_FMS_BUTTONS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` IN (9010,9020,9030,9040,9050,9060,9070);

-- 菜单组件路径与前端实际页面一致（逐条比对 component 非空）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V158_FMS_COMPONENTS PASS'
            ELSE CONCAT('V158_FMS_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9000 AND (`component` IS NULL OR `component` = '');

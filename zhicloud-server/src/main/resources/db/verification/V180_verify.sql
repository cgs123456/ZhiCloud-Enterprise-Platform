-- V180 验证：portal 申诉附件列 + 菜单权限种子
SELECT CASE WHEN COUNT(*) = 1 THEN 'V180_APPEAL_FILE_COL PASS'
            ELSE CONCAT('V180_APPEAL_FILE_COL FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_performance_assessment'
   AND `column_name` = 'appeal_file_urls' AND `data_type` = 'json';

SELECT CASE WHEN COUNT(*) = 3 THEN 'V180_PORTAL_MENUS PASS'
            ELSE CONCAT('V180_PORTAL_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3
   AND `permission` IN ('hrm:portal:employee:update','hrm:portal:attendance:leave','hrm:portal:performance:action');

-- V173 验证：HRM 工作台/操作日志权限种子落库
SELECT CASE WHEN COUNT(*) = 2 THEN 'V173_HOME_LOG_MENUS PASS'
            ELSE CONCAT('V173_HOME_LOG_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3
   AND `permission` IN ('hrm:home:query','hrm:operate-log:query');

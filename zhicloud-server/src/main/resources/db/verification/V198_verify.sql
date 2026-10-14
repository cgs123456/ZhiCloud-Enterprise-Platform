-- V198 验证：TMS 运费报表菜单

SELECT CASE WHEN COUNT(*) = 1 THEN 'V198_FREIGHT_REPORT_MENU PASS'
            ELSE CONCAT('V198_FREIGHT_REPORT_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `component` = 'tms/freight/report/index';

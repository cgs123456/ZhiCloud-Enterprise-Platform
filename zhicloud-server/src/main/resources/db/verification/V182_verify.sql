-- V182 验证：HRM 月度社保菜单种子落库
SELECT CASE WHEN COUNT(*) = 4 THEN 'V182_INS_MONTH_MENUS PASS'
            ELSE CONCAT('V182_INS_MONTH_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` BETWEEN 9254 AND 9257;

SELECT CASE WHEN COUNT(*) = 1 THEN 'V182_INS_MONTH_COMPONENT PASS'
            ELSE CONCAT('V182_INS_MONTH_COMPONENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 9254 AND `type` = 2
   AND `component` = 'hrm/insurance/month-record/index';

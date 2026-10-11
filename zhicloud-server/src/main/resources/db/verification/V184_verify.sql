-- V184 验证：HRM 职位管理菜单种子落库
SELECT CASE WHEN COUNT(*) = 4 THEN 'V184_POSITION_MENUS PASS'
            ELSE CONCAT('V184_POSITION_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` BETWEEN 9258 AND 9261;

SELECT CASE WHEN COUNT(*) = 1 THEN 'V184_POSITION_COMPONENT PASS'
            ELSE CONCAT('V184_POSITION_COMPONENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 9258 AND `type` = 2
   AND `component` = 'hrm/position/index';

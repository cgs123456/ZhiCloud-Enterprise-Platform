-- V179 验证：HRM 薪资业务域菜单种子落库
SELECT CASE WHEN COUNT(*) = 13 THEN 'V179_SALARY_BIZ_MENUS PASS'
            ELSE CONCAT('V179_SALARY_BIZ_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` BETWEEN 9237 AND 9249;

-- 三个菜单页 component 非空
SELECT CASE WHEN COUNT(*) = 3 THEN 'V179_SALARY_BIZ_COMPONENTS PASS'
            ELSE CONCAT('V179_SALARY_BIZ_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100
   AND `component` IN ('hrm/salary/employee-info/index','hrm/salary/month-record/index',
                       'hrm/salary/slip/send-record/index');

-- 核算权限为按钮级
SELECT CASE WHEN COUNT(*) = 1 THEN 'V179_SALARY_COMPUTE_BUTTON PASS'
            ELSE CONCAT('V179_SALARY_COMPUTE_BUTTON FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 3 AND `permission` = 'hrm:salary:month-record:compute';

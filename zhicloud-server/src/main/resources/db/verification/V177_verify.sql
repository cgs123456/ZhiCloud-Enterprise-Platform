-- V177 验证：HRM 薪资配置菜单 + 字典种子落库
SELECT CASE WHEN COUNT(*) = 18 THEN 'V177_SALARY_MENUS PASS'
            ELSE CONCAT('V177_SALARY_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` BETWEEN 9219 AND 9236;

SELECT CASE WHEN COUNT(*) = 4 THEN 'V177_SALARY_DICT_TYPES PASS'
            ELSE CONCAT('V177_SALARY_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0'
   AND `type` IN ('hrm_salary_tax_type','hrm_salary_option_type','hrm_salary_yes_no','hrm_salary_change_reason');

-- 字典数据：计税类型3 + 薪资项类型3 + 是否2 + 调整原因6 = 14
SELECT CASE WHEN COUNT(*) = 14 THEN 'V177_SALARY_DICT_DATA PASS'
            ELSE CONCAT('V177_SALARY_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0'
   AND `dict_type` IN ('hrm_salary_tax_type','hrm_salary_option_type','hrm_salary_yes_no','hrm_salary_change_reason');

-- 五个菜单页 component 非空
SELECT CASE WHEN COUNT(*) = 5 THEN 'V177_SALARY_COMPONENTS PASS'
            ELSE CONCAT('V177_SALARY_COMPONENTS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2 AND `parent_id` = 9100
   AND `component` LIKE 'hrm/salary/config%';

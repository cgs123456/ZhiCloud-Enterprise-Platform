-- V183 验证：HRM 补 4 个缺失字典落库
SELECT CASE WHEN COUNT(*) = 4 THEN 'V183_MISSING_DICT_TYPES PASS'
            ELSE CONCAT('V183_MISSING_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0'
   AND `type` IN ('hrm_insurance_emp_status','hrm_salary_change_type',
                  'hrm_salary_change_record_status','hrm_salary_slip_read_status');

-- 字典数据 2+2+3+2 = 9
SELECT CASE WHEN COUNT(*) = 9 THEN 'V183_MISSING_DICT_DATA PASS'
            ELSE CONCAT('V183_MISSING_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0'
   AND `dict_type` IN ('hrm_insurance_emp_status','hrm_salary_change_type',
                       'hrm_salary_change_record_status','hrm_salary_slip_read_status');

-- 每个字典至少 2 条数据（无空字典）
SELECT CASE WHEN COUNT(*) = 4 THEN 'V183_DICT_NON_EMPTY PASS'
            ELSE CONCAT('V183_DICT_NON_EMPTY FAIL ', COUNT(*)) END AS check_result
  FROM (
    SELECT `dict_type` FROM `system_dict_data`
     WHERE `deleted` = b'0'
       AND `dict_type` IN ('hrm_insurance_emp_status','hrm_salary_change_type',
                           'hrm_salary_change_record_status','hrm_salary_slip_read_status')
     GROUP BY `dict_type` HAVING COUNT(*) >= 2
  ) d;

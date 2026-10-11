-- V178 验证：HRM 薪资业务域 7 张表落库
SELECT CASE WHEN COUNT(*) = 7 THEN 'V178_SALARY_BIZ_TABLES PASS'
            ELSE CONCAT('V178_SALARY_BIZ_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_salary_employee_info','hr_salary_employee_change_record','hr_salary_month_record',
                        'hr_salary_month_employee_record','hr_salary_slip_template','hr_salary_slip_send_record',
                        'hr_salary_slip');

-- 员工月度工资 (month_record_id, employee_id) 复合索引
SELECT CASE WHEN COUNT(*) = 2 THEN 'V178_MONTH_EMP_IDX PASS'
            ELSE CONCAT('V178_MONTH_EMP_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_salary_month_employee_record'
   AND `index_name` = 'idx_hr_sal_month_emp_record';

-- 工资项值 json 列（员工薪资/月度员工/工资条模板/发放记录）
SELECT CASE WHEN COUNT(*) = 4 THEN 'V178_OPTION_VALUE_JSON PASS'
            ELSE CONCAT('V178_OPTION_VALUE_JSON FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE()
   AND ((`table_name` = 'hr_salary_employee_info' AND `column_name` IN ('salary_options','probation_salary_options'))
     OR (`table_name` = 'hr_salary_month_employee_record' AND `column_name` = 'option_values')
     OR (`table_name` = 'hr_salary_slip_template' AND `column_name` = 'options'))
   AND `data_type` = 'json';

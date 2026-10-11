-- V181 验证：HRM 保险域月度社保 2 张表落库
SELECT CASE WHEN COUNT(*) = 2 THEN 'V181_INS_MONTH_TABLES PASS'
            ELSE CONCAT('V181_INS_MONTH_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_insurance_month_record','hr_insurance_month_employee_record');

-- 员工月度社保 (month_record_id, employee_id) 复合索引
SELECT CASE WHEN COUNT(*) = 2 THEN 'V181_INS_MONTH_EMP_IDX PASS'
            ELSE CONCAT('V181_INS_MONTH_EMP_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_insurance_month_employee_record'
   AND `index_name` = 'idx_hr_ins_month_emp_record';

-- 两组项目 json 列
SELECT CASE WHEN COUNT(*) = 2 THEN 'V181_INS_MONTH_JSON PASS'
            ELSE CONCAT('V181_INS_MONTH_JSON FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_insurance_month_employee_record'
   AND `column_name` IN ('social_security_projects','provident_fund_projects') AND `data_type` = 'json';

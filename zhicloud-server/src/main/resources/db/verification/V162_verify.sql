-- V162 验证：员工子域 10 张表全部存在
SELECT CASE WHEN COUNT(*) = 10 THEN 'V162_SUB_TABLES PASS'
            ELSE CONCAT('V162_SUB_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name IN ('hr_employee_change_record','hr_employee_quit_info','hr_employee_file',
                      'hr_employee_certificate','hr_employee_contact','hr_employee_education_experience',
                      'hr_employee_work_experience','hr_employee_training_experience',
                      'hr_employee_field_config','hr_employee_contract');

-- 合同表附件列是 json
SELECT CASE WHEN COUNT(*) = 1 THEN 'V162_CONTRACT_JSON PASS'
            ELSE CONCAT('V162_CONTRACT_JSON FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'hr_employee_contract'
   AND column_name = 'file_urls' AND data_type = 'json';

-- 字段配置表唯一键（kind, entry_status, name）存在
SELECT CASE WHEN COUNT(*) >= 3 THEN 'V162_FIELD_CONFIG_UK PASS'
            ELSE CONCAT('V162_FIELD_CONFIG_UK FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'hr_employee_field_config'
   AND index_name = 'uk_hr_emp_field_config';

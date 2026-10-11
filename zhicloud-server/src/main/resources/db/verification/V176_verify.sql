-- V176 验证：HRM 薪资配置域 5 张表落库
SELECT CASE WHEN COUNT(*) = 5 THEN 'V176_SALARY_CONFIG_TABLES PASS'
            ELSE CONCAT('V176_SALARY_CONFIG_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_salary_group','hr_salary_option','hr_salary_tax_rule',
                        'hr_salary_change_template','hr_salary_config');

-- 薪资项 code/parent_code 索引
SELECT CASE WHEN COUNT(*) = 2 THEN 'V176_SALARY_OPTION_IDX PASS'
            ELSE CONCAT('V176_SALARY_OPTION_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_salary_option'
   AND `index_name` IN ('idx_hr_salary_option_code','idx_hr_salary_option_parent')
   AND `seq_in_index` = 1;

-- 薪资组适用部门/员工 json 列
SELECT CASE WHEN COUNT(*) = 2 THEN 'V176_SALARY_GROUP_JSON PASS'
            ELSE CONCAT('V176_SALARY_GROUP_JSON FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_salary_group'
   AND `column_name` IN ('dept_ids','employee_ids') AND `data_type` = 'json';

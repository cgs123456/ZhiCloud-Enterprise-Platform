-- V168 验证：HRM 保险域 5 张表落库
SELECT CASE WHEN COUNT(*) = 5 THEN 'V168_INS_TABLES PASS'
            ELSE CONCAT('V168_INS_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_insurance_standard_type','hr_insurance_standard_project',
                        'hr_insurance_scheme','hr_insurance_scheme_project','hr_insurance_employee_info');

-- 标准类型 (area_id,code) 查询索引存在（首列 seq=1）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V168_STD_TYPE_IDX PASS'
            ELSE CONCAT('V168_STD_TYPE_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_insurance_standard_type'
   AND `index_name` = 'idx_hr_ins_std_type_area' AND `seq_in_index` = 1;

-- 标准项目 type 列存在（前端 InsuranceStandardProjectVO 消费）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V168_STD_PROJECT_TYPE_COL PASS'
            ELSE CONCAT('V168_STD_PROJECT_TYPE_COL FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_insurance_standard_project'
   AND `column_name` = 'type';

-- 方案项目表 scheme_id 索引存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V168_SCHEME_PROJECT_IDX PASS'
            ELSE CONCAT('V168_SCHEME_PROJECT_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_insurance_scheme_project'
   AND `index_name` = 'idx_hr_ins_scheme_project_scheme' AND `seq_in_index` = 1;

-- 员工参保信息表含 scheme_id 列（参保方案关联）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V168_EMP_INFO_TABLE PASS'
            ELSE CONCAT('V168_EMP_INFO_TABLE FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_insurance_employee_info'
   AND `column_name` = 'scheme_id';

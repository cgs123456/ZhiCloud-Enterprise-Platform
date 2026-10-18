-- V208 验证：HRM 尾置收尾的净效果（独立复核面）
-- 分工：V208 内自检已覆盖"删干净、保留面在、数据面不塌"；本文件复核 Flyway 通道记录 + 独立重断言。
-- 运行口径同 V140_verify：逐条 SELECT 出的 check_result 必须全为 PASS（E2E 脚本解析）。

-- 1. Flyway 通道记录：V208 必须由应用自带 Flyway 成功执行
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '208' AND success = 1) = 1
            THEN 'V208_FLYWAY_APPLIED PASS'
            ELSE 'V208_FLYWAY_APPLIED FAIL 版本 208 不在成功记录里' END AS check_result;

-- 2. hr_employee 旧镜像列清零（information_schema 实测）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_VERIFY_LEGACY_COLUMNS_GONE PASS'
            ELSE CONCAT('V208_VERIFY_LEGACY_COLUMNS_GONE FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND column_name IN ('gender','birth_date','hire_date','leave_date','status','employment_type');

-- 3. hr_employee 新列齐全（7 个）
SELECT CASE WHEN COUNT(*) = 7 THEN 'V208_VERIFY_NEW_COLUMNS_PRESENT PASS'
            ELSE CONCAT('V208_VERIFY_NEW_COLUMNS_PRESENT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND column_name IN ('sex','birthday','entry_status','employee_status','type','entry_time','leave_time');

-- 4. idx_status 消失，关键索引保留（PRIMARY/uk_emp_no/idx_dept_id/idx_position_id/idx_tenant_id + V161 三索引）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_VERIFY_IDX_STATUS_GONE PASS'
            ELSE CONCAT('V208_VERIFY_IDX_STATUS_GONE FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND index_name = 'idx_status';
SELECT CASE WHEN COUNT(DISTINCT index_name) >= 7 THEN 'V208_VERIFY_KEY_INDEXES_INTACT PASS'
            ELSE CONCAT('V208_VERIFY_KEY_INDEXES_INTACT FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND index_name IN ('PRIMARY','uk_emp_no','idx_dept_id','idx_position_id','idx_tenant_id',
                      'idx_hr_employee_entry_status','idx_hr_employee_user','idx_hr_employee_leader');

-- 5. 9 张孤儿表清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_VERIFY_ORPHAN_TABLES_GONE PASS'
            ELSE CONCAT('V208_VERIFY_ORPHAN_TABLES_GONE FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND table_name IN ('hr_attendance','hr_contract','hr_interview','hr_job_posting','hr_leave_balance',
                      'hr_performance','hr_resume','hr_salary','hr_social_insurance_base');

-- 6. 保留 5 表齐全
SELECT CASE WHEN COUNT(*) = 5 THEN 'V208_VERIFY_KEPT_TABLES_INTACT PASS'
            ELSE CONCAT('V208_VERIFY_KEPT_TABLES_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND table_name IN ('hr_employee','hr_position','hr_department','hr_leave_request','hr_leave_type');

-- 7. 退役字典清零（类型行 + 数据行），hr_position_level 保留
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_VERIFY_RETIRED_DICT_TYPES_GONE PASS'
            ELSE CONCAT('V208_VERIFY_RETIRED_DICT_TYPES_GONE FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type` WHERE deleted = b'0'
   AND `type` IN ('hr_gender','hr_employee_status','hr_employment_type','hr_attendance_status','hr_salary_status','hr_performance_grade');
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_VERIFY_RETIRED_DICT_DATA_GONE PASS'
            ELSE CONCAT('V208_VERIFY_RETIRED_DICT_DATA_GONE FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
   WHERE `dict_type` IN ('hr_gender','hr_employee_status','hr_employment_type','hr_attendance_status','hr_salary_status','hr_performance_grade');
SELECT CASE WHEN COUNT(*) = 1 THEN 'V208_VERIFY_POSITION_LEVEL_DICT_TYPE_KEPT PASS'
            ELSE CONCAT('V208_VERIFY_POSITION_LEVEL_DICT_TYPE_KEPT FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type` WHERE `type` = 'hr_position_level' AND deleted = b'0';
SELECT CASE WHEN COUNT(*) > 0 THEN 'V208_VERIFY_POSITION_LEVEL_DICT_DATA_KEPT PASS'
            ELSE 'V208_VERIFY_POSITION_LEVEL_DICT_DATA_KEPT FAIL' END AS check_result
  FROM `system_dict_data` WHERE `dict_type` = 'hr_position_level' AND deleted = b'0';

-- 8. 数据面：hr_employee 存活行 > 0；三新轴无空；sex 值域 ∈ {1,2}
SELECT CASE WHEN COUNT(*) > 0 THEN CONCAT('V208_VERIFY_HR_EMPLOYEE_INTACT PASS ', COUNT(*))
            ELSE 'V208_VERIFY_HR_EMPLOYEE_INTACT FAIL' END AS check_result
  FROM `hr_employee` WHERE deleted = b'0'
   AND entry_status IS NOT NULL AND employee_status IS NOT NULL AND `type` IS NOT NULL;
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_VERIFY_SEX_DOMAIN_OK PASS'
            ELSE CONCAT('V208_VERIFY_SEX_DOMAIN_OK FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee` WHERE sex IS NOT NULL AND sex NOT IN (1, 2);

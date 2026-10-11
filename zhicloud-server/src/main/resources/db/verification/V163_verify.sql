-- V163 验证：员工域字典 + 通知模板 + 字段配置种子落库
SELECT CASE WHEN COUNT(*) = 4 THEN 'V163_DICT_TYPES PASS'
            ELSE CONCAT('V163_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0'
   AND `type` IN ('hrm_employee_entry_status','hrm_employee_status','hrm_employee_type','hrm_employee_education');

SELECT CASE WHEN COUNT(*) = 26 THEN 'V163_DICT_DATA PASS'
            ELSE CONCAT('V163_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` LIKE 'hrm_employee%';

SELECT CASE WHEN COUNT(*) = 1 THEN 'V163_NOTIFY_TEMPLATE PASS'
            ELSE CONCAT('V163_NOTIFY_TEMPLATE FAIL ', COUNT(*)) END AS check_result
  FROM `system_notify_template`
 WHERE `deleted` = b'0' AND `code` = 'hrm_employee_profile_fill';

-- 字段配置三套（档案 34 + 在职新建 34 + 待入职新建 34 = 102）
SELECT CASE WHEN COUNT(*) = 34 THEN 'V163_ARCHIVE_FIELDS PASS'
            ELSE CONCAT('V163_ARCHIVE_FIELDS FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee_field_config` WHERE `deleted` = b'0' AND `kind` = 1;
SELECT CASE WHEN COUNT(*) = 34 THEN 'V163_CREATE_ACTIVE_FIELDS PASS'
            ELSE CONCAT('V163_CREATE_ACTIVE_FIELDS FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee_field_config` WHERE `deleted` = b'0' AND `kind` = 2 AND `entry_status` = 1;
SELECT CASE WHEN COUNT(*) = 34 THEN 'V163_CREATE_PENDING_FIELDS PASS'
            ELSE CONCAT('V163_CREATE_PENDING_FIELDS FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee_field_config` WHERE `deleted` = b'0' AND `kind` = 2 AND `entry_status` = 2;

-- 字段名对齐前端 EmployeeForm 的 isFieldVisible 名单（抽查核心若干）
SELECT CASE WHEN COUNT(*) >= 8 THEN 'V163_FIELD_NAMES PASS'
            ELSE CONCAT('V163_FIELD_NAMES FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee_field_config`
 WHERE `deleted` = b'0' AND `kind` = 2 AND `entry_status` = 1
   AND `name` IN ('name','jobNumber','userId','mobile','email','sex','entryStatus','entryTime');

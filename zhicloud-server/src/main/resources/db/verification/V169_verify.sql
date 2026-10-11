-- V169 验证：HRM 保险标准种子 + 字典落库
SELECT CASE WHEN COUNT(*) = 3 THEN 'V169_STD_TYPE_COUNT PASS'
            ELSE CONCAT('V169_STD_TYPE_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM `hr_insurance_standard_type`
 WHERE `area_id` = 440300 AND `tenant_id` = 1 AND `deleted` = b'0';

-- 城镇职工项目数（6 项）
SELECT CASE WHEN COUNT(*) = 6 THEN 'V169_STD_PROJECT_URBAN PASS'
            ELSE CONCAT('V169_STD_PROJECT_URBAN FAIL ', COUNT(*)) END AS check_result
  FROM `hr_insurance_standard_project`
 WHERE `type_code` = 'URBAN' AND `tenant_id` = 1 AND `deleted` = b'0';

-- 方案/项目类型字典
SELECT CASE WHEN COUNT(*) = 2 THEN 'V169_INS_DICT_TYPES PASS'
            ELSE CONCAT('V169_INS_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0'
   AND `type` IN ('hrm_insurance_scheme_type','hrm_insurance_project_type');

SELECT CASE WHEN COUNT(*) = 13 THEN 'V169_INS_DICT_DATA PASS'
            ELSE CONCAT('V169_INS_DICT_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` LIKE 'hrm_insurance%';

-- V159 验证：FMS config 字典种子落库
SELECT CASE WHEN COUNT(*) = 2 THEN 'V159_FMS_DICT_TYPES PASS'
            ELSE CONCAT('V159_FMS_DICT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type`
 WHERE `deleted` = b'0' AND `type` IN ('fms_account_user_level', 'fms_finance_indicator_type');

SELECT CASE WHEN COUNT(*) = 3 THEN 'V159_FMS_LEVEL_DATA PASS'
            ELSE CONCAT('V159_FMS_LEVEL_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` = 'fms_account_user_level';

SELECT CASE WHEN COUNT(*) = 2 THEN 'V159_FMS_INDICATOR_DATA PASS'
            ELSE CONCAT('V159_FMS_INDICATOR_DATA FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_data`
 WHERE `deleted` = b'0' AND `dict_type` = 'fms_finance_indicator_type';

-- V165 验证：field_config 去重 + 个人备忘表落库
-- 去重后 kind=1 段回归 34（companyAge 仅 1 行）
SELECT CASE WHEN COUNT(*) = 34 THEN 'V165_ARCHIVE_FIELDS_DEDUP PASS'
            ELSE CONCAT('V165_ARCHIVE_FIELDS_DEDUP FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee_field_config` WHERE `deleted` = b'0' AND `kind` = 1;

SELECT CASE WHEN COUNT(*) = 1 THEN 'V165_COMPANY_AGE_SINGLE PASS'
            ELSE CONCAT('V165_COMPANY_AGE_SINGLE FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee_field_config` WHERE `deleted` = b'0' AND `kind` = 1 AND `name` = 'companyAge';

-- 个人备忘表存在（information_schema 计 1）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V165_PERSONAL_NOTE_TABLE PASS'
            ELSE CONCAT('V165_PERSONAL_NOTE_TABLE FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_employee_personal_note';

-- V157 验证：FMS config 域 11 张 fms_* 表全部存在
SELECT CASE WHEN COUNT(*) = 11 THEN 'V157_FMS_TABLES PASS'
            ELSE CONCAT('V157_FMS_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name LIKE 'fms\_%';

-- 全部 11 张表都是租户表（TenantBaseDO → 必须有 tenant_id 列）
SELECT CASE WHEN COUNT(*) = 11 THEN 'V157_FMS_TENANT_COLS PASS'
            ELSE CONCAT('V157_FMS_TENANT_COLS FAIL ', COUNT(*)) END AS check_result
  FROM (SELECT table_name FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name LIKE 'fms\_%' AND column_name = 'tenant_id'
        GROUP BY table_name) t;

-- 凭证模板表含 json 列（entries 分录数组）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V157_VOUCHER_TEMPLATE_JSON PASS'
            ELSE CONCAT('V157_VOUCHER_TEMPLATE_JSON FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'fms_voucher_template'
   AND column_name = 'entries' AND data_type = 'json';

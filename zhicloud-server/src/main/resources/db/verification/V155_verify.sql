-- V155 验证：微信公众号 mp_* 8 张表全部存在
SELECT CASE WHEN COUNT(*) = 8 THEN 'V155_MP_TABLES PASS'
            ELSE CONCAT('V155_MP_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name LIKE 'mp\_%';

-- mp_account 是唯一租户表（TenantBaseDO），必须有 tenant_id 列
SELECT CASE WHEN COUNT(*) = 1 THEN 'V155_MP_ACCOUNT_TENANT_COL PASS'
            ELSE CONCAT('V155_MP_ACCOUNT_TENANT_COL FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'mp_account' AND column_name = 'tenant_id';

-- 非租户 DO（BaseDO）不得有 tenant_id 列
SELECT CASE WHEN COUNT(*) = 0 THEN 'V155_MP_BASEDO_NO_TENANT PASS'
            ELSE CONCAT('V155_MP_BASEDO_NO_TENANT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name LIKE 'mp\_%' AND table_name <> 'mp_account'
   AND column_name = 'tenant_id';

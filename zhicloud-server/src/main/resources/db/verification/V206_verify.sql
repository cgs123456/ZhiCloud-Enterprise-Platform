-- V206 验证：crm_sale_order 接入 BPM 审批（§12.261 C 组第 3 项）

-- 1. audit_status 列存在且为 INT 型
SELECT CASE WHEN COUNT(*) = 1 THEN 'V206_CRM_AUDIT_STATUS_COL_PASS'
            ELSE CONCAT('V206_CRM_AUDIT_STATUS_COL_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'crm_sale_order'
   AND COLUMN_NAME = 'audit_status'
   AND DATA_TYPE = 'int';

-- 2. 存量回填完整：全表无 audit_status IS NULL 行（迁移第 2 步语义）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V206_CRM_AUDIT_STATUS_BACKFILL_PASS'
            ELSE CONCAT('V206_CRM_AUDIT_STATUS_BACKFILL_FAIL ', COUNT(*)) END AS check_result
  FROM crm_sale_order
 WHERE audit_status IS NULL;

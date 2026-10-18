-- V213 验证：CRM 合同电子签任务表（D5，ADR 271；台账 §12.271）

-- 1. 任务表存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V213_ESIGN_TASK_TABLE_PASS'
            ELSE CONCAT('V213_ESIGN_TASK_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract_esign_task';

-- 2. 关键列齐 + 非空（contract_id / task_no / status）
SELECT CASE WHEN COUNT(*) = 3 THEN 'V213_ESIGN_TASK_COLUMNS_PASS'
            ELSE CONCAT('V213_ESIGN_TASK_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract_esign_task'
   AND COLUMN_NAME IN ('contract_id', 'task_no', 'status') AND IS_NULLABLE = 'NO';

-- 3. 任务号唯一键在（tenant_id, task_no）
SELECT CASE WHEN COUNT(DISTINCT INDEX_NAME) = 1 THEN 'V213_ESIGN_TASK_UNIQUE_KEY_PASS'
            ELSE CONCAT('V213_ESIGN_TASK_UNIQUE_KEY_FAIL ', COUNT(DISTINCT INDEX_NAME)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract_esign_task'
   AND INDEX_NAME = 'uk_crm_contract_esign_task_task_no';

-- 4. 合同维度索引在（tenant_id, contract_id）
SELECT CASE WHEN COUNT(DISTINCT INDEX_NAME) = 1 THEN 'V213_ESIGN_TASK_CONTRACT_INDEX_PASS'
            ELSE CONCAT('V213_ESIGN_TASK_CONTRACT_INDEX_FAIL ', COUNT(DISTINCT INDEX_NAME)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract_esign_task'
   AND INDEX_NAME = 'idx_crm_contract_esign_task_contract';

-- 5. status 默认 10（未配置默认待签署）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V213_ESIGN_TASK_STATUS_DEFAULT_PASS'
            ELSE CONCAT('V213_ESIGN_TASK_STATUS_DEFAULT_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract_esign_task'
   AND COLUMN_NAME = 'status' AND COLUMN_DEFAULT = '10';

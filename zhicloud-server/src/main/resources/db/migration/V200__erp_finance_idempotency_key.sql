-- ============================================================
-- V200：erp_finance_payment / erp_finance_receipt 补幂等键（真实缺陷修复）
-- ============================================================
-- 背景（台账 §12.224.2 #3、§12.229 复核仍在）：付款单与收款单的创建路径
-- 只有 noRedisDAO.generate() 生成单号 + 应用层查重，没有任何幂等键。
-- 后果：客户端重试（网络超时、双击、消息重投）会生成**不同单号**的两张业务单据，
-- 且都带业务副作用（updatePurchasePrice / updateSalePrice、过账事件）。
-- 现有唯一键 uk_erp_finance_payment_no_tenant (no, tenant_id) 挡不住它——
-- 单号是服务端生成的，同一次业务意图的两次提交单号本来就不同。
--
-- 修法：照抄 CRM 回款的成熟范式（crm_receivable 的 idempotency_key + payload_hash
-- + uk_crm_receivable_idempotency (tenant_id, idempotency_key)，见 CrmReceivableDO）：
--   1. idempotency_key  客户端幂等键，同租户内唯一
--   2. payload_hash     创建请求载荷 SHA-256；同键不同载荷必须拒绝，
--                       不能误返回其他业务单据
-- 唯一键含 tenant_id：多租户下同一客户端在不同租户可各有一次提交。
--
-- 存量安全：本次为 ADD COLUMN，存量行 idempotency_key 为 NULL。
-- MySQL 唯一索引允许多个 NULL，故存量行不参与冲突判定，不会阻塞 ALTER；
-- 已提交单据永远不会被重复创建命中（幂等键只在创建路径写入），语义无损。
-- ============================================================

ALTER TABLE `erp_finance_payment`
  ADD COLUMN `idempotency_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端幂等键，同租户唯一，防重复创建' AFTER `discount_price`,
  ADD COLUMN `payload_hash` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建请求载荷 SHA-256；同键不同载荷必须拒绝' AFTER `idempotency_key`,
  ADD UNIQUE KEY `uk_erp_finance_payment_idempotency` (`tenant_id`, `idempotency_key`);

ALTER TABLE `erp_finance_receipt`
  ADD COLUMN `idempotency_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户端幂等键，同租户唯一，防重复创建' AFTER `discount_price`,
  ADD COLUMN `payload_hash` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建请求载荷 SHA-256；同键不同载荷必须拒绝' AFTER `idempotency_key`,
  ADD UNIQUE KEY `uk_erp_finance_receipt_idempotency` (`tenant_id`, `idempotency_key`);

-- 自检 1：付款单幂等键列与唯一键均已建立
-- 口径注意：information_schema.statistics 每索引列一行，判存在性必须 COUNT(DISTINCT index_name)（V192/V199 固化）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V200_PAYMENT_IDEMPOTENCY_PASS'
            ELSE CONCAT('V200_PAYMENT_IDEMPOTENCY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment'
   AND index_name = 'uk_erp_finance_payment_idempotency';

-- 自检 2：付款单唯一键覆盖 (tenant_id, idempotency_key) 两列
SELECT CASE WHEN COUNT(*) = 2 THEN 'V200_PAYMENT_IDEMPOTENCY_COLUMNS_PASS'
            ELSE CONCAT('V200_PAYMENT_IDEMPOTENCY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment'
   AND index_name = 'uk_erp_finance_payment_idempotency'
   AND column_name IN ('tenant_id', 'idempotency_key');

-- 自检 3：收款单幂等键列与唯一键均已建立
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V200_RECEIPT_IDEMPOTENCY_PASS'
            ELSE CONCAT('V200_RECEIPT_IDEMPOTENCY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
   AND index_name = 'uk_erp_finance_receipt_idempotency';

-- 自检 4：收款单唯一键覆盖 (tenant_id, idempotency_key) 两列
SELECT CASE WHEN COUNT(*) = 2 THEN 'V200_RECEIPT_IDEMPOTENCY_COLUMNS_PASS'
            ELSE CONCAT('V200_RECEIPT_IDEMPOTENCY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
   AND index_name = 'uk_erp_finance_receipt_idempotency'
   AND column_name IN ('tenant_id', 'idempotency_key');

-- 自检 5：payload_hash 为 char(64)（SHA-256 十六进制定长）
SELECT CASE WHEN COUNT(*) = 2 THEN 'V200_PAYLOAD_HASH_LENGTH_PASS'
            ELSE CONCAT('V200_PAYLOAD_HASH_LENGTH_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name IN ('erp_finance_payment', 'erp_finance_receipt')
   AND column_name = 'payload_hash' AND character_maximum_length = 64;

-- 自检 6：存量行幂等键为空（NULL 不参与唯一索引冲突判定，ALTER 不被阻塞）
SELECT CONCAT('V200_EXISTING_IDEMPOTENCY_NULL=', COUNT(*)) AS check_result
  FROM (SELECT idempotency_key FROM `erp_finance_payment` WHERE idempotency_key IS NOT NULL
        UNION ALL
        SELECT idempotency_key FROM `erp_finance_receipt` WHERE idempotency_key IS NOT NULL) t;

-- 自检 7：原有单号唯一键未被破坏（查询仍走它）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V200_PAYMENT_NO_KEY_KEPT_PASS'
            ELSE CONCAT('V200_PAYMENT_NO_KEY_KEPT_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment'
   AND index_name = 'uk_erp_finance_payment_no_tenant';

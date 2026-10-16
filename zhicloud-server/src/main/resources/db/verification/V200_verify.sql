-- V200 验证：erp_finance_payment / erp_finance_receipt 幂等键唯一键

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V200_PAYMENT_IDEMPOTENCY_PASS'
            ELSE CONCAT('V200_PAYMENT_IDEMPOTENCY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment'
   AND index_name = 'uk_erp_finance_payment_idempotency';

SELECT CASE WHEN COUNT(*) = 2 THEN 'V200_PAYMENT_IDEMPOTENCY_COLUMNS_PASS'
            ELSE CONCAT('V200_PAYMENT_IDEMPOTENCY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment'
   AND index_name = 'uk_erp_finance_payment_idempotency'
   AND column_name IN ('tenant_id', 'idempotency_key');

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V200_RECEIPT_IDEMPOTENCY_PASS'
            ELSE CONCAT('V200_RECEIPT_IDEMPOTENCY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
   AND index_name = 'uk_erp_finance_receipt_idempotency';

SELECT CASE WHEN COUNT(*) = 2 THEN 'V200_RECEIPT_IDEMPOTENCY_COLUMNS_PASS'
            ELSE CONCAT('V200_RECEIPT_IDEMPOTENCY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
   AND index_name = 'uk_erp_finance_receipt_idempotency'
   AND column_name IN ('tenant_id', 'idempotency_key');

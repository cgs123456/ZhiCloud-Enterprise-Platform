-- V195 验证：银行账户挂账簿（账套视角现金流量表方案 B）

SELECT CASE WHEN COUNT(*) = 1 THEN 'V195_BANK_ACCOUNT_BOOK_COLUMN PASS'
            ELSE CONCAT('V195_BANK_ACCOUNT_BOOK_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_bank_account' AND column_name = 'account_book_id';

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V195_BANK_ACCOUNT_BOOK_INDEX PASS'
            ELSE CONCAT('V195_BANK_ACCOUNT_BOOK_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_bank_account'
   AND index_name = 'idx_erp_bank_account_book';

SELECT CASE WHEN COUNT(*) = 0 THEN 'V195_NO_BACKFILL PASS'
            ELSE CONCAT('V195_NO_BACKFILL FAIL ', COUNT(*)) END AS check_result
  FROM `erp_bank_account` WHERE `account_book_id` IS NOT NULL;

SELECT CASE WHEN COUNT(*) = 0 THEN 'V195_CASH_FLOW_UNTOUCHED PASS'
            ELSE CONCAT('V195_CASH_FLOW_UNTOUCHED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_cash_flow' AND column_name = 'account_book_id';

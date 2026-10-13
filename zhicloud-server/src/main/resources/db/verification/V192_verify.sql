-- V192 验证：修正 V191 索引自检口径 + 复核链路

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V192_ACCOUNT_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V192_ACCOUNT_BOOK_SET_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

SELECT CASE WHEN COUNT(*) = 1 THEN 'V192_ACCOUNT_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V192_ACCOUNT_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

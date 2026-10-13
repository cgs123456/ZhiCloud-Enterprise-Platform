-- V193 验证：账套→账簿→凭证层级一致性（凭证侧无冗余列）

SELECT CASE WHEN COUNT(*) = 1 THEN 'V193_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V193_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V193_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V193_BOOK_SET_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

SELECT CASE WHEN COUNT(*) = 0 THEN 'V193_VOUCHER_NO_REDUNDANT_COLUMN PASS'
            ELSE CONCAT('V193_VOUCHER_NO_REDUNDANT_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_voucher' AND column_name = 'account_set_id';

SELECT CASE WHEN COUNT(*) = 1 THEN 'V193_FMS_ACCOUNT_SET_INTACT PASS'
            ELSE CONCAT('V193_FMS_ACCOUNT_SET_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'fms_account_set';

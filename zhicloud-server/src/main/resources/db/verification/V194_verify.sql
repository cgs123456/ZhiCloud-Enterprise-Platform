-- V194 验证：FMS 会计科目菜单播种 + 融合链路一致性

-- 1. 账簿挂账套列存在（V191 层级）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V194_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V194_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

-- 2. 账套→账簿复合索引存在（COUNT(DISTINCT index_name) 口径）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V194_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V194_BOOK_SET_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

-- 3. 凭证表无 account_set_id 冗余列（设计意图：单一来源是账簿）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V194_VOUCHER_NO_REDUNDANT_COLUMN PASS'
            ELSE CONCAT('V194_VOUCHER_NO_REDUNDANT_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_voucher' AND column_name = 'account_set_id';

-- 4. 科目表 erp_gl_account 存在（单一真相）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V194_GL_ACCOUNT_INTACT PASS'
            ELSE CONCAT('V194_GL_ACCOUNT_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_gl_account';

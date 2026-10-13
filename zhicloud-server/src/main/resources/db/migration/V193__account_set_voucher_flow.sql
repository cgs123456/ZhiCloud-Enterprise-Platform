-- ============================================================
-- V193：账套 → 账簿 → 凭证 层级打通后的一致性复核（凭证侧不落冗余列）
-- ============================================================
-- 背景（台账 §12.213）：V191 给 erp_account_book 挂了 account_set_id；
-- 凭证侧（erp_gl_voucher）**刻意不落 account_set_id 冗余列**——
--   账簿改挂账套时，凭证上的冗余列不会同步更新，会形成「两套账套真相」；
--   故按账套查凭证统一走「账套 → 账簿集合 → 凭证 IN 查询」，单一来源是账簿。
--
-- 本迁移只做一致性复核与信息性输出，不改表结构（V191 已建列与索引）：
--   1. 账簿挂账套列存在
--   2. 账套→账簿索引存在（复合索引按 DISTINCT index_name 判断，V192 已固化口径）
--   3. 凭证表存在且**没有** account_set_id 冗余列（即设计意图未被破坏）
--   4. 已挂账套的账簿数（信息性）
--   5. 账套表存在（FMS 侧）
-- ============================================================

-- 1. 账簿挂账套列存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V193_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V193_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

-- 2. 账套→账簿复合索引存在
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V193_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V193_BOOK_SET_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

-- 3. 凭证表不得有 account_set_id 冗余列（设计意图：单一来源是账簿）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V193_VOUCHER_NO_REDUNDANT_COLUMN PASS'
            ELSE CONCAT('V193_VOUCHER_NO_REDUNDANT_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_gl_voucher' AND column_name = 'account_set_id';

-- 4. 账套表仍在（FMS 侧）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V193_FMS_ACCOUNT_SET_INTACT PASS'
            ELSE CONCAT('V193_FMS_ACCOUNT_SET_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'fms_account_set';

-- 5. 已挂账套的账簿数（信息性，0 属预期——尚无账簿挂账套）
SELECT CONCAT('V193_LINKED_BOOKS=', COUNT(*)) AS check_result
  FROM erp_account_book WHERE deleted = 0 AND account_set_id IS NOT NULL;

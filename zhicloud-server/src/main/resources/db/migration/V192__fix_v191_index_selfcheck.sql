-- ============================================================
-- V192：修正 V191 自检 2 的口径（索引存在性判断错误）
-- ============================================================
-- V191 的自检 2 用 `COUNT(*) = 1` 判断 idx_erp_account_book_set 是否建成，但该索引是
-- (tenant_id, account_set_id) 复合索引，**information_schema.statistics 每个索引列一行**，
-- 故 COUNT(*) 恒为 2，自检误报 FAIL。索引实际已正确建成（实测：idx_erp_account_book_set, 2 列）。
--
-- 修正：改用 COUNT(DISTINCT index_name) 判断存在性。这是本仓第二次踩同一个坑（复合索引按列计数），
-- 统一口径后固化，避免后续复合索引自检再次误报。
-- ============================================================

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V192_ACCOUNT_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V192_ACCOUNT_BOOK_SET_INDEX FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

-- 顺带固化列存在性（V191 自检 1 口径正确，复核一次）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V192_ACCOUNT_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V192_ACCOUNT_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

-- 存量账簿挂接率（信息性：0 表示尚无账簿挂账套，属预期；不为 0 说明已连通）
SELECT CONCAT('V192_LINKED_BOOKS=', COUNT(*)) AS check_result
  FROM erp_account_book WHERE deleted = 0 AND account_set_id IS NOT NULL;

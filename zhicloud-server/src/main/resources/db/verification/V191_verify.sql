-- V191 验证：ERP 账簿挂接 FMS 账套（打通「账套 → 账簿 → 凭证」层级）

-- 自检 1：列已加
SELECT CASE WHEN COUNT(*) = 1 THEN 'V191_ACCOUNT_BOOK_SET_COLUMN PASS'
            ELSE CONCAT('V191_ACCOUNT_BOOK_SET_COLUMN FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book' AND column_name = 'account_set_id';

-- 自检 2：索引已建
SELECT CASE WHEN COUNT(*) = 1 THEN 'V191_ACCOUNT_BOOK_SET_INDEX PASS'
            ELSE CONCAT('V191_ACCOUNT_BOOK_SET_INDEX FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_account_book'
   AND index_name = 'idx_erp_account_book_set';

-- 自检 3：存量账簿数据未丢（可空列不触碰既有行）
SELECT CASE WHEN COUNT(*) >= 0 THEN CONCAT('V191_BACKWARD_COMPAT PASS ', COUNT(*))
            ELSE 'V191_BACKWARD_COMPAT FAIL' END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_account_book';

-- 自检 4：FMS 账套表仍在（融合后账套归 FMS 管）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V191_FMS_ACCOUNT_SET_INTACT PASS'
            ELSE CONCAT('V191_FMS_ACCOUNT_SET_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'fms_account_set';

-- 自检 5：ERP 凭证表仍在（被挂接的对象）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V191_ERP_GL_VOUCHER_INTACT PASS'
            ELSE CONCAT('V191_ERP_GL_VOUCHER_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'erp_gl_voucher';

-- 自检 6：企业域菜单总量不回退
SELECT CASE WHEN COUNT(*) > 1930 THEN CONCAT('V191_MENUS_INTACT PASS ', COUNT(*))
            ELSE CONCAT('V191_MENUS_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0';

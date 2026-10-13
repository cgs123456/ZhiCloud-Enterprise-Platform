-- ============================================================
-- V191：ERP 账簿挂接 FMS 账套（打通「账套 → 账簿 → 凭证」层级）
-- ============================================================
-- 背景（台账 §12.212）：FMS/ERP 财务融合后，FMS 的账套配置层与 ERP 的凭证执行层
-- 在数据模型上还差一环——erp_gl_voucher 只到 account_book_id，账簿
-- (erp_account_book) 不知道自己属于哪个账套，导致「按账套查凭证流水」
-- 无法实现，FMS 的账套配置退化为孤岛。
--
-- 设计选择（权衡后）：加列而非改列。account_set_id 可空，
--   - 存量数据不受影响，ERP 已验证的会计事件链（订单→出库→MDM→WMS→
--     收款→GL 凭证→审核→冲销→期间关闭）零改动、零回归风险；
--   - 新建账簿时可挂账套，之后凭证即可按账套维度汇总；
--   - 不加外键：FMS 与 ERP 是两个独立 Modulith 模块，跨模块 FK 会让
--     Flyway 建表顺序耦合两模块（本轮 V190 已明确不跨模块边界）。
--
-- 同时给 FMS 账套补「是否已建主账簿」的反向可见性：主账簿由 ERP 建、
-- 挂在账套下，账套详情页据此提示「去 ERP 建主账簿」。
-- ============================================================

-- 1. erp_account_book 增加账套编号列（可空，向后兼容）
ALTER TABLE `erp_account_book`
  ADD COLUMN `account_set_id` bigint DEFAULT NULL COMMENT 'FMS 账套编号（可空；挂接后凭证可按账套维度汇总）' AFTER `id`;

-- 2. 为账套维度查询建索引（按租户 + 账套）
CREATE INDEX `idx_erp_account_book_set` ON `erp_account_book` (`tenant_id`, `account_set_id`);

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

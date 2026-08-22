-- ============================================================
-- V10: ERP 模块业务表二级索引补充（P0-5）
-- ============================================================
-- 背景：erp.sql 中 46 张业务表完全无二级索引，全表扫描导致查询性能差。
-- 策略：通过存储过程 + information_schema 判断索引是否存在，实现幂等添加。
--      Flyway 的 MySQLParser 支持 DELIMITER，存储过程体使用 $$ 作为语句结束符，
--      避免体内 ; 被解析器提前截断。
-- 覆盖索引：
--   1. (tenant_id, deleted)         —— 多租户 + 软删除复合过滤
--   2. (create_time)               —— 按创建时间范围查询/分页
--   3. (code, tenant_id) UNIQUE    —— 主数据表编码唯一约束
--   4. (no, tenant_id) UNIQUE      —— 业务单据号唯一约束
--   5. (status)                    —— 业务单据状态过滤
--   6. (parent_id) / (category_id) / (supplier_id) / (customer_id) 等外键索引
-- ============================================================

-- ------------------------------------------------------------ 
-- 幂等索引添加存储过程
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(500)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1072 BEGIN END;  -- 列名漂移（Key column doesn't exist）时静默跳过，避免迁移中断
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS p_add_unique_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_add_unique_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(500)
)
BEGIN
    DECLARE CONTINUE HANDLER FOR 1072 BEGIN END;  -- 列名漂移（Key column doesn't exist）时静默跳过，避免迁移中断
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 1. 基础主数据（7 张）
-- ============================================================
-- erp_product_category
CALL p_add_index_if_not_exists('erp_product_category', 'idx_erp_product_category_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_product_category', 'idx_erp_product_category_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_product_category', 'idx_erp_product_category_parent_id', 'parent_id');
CALL p_add_unique_if_not_exists('erp_product_category', 'uk_erp_product_category_code_tenant', 'code, tenant_id');

-- erp_product_unit
CALL p_add_index_if_not_exists('erp_product_unit', 'idx_erp_product_unit_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_product_unit', 'idx_erp_product_unit_create_time', 'create_time');
CALL p_add_unique_if_not_exists('erp_product_unit', 'uk_erp_product_unit_name_tenant', 'name, tenant_id');

-- erp_product
CALL p_add_index_if_not_exists('erp_product', 'idx_erp_product_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_product', 'idx_erp_product_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_product', 'idx_erp_product_category_id', 'category_id');
CALL p_add_index_if_not_exists('erp_product', 'idx_erp_product_unit_id', 'unit_id');
CALL p_add_index_if_not_exists('erp_product', 'idx_erp_product_status', 'status');
CALL p_add_index_if_not_exists('erp_product', 'idx_erp_product_barcode', 'barcode');
CALL p_add_unique_if_not_exists('erp_product', 'uk_erp_product_code_tenant', 'code, tenant_id');

-- erp_supplier
CALL p_add_index_if_not_exists('erp_supplier', 'idx_erp_supplier_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_supplier', 'idx_erp_supplier_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_supplier', 'idx_erp_supplier_status', 'status');
CALL p_add_unique_if_not_exists('erp_supplier', 'uk_erp_supplier_code_tenant', 'code, tenant_id');

-- erp_customer
CALL p_add_index_if_not_exists('erp_customer', 'idx_erp_customer_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_customer', 'idx_erp_customer_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_customer', 'idx_erp_customer_status', 'status');
CALL p_add_unique_if_not_exists('erp_customer', 'uk_erp_customer_code_tenant', 'code, tenant_id');

-- erp_warehouse
CALL p_add_index_if_not_exists('erp_warehouse', 'idx_erp_warehouse_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_warehouse', 'idx_erp_warehouse_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_warehouse', 'idx_erp_warehouse_status', 'status');
CALL p_add_unique_if_not_exists('erp_warehouse', 'uk_erp_warehouse_code_tenant', 'code, tenant_id');

-- erp_account
CALL p_add_index_if_not_exists('erp_account', 'idx_erp_account_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_account', 'idx_erp_account_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_account', 'idx_erp_account_status', 'status');
CALL p_add_unique_if_not_exists('erp_account', 'uk_erp_account_code_tenant', 'code, tenant_id');

-- ============================================================
-- 2. 库存管理（10 张）
-- ============================================================
-- erp_stock
CALL p_add_index_if_not_exists('erp_stock', 'idx_erp_stock_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock', 'idx_erp_stock_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock', 'idx_erp_stock_warehouse_id_product_id', 'warehouse_id, product_id');

-- erp_stock_record
CALL p_add_index_if_not_exists('erp_stock_record', 'idx_erp_stock_record_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_record', 'idx_erp_stock_record_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_record', 'idx_erp_stock_record_product_id', 'product_id');
CALL p_add_index_if_not_exists('erp_stock_record', 'idx_erp_stock_record_warehouse_id', 'warehouse_id');
CALL p_add_index_if_not_exists('erp_stock_record', 'idx_erp_stock_record_business_type', 'biz_type');

-- erp_stock_in
CALL p_add_index_if_not_exists('erp_stock_in', 'idx_erp_stock_in_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_in', 'idx_erp_stock_in_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_in', 'idx_erp_stock_in_status', 'status');
CALL p_add_unique_if_not_exists('erp_stock_in', 'uk_erp_stock_in_no_tenant', 'no, tenant_id');

-- erp_stock_in_item
CALL p_add_index_if_not_exists('erp_stock_in_item', 'idx_erp_stock_in_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_in_item', 'idx_erp_stock_in_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_in_item', 'idx_erp_stock_in_item_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_stock_in_item', 'idx_erp_stock_in_item_product_id', 'product_id');

-- erp_stock_out
CALL p_add_index_if_not_exists('erp_stock_out', 'idx_erp_stock_out_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_out', 'idx_erp_stock_out_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_out', 'idx_erp_stock_out_status', 'status');
CALL p_add_unique_if_not_exists('erp_stock_out', 'uk_erp_stock_out_no_tenant', 'no, tenant_id');

-- erp_stock_out_item
CALL p_add_index_if_not_exists('erp_stock_out_item', 'idx_erp_stock_out_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_out_item', 'idx_erp_stock_out_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_out_item', 'idx_erp_stock_out_item_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_stock_out_item', 'idx_erp_stock_out_item_product_id', 'product_id');

-- erp_stock_move
CALL p_add_index_if_not_exists('erp_stock_move', 'idx_erp_stock_move_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_move', 'idx_erp_stock_move_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_move', 'idx_erp_stock_move_status', 'status');
CALL p_add_unique_if_not_exists('erp_stock_move', 'uk_erp_stock_move_no_tenant', 'no, tenant_id');

-- erp_stock_move_item
CALL p_add_index_if_not_exists('erp_stock_move_item', 'idx_erp_stock_move_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_move_item', 'idx_erp_stock_move_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_move_item', 'idx_erp_stock_move_item_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_stock_move_item', 'idx_erp_stock_move_item_product_id', 'product_id');

-- erp_stock_check
CALL p_add_index_if_not_exists('erp_stock_check', 'idx_erp_stock_check_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_check', 'idx_erp_stock_check_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_check', 'idx_erp_stock_check_status', 'status');
CALL p_add_unique_if_not_exists('erp_stock_check', 'uk_erp_stock_check_no_tenant', 'no, tenant_id');

-- erp_stock_check_item
CALL p_add_index_if_not_exists('erp_stock_check_item', 'idx_erp_stock_check_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_check_item', 'idx_erp_stock_check_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_check_item', 'idx_erp_stock_check_item_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_stock_check_item', 'idx_erp_stock_check_item_product_id', 'product_id');

-- ============================================================
-- 3. 采购管理（6 张）
-- ============================================================
-- erp_purchase_order
CALL p_add_index_if_not_exists('erp_purchase_order', 'idx_erp_purchase_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_purchase_order', 'idx_erp_purchase_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_purchase_order', 'idx_erp_purchase_order_status', 'status');
CALL p_add_index_if_not_exists('erp_purchase_order', 'idx_erp_purchase_order_supplier_id', 'supplier_id');
CALL p_add_unique_if_not_exists('erp_purchase_order', 'uk_erp_purchase_order_no_tenant', 'no, tenant_id');

-- erp_purchase_order_items
CALL p_add_index_if_not_exists('erp_purchase_order_items', 'idx_erp_purchase_order_items_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_purchase_order_items', 'idx_erp_purchase_order_items_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_purchase_order_items', 'idx_erp_purchase_order_items_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_purchase_order_items', 'idx_erp_purchase_order_items_product_id', 'product_id');

-- erp_purchase_in
CALL p_add_index_if_not_exists('erp_purchase_in', 'idx_erp_purchase_in_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_purchase_in', 'idx_erp_purchase_in_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_purchase_in', 'idx_erp_purchase_in_status', 'status');
CALL p_add_index_if_not_exists('erp_purchase_in', 'idx_erp_purchase_in_supplier_id', 'supplier_id');
CALL p_add_unique_if_not_exists('erp_purchase_in', 'uk_erp_purchase_in_no_tenant', 'no, tenant_id');

-- erp_purchase_in_items
CALL p_add_index_if_not_exists('erp_purchase_in_items', 'idx_erp_purchase_in_items_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_purchase_in_items', 'idx_erp_purchase_in_items_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_purchase_in_items', 'idx_erp_purchase_in_items_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_purchase_in_items', 'idx_erp_purchase_in_items_product_id', 'product_id');

-- erp_purchase_return
CALL p_add_index_if_not_exists('erp_purchase_return', 'idx_erp_purchase_return_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_purchase_return', 'idx_erp_purchase_return_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_purchase_return', 'idx_erp_purchase_return_status', 'status');
CALL p_add_index_if_not_exists('erp_purchase_return', 'idx_erp_purchase_return_supplier_id', 'supplier_id');
CALL p_add_unique_if_not_exists('erp_purchase_return', 'uk_erp_purchase_return_no_tenant', 'no, tenant_id');

-- erp_purchase_return_items
CALL p_add_index_if_not_exists('erp_purchase_return_items', 'idx_erp_purchase_return_items_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_purchase_return_items', 'idx_erp_purchase_return_items_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_purchase_return_items', 'idx_erp_purchase_return_items_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_purchase_return_items', 'idx_erp_purchase_return_items_product_id', 'product_id');

-- ============================================================
-- 4. 销售管理（6 张）
-- ============================================================
-- erp_sale_order
CALL p_add_index_if_not_exists('erp_sale_order', 'idx_erp_sale_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_sale_order', 'idx_erp_sale_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_sale_order', 'idx_erp_sale_order_status', 'status');
CALL p_add_index_if_not_exists('erp_sale_order', 'idx_erp_sale_order_customer_id', 'customer_id');
CALL p_add_unique_if_not_exists('erp_sale_order', 'uk_erp_sale_order_no_tenant', 'no, tenant_id');

-- erp_sale_order_items
CALL p_add_index_if_not_exists('erp_sale_order_items', 'idx_erp_sale_order_items_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_sale_order_items', 'idx_erp_sale_order_items_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_sale_order_items', 'idx_erp_sale_order_items_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_sale_order_items', 'idx_erp_sale_order_items_product_id', 'product_id');

-- erp_sale_out
CALL p_add_index_if_not_exists('erp_sale_out', 'idx_erp_sale_out_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_sale_out', 'idx_erp_sale_out_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_sale_out', 'idx_erp_sale_out_status', 'status');
CALL p_add_index_if_not_exists('erp_sale_out', 'idx_erp_sale_out_customer_id', 'customer_id');
CALL p_add_unique_if_not_exists('erp_sale_out', 'uk_erp_sale_out_no_tenant', 'no, tenant_id');

-- erp_sale_out_items
CALL p_add_index_if_not_exists('erp_sale_out_items', 'idx_erp_sale_out_items_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_sale_out_items', 'idx_erp_sale_out_items_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_sale_out_items', 'idx_erp_sale_out_items_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_sale_out_items', 'idx_erp_sale_out_items_product_id', 'product_id');

-- erp_sale_return
CALL p_add_index_if_not_exists('erp_sale_return', 'idx_erp_sale_return_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_sale_return', 'idx_erp_sale_return_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_sale_return', 'idx_erp_sale_return_status', 'status');
CALL p_add_index_if_not_exists('erp_sale_return', 'idx_erp_sale_return_customer_id', 'customer_id');
CALL p_add_unique_if_not_exists('erp_sale_return', 'uk_erp_sale_return_no_tenant', 'no, tenant_id');

-- erp_sale_return_items
CALL p_add_index_if_not_exists('erp_sale_return_items', 'idx_erp_sale_return_items_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_sale_return_items', 'idx_erp_sale_return_items_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_sale_return_items', 'idx_erp_sale_return_items_order_id', 'master_id');
CALL p_add_index_if_not_exists('erp_sale_return_items', 'idx_erp_sale_return_items_product_id', 'product_id');

-- ============================================================
-- 5. 财务管理（4 张）
-- ============================================================
-- erp_finance_payment
CALL p_add_index_if_not_exists('erp_finance_payment', 'idx_erp_finance_payment_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_finance_payment', 'idx_erp_finance_payment_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_finance_payment', 'idx_erp_finance_payment_status', 'status');
CALL p_add_index_if_not_exists('erp_finance_payment', 'idx_erp_finance_payment_supplier_id', 'supplier_id');
CALL p_add_unique_if_not_exists('erp_finance_payment', 'uk_erp_finance_payment_no_tenant', 'no, tenant_id');

-- erp_finance_payment_item
CALL p_add_index_if_not_exists('erp_finance_payment_item', 'idx_erp_finance_payment_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_finance_payment_item', 'idx_erp_finance_payment_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_finance_payment_item', 'idx_erp_finance_payment_item_order_id', 'master_id');

-- erp_finance_receipt
CALL p_add_index_if_not_exists('erp_finance_receipt', 'idx_erp_finance_receipt_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_finance_receipt', 'idx_erp_finance_receipt_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_finance_receipt', 'idx_erp_finance_receipt_status', 'status');
CALL p_add_index_if_not_exists('erp_finance_receipt', 'idx_erp_finance_receipt_customer_id', 'customer_id');
CALL p_add_unique_if_not_exists('erp_finance_receipt', 'uk_erp_finance_receipt_no_tenant', 'no, tenant_id');

-- erp_finance_receipt_item
CALL p_add_index_if_not_exists('erp_finance_receipt_item', 'idx_erp_finance_receipt_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_finance_receipt_item', 'idx_erp_finance_receipt_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_finance_receipt_item', 'idx_erp_finance_receipt_item_order_id', 'master_id');

-- ============================================================
-- 6. 会计期间（2 张）
-- ============================================================
-- erp_period
CALL p_add_index_if_not_exists('erp_period', 'idx_erp_period_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_period', 'idx_erp_period_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_period', 'idx_erp_period_status', 'status');
CALL p_add_unique_if_not_exists('erp_period', 'uk_erp_period_code_tenant', 'code, tenant_id');

-- erp_period_close
CALL p_add_index_if_not_exists('erp_period_close', 'idx_erp_period_close_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_period_close', 'idx_erp_period_close_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_period_close', 'idx_erp_period_close_period_id', 'period_id');

-- ============================================================
-- 7. 总账（3 张）
-- ============================================================
-- erp_gl_account
CALL p_add_index_if_not_exists('erp_gl_account', 'idx_erp_gl_account_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_gl_account', 'idx_erp_gl_account_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_gl_account', 'idx_erp_gl_account_status', 'status');
CALL p_add_index_if_not_exists('erp_gl_account', 'idx_erp_gl_account_parent_id', 'parent_id');
CALL p_add_unique_if_not_exists('erp_gl_account', 'uk_erp_gl_account_code_tenant', 'code, tenant_id');

-- erp_gl_voucher
CALL p_add_index_if_not_exists('erp_gl_voucher', 'idx_erp_gl_voucher_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_gl_voucher', 'idx_erp_gl_voucher_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_gl_voucher', 'idx_erp_gl_voucher_status', 'status');
CALL p_add_index_if_not_exists('erp_gl_voucher', 'idx_erp_gl_voucher_period_id', 'period_id');
CALL p_add_unique_if_not_exists('erp_gl_voucher', 'uk_erp_gl_voucher_no_tenant', 'voucher_no, tenant_id');

-- erp_gl_voucher_entry
CALL p_add_index_if_not_exists('erp_gl_voucher_entry', 'idx_erp_gl_voucher_entry_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_gl_voucher_entry', 'idx_erp_gl_voucher_entry_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_gl_voucher_entry', 'idx_erp_gl_voucher_entry_voucher_id', 'voucher_id');
CALL p_add_index_if_not_exists('erp_gl_voucher_entry', 'idx_erp_gl_voucher_entry_account_id', 'account_id');

-- ============================================================
-- 8. 批次/序列号管理（2 张）
-- ============================================================
-- erp_stock_batch
CALL p_add_index_if_not_exists('erp_stock_batch', 'idx_erp_stock_batch_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_batch', 'idx_erp_stock_batch_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_batch', 'idx_erp_stock_batch_status', 'status');
CALL p_add_index_if_not_exists('erp_stock_batch', 'idx_erp_stock_batch_product_id', 'product_id');
CALL p_add_index_if_not_exists('erp_stock_batch', 'idx_erp_stock_batch_warehouse_id', 'warehouse_id');
CALL p_add_index_if_not_exists('erp_stock_batch', 'idx_erp_stock_batch_batch_no', 'batch_no');

-- erp_stock_serial
CALL p_add_index_if_not_exists('erp_stock_serial', 'idx_erp_stock_serial_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_stock_serial', 'idx_erp_stock_serial_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_stock_serial', 'idx_erp_stock_serial_status', 'status');
CALL p_add_index_if_not_exists('erp_stock_serial', 'idx_erp_stock_serial_product_id', 'product_id');
CALL p_add_index_if_not_exists('erp_stock_serial', 'idx_erp_stock_serial_warehouse_id', 'warehouse_id');
CALL p_add_index_if_not_exists('erp_stock_serial', 'idx_erp_stock_serial_serial_no', 'serial_no');

-- ============================================================
-- 9. 多币种管理（2 张）
-- ============================================================
-- erp_currency
CALL p_add_index_if_not_exists('erp_currency', 'idx_erp_currency_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_currency', 'idx_erp_currency_create_time', 'create_time');
CALL p_add_unique_if_not_exists('erp_currency', 'uk_erp_currency_code', 'code');

-- erp_exchange_rate
CALL p_add_index_if_not_exists('erp_exchange_rate', 'idx_erp_exchange_rate_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_exchange_rate', 'idx_erp_exchange_rate_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_exchange_rate', 'idx_erp_exchange_rate_from_currency_id', 'from_currency_id');
CALL p_add_index_if_not_exists('erp_exchange_rate', 'idx_erp_exchange_rate_to_currency_id', 'to_currency_id');

-- ============================================================
-- 10. 管理会计（4 张）
-- ============================================================
-- erp_cost_center
CALL p_add_index_if_not_exists('erp_cost_center', 'idx_erp_cost_center_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_cost_center', 'idx_erp_cost_center_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_cost_center', 'idx_erp_cost_center_status', 'status');
CALL p_add_unique_if_not_exists('erp_cost_center', 'uk_erp_cost_center_code_tenant', 'code, tenant_id');

-- erp_profit_center
CALL p_add_index_if_not_exists('erp_profit_center', 'idx_erp_profit_center_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_profit_center', 'idx_erp_profit_center_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_profit_center', 'idx_erp_profit_center_status', 'status');
CALL p_add_unique_if_not_exists('erp_profit_center', 'uk_erp_profit_center_code_tenant', 'code, tenant_id');

-- erp_cost_allocation
CALL p_add_index_if_not_exists('erp_cost_allocation', 'idx_erp_cost_allocation_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_cost_allocation', 'idx_erp_cost_allocation_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_cost_allocation', 'idx_erp_cost_allocation_cost_center_id', 'cost_center_id');

-- erp_profitability_analysis
CALL p_add_index_if_not_exists('erp_profitability_analysis', 'idx_erp_profitability_analysis_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('erp_profitability_analysis', 'idx_erp_profitability_analysis_create_time', 'create_time');
CALL p_add_index_if_not_exists('erp_profitability_analysis', 'idx_erp_profitability_analysis_profit_center_id', 'profit_center_id');

-- ============================================================
-- 清理存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;
DROP PROCEDURE IF EXISTS p_add_unique_if_not_exists;

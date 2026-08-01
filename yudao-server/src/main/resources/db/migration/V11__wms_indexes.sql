-- ============================================================
-- V11: WMS 模块业务表二级索引补充（P0-5）
-- ============================================================
-- 背景：wms.sql 中已有 idx_tenant_id 和 uk_no/uk_code，但缺少：
--   1. (tenant_id, deleted) 复合索引 —— 多租户 + 软删除高效过滤
--   2. (create_time) 索引 —— 按创建时间查询/分页
--   3. (status) 索引 —— 业务单据状态过滤
--   4. (order_time) 索引 —— 按单据日期范围查询
-- 复用 V10 中的存储过程 p_add_index_if_not_exists / p_add_unique_if_not_exists
-- ============================================================

DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;
CREATE PROCEDURE p_add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(500)
)
BEGIN
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
END;

-- ============================================================
-- 1. 基础主数据（5 张）
-- ============================================================
-- wms_warehouse（已有 uk_code、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_warehouse', 'idx_wms_warehouse_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_warehouse', 'idx_wms_warehouse_create_time', 'create_time');

-- wms_merchant（已有 uk_code、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_merchant', 'idx_wms_merchant_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_merchant', 'idx_wms_merchant_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_merchant', 'idx_wms_merchant_type', 'type');

-- wms_item_brand（已有 uk_code、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_item_brand', 'idx_wms_item_brand_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_item_brand', 'idx_wms_item_brand_create_time', 'create_time');

-- wms_item_category（已有 uk_code、idx_parent_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_item_category', 'idx_wms_item_category_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_item_category', 'idx_wms_item_category_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_item_category', 'idx_wms_item_category_status', 'status');

-- wms_item（已有 idx_category_id、idx_brand_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_item', 'idx_wms_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_item', 'idx_wms_item_create_time', 'create_time');

-- wms_item_sku（已有 idx_item_id、idx_bar_code、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_item_sku', 'idx_wms_item_sku_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_item_sku', 'idx_wms_item_sku_create_time', 'create_time');

-- ============================================================
-- 2. 库存核心（2 张）
-- ============================================================
-- wms_inventory（已有 uk_sku_warehouse、idx_warehouse_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_inventory', 'idx_wms_inventory_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_inventory', 'idx_wms_inventory_create_time', 'create_time');

-- wms_inventory_history（已有 idx_warehouse_sku、idx_order、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_inventory_history', 'idx_wms_inventory_history_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_inventory_history', 'idx_wms_inventory_history_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_inventory_history', 'idx_wms_inventory_history_order_type', 'order_type');

-- ============================================================
-- 3. 库存单据（8 张）
-- ============================================================
-- wms_receipt_order（已有 uk_no、idx_warehouse_id、idx_merchant_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_receipt_order', 'idx_wms_receipt_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_receipt_order', 'idx_wms_receipt_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_receipt_order', 'idx_wms_receipt_order_status', 'status');
CALL p_add_index_if_not_exists('wms_receipt_order', 'idx_wms_receipt_order_order_time', 'order_time');
CALL p_add_index_if_not_exists('wms_receipt_order', 'idx_wms_receipt_order_type', 'type');

-- wms_receipt_order_detail（已有 idx_order_id、idx_sku_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_receipt_order_detail', 'idx_wms_receipt_order_detail_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_receipt_order_detail', 'idx_wms_receipt_order_detail_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_receipt_order_detail', 'idx_wms_receipt_order_detail_warehouse_id', 'warehouse_id');

-- wms_shipment_order（已有 uk_no、idx_warehouse_id、idx_merchant_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_shipment_order', 'idx_wms_shipment_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_shipment_order', 'idx_wms_shipment_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_shipment_order', 'idx_wms_shipment_order_status', 'status');
CALL p_add_index_if_not_exists('wms_shipment_order', 'idx_wms_shipment_order_order_time', 'order_time');
CALL p_add_index_if_not_exists('wms_shipment_order', 'idx_wms_shipment_order_type', 'type');

-- wms_shipment_order_detail（已有 idx_order_id、idx_sku_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_shipment_order_detail', 'idx_wms_shipment_order_detail_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_shipment_order_detail', 'idx_wms_shipment_order_detail_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_shipment_order_detail', 'idx_wms_shipment_order_detail_warehouse_id', 'warehouse_id');

-- wms_movement_order（已有 uk_no、idx_source_warehouse_id、idx_target_warehouse_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_movement_order', 'idx_wms_movement_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_movement_order', 'idx_wms_movement_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_movement_order', 'idx_wms_movement_order_status', 'status');
CALL p_add_index_if_not_exists('wms_movement_order', 'idx_wms_movement_order_order_time', 'order_time');

-- wms_movement_order_detail（已有 idx_order_id、idx_sku_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_movement_order_detail', 'idx_wms_movement_order_detail_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_movement_order_detail', 'idx_wms_movement_order_detail_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_movement_order_detail', 'idx_wms_movement_order_detail_source_warehouse_id', 'source_warehouse_id');
CALL p_add_index_if_not_exists('wms_movement_order_detail', 'idx_wms_movement_order_detail_target_warehouse_id', 'target_warehouse_id');

-- wms_check_order（已有 uk_no、idx_warehouse_id、idx_tenant_id）
CALL p_add_index_if_not_exists('wms_check_order', 'idx_wms_check_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_check_order', 'idx_wms_check_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_check_order', 'idx_wms_check_order_status', 'status');
CALL p_add_index_if_not_exists('wms_check_order', 'idx_wms_check_order_order_time', 'order_time');

-- wms_check_order_detail
CALL p_add_index_if_not_exists('wms_check_order_detail', 'idx_wms_check_order_detail_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_check_order_detail', 'idx_wms_check_order_detail_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_check_order_detail', 'idx_wms_check_order_detail_warehouse_id', 'warehouse_id');

-- ============================================================
-- 4. 波次单（2 张）
-- ============================================================
-- wms_wave_order（已有 uk_no）
CALL p_add_index_if_not_exists('wms_wave_order', 'idx_wms_wave_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_wave_order', 'idx_wms_wave_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_wave_order', 'idx_wms_wave_order_status', 'status');
CALL p_add_index_if_not_exists('wms_wave_order', 'idx_wms_wave_order_warehouse_id', 'warehouse_id');
CALL p_add_index_if_not_exists('wms_wave_order', 'idx_wms_wave_order_order_time', 'order_time');
CALL p_add_index_if_not_exists('wms_wave_order', 'idx_wms_wave_order_strategy', 'strategy');

-- wms_wave_order_detail（已有 idx_wave_order_id、idx_shipment_order_id）
CALL p_add_index_if_not_exists('wms_wave_order_detail', 'idx_wms_wave_order_detail_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_wave_order_detail', 'idx_wms_wave_order_detail_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_wave_order_detail', 'idx_wms_wave_order_detail_sku_id', 'sku_id');

-- ============================================================
-- 5. 库存批次（1 张）
-- ============================================================
-- wms_inventory_batch
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_create_time', 'create_time');
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_status', 'status');
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_sku_id', 'sku_id');
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_warehouse_id', 'warehouse_id');
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_batch_no', 'batch_no');
CALL p_add_index_if_not_exists('wms_inventory_batch', 'idx_wms_inventory_batch_expiry_date', 'expiry_date');

-- ============================================================
-- 清理存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;

-- V205 验证：wms_inventory 库位维度接入 + ASN 明细收货库位列（§12.259，ADR 259）

-- 1. 两表新列存在
SELECT CASE WHEN COUNT(*) = 2 THEN 'V205_WMS_LOCATION_DIM_COLUMNS_PASS'
            ELSE CONCAT('V205_WMS_LOCATION_DIM_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND ((TABLE_NAME = 'wms_inventory' AND COLUMN_NAME = 'location_id')
     OR (TABLE_NAME = 'wms_asn_order_detail' AND COLUMN_NAME = 'location_id'));

-- 2. 旧两列唯一键已删
SELECT CASE WHEN COUNT(DISTINCT INDEX_NAME) = 0 THEN 'V205_OLD_UK_DROPPED_PASS'
            ELSE CONCAT('V205_OLD_UK_DROPPED_FAIL ', COUNT(DISTINCT INDEX_NAME)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory'
   AND INDEX_NAME = 'uk_sku_warehouse';

-- 3. 新三元唯一键就位（3 列，列名精确）
SELECT CASE WHEN COUNT(*) = 3 AND COUNT(DISTINCT column_name) = 3
              AND SUM(column_name IN ('sku_id', 'warehouse_id', 'location_id')) = 3
            THEN 'V205_NEW_UK_PASS'
            ELSE CONCAT('V205_NEW_UK_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory'
   AND INDEX_NAME = 'uk_sku_warehouse_location';

-- 4. 库位维索引就位
SELECT CASE WHEN COUNT(*) = 1 THEN 'V205_IDX_LOCATION_ID_PASS'
            ELSE CONCAT('V205_IDX_LOCATION_ID_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory'
   AND INDEX_NAME = 'idx_location_id';

-- 5. 存量库存行 location_id 全归 0（幂等反证；直灌重跑不漂移）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V205_INVENTORY_LOCATION_BACKFILL_PASS'
            ELSE CONCAT('V205_INVENTORY_LOCATION_BACKFILL_FAIL ', COUNT(*)) END AS check_result
  FROM wms_inventory
 WHERE deleted = b'0' AND (location_id IS NULL OR location_id <> 0);

-- 6. ASN 明细存量行 location_id 全归 0
SELECT CASE WHEN COUNT(*) = 0 THEN 'V205_ASN_DETAIL_LOCATION_BACKFILL_PASS'
            ELSE CONCAT('V205_ASN_DETAIL_LOCATION_BACKFILL_FAIL ', COUNT(*)) END AS check_result
  FROM wms_asn_order_detail
 WHERE deleted = b'0' AND (location_id IS NULL OR location_id <> 0);

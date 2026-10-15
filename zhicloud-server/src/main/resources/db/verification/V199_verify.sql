-- V199 验证：erp_stock 唯一键

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V199_UNIQUE_KEY_PASS'
            ELSE CONCAT('V199_UNIQUE_KEY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
   AND index_name = 'uk_erp_stock_tenant_product_warehouse';

SELECT CASE WHEN COUNT(*) = 3 THEN 'V199_UNIQUE_KEY_COLUMNS_PASS'
            ELSE CONCAT('V199_UNIQUE_KEY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
   AND index_name = 'uk_erp_stock_tenant_product_warehouse'
   AND column_name IN ('tenant_id', 'product_id', 'warehouse_id');

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V199_PLAIN_INDEX_KEPT_PASS'
            ELSE CONCAT('V199_PLAIN_INDEX_KEPT_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
   AND index_name = 'idx_erp_stock_warehouse_id_product_id';

-- ============================================================
-- V119 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. inventory_item 存在，且租户复合键涉及的六个列存在、类型为 BIGINT、
--      不允许 NULL；
--   2. uk_inventory_item_tenant_composite 存在、为 UNIQUE BTREE，列顺序精确为
--      (tenant_id, item_id, warehouse_id, location_id, area_id, batch_id)；
--   3. 旧全局唯一键 uk_item_warehouse_location_area_batch 已不存在，且不存在
--      任何仍按旧五列全局唯一的 UNIQUE 索引；
--   4. (tenant_id, item_id, warehouse_id, location_id, area_id, batch_id)
--      无重复值。
-- ============================================================

SELECT 'V119_INVENTORY_ITEM_TENANT_COLUMNS' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND column_name = 'tenant_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND column_name = 'item_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND column_name = 'warehouse_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND column_name = 'location_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND column_name = 'area_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           AND EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND column_name = 'batch_id'
                  AND LOWER(data_type) = 'bigint'
                  AND is_nullable = 'NO'
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V119_INVENTORY_ITEM_TENANT_UNIQUE_INDEX' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'inventory_item'
                  AND index_name = 'uk_inventory_item_tenant_composite'
                GROUP BY table_name, index_name
               HAVING COUNT(*) = 6
                  AND MAX(non_unique) = 0
                  AND SUM(CASE WHEN column_name = 'tenant_id' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'item_id' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'warehouse_id' AND seq_in_index = 3 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'location_id' AND seq_in_index = 4 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'area_id' AND seq_in_index = 5 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN column_name = 'batch_id' AND seq_in_index = 6 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
                  AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 6
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V119_INVENTORY_ITEM_LEGACY_INDEX_REMOVED' AS check_name,
       CASE
           WHEN (SELECT COUNT(*)
                   FROM information_schema.statistics
                  WHERE table_schema = DATABASE()
                    AND table_name = 'inventory_item'
                    AND index_name = 'uk_item_warehouse_location_area_batch') = 0
            AND (SELECT COUNT(*)
                   FROM (
                       SELECT index_name
                         FROM information_schema.statistics
                        WHERE table_schema = DATABASE()
                          AND table_name = 'inventory_item'
                          AND non_unique = 0
                        GROUP BY index_name
                       HAVING COUNT(*) = 5
                          AND LOWER(GROUP_CONCAT(column_name ORDER BY column_name)) =
                              'area_id,batch_id,item_id,location_id,warehouse_id'
                   ) AS global_unique_indexes) = 0
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V119_INVENTORY_ITEM_TENANT_UNIQUE_DATA' AS check_name,
       CASE
           WHEN NOT EXISTS (
               SELECT 1
                 FROM inventory_item
                GROUP BY tenant_id, item_id, warehouse_id, location_id, area_id, batch_id
               HAVING COUNT(*) > 1
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

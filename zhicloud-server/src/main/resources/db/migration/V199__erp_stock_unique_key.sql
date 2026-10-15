-- ============================================================
-- V199：erp_stock 补 (tenant_id, product_id, warehouse_id) 唯一键（真实缺陷修复）
-- ============================================================
-- 背景（台账 §12.224.2 #1）：ErpStockServiceImpl.updateStockCountIncrement 的
-- 并发初始化路径写着「依赖 erp_stock 的 (product_id, warehouse_id) 唯一约束」并
-- catch (DuplicateKeyException) 回查——但**该唯一约束在仓库中不存在**
-- （真机 SHOW INDEX：idx_erp_stock_warehouse_id_product_id 的 non_unique=1，是普通索引）。
-- 后果：并发首建同一 (产品, 仓库) 会插入两行 → selectByProductIdAndWarehouseId 的
-- selectOne 抛 TooManyResultsException；且 CAS 按 id 更新，两行余额各自漂移。
-- 注释在说谎，会误导后续维护者以为已有防护。
--
-- 修法：补唯一键（含 tenant_id，多租户下同一产品在不同租户可各有一行）。
-- 存量安全：改前自检无重复行（见自检 3），有重复则 ALTER 报 1062 失败、阻止带病上线。
-- ============================================================

ALTER TABLE `erp_stock`
  ADD UNIQUE KEY `uk_erp_stock_tenant_product_warehouse` (`tenant_id`, `product_id`, `warehouse_id`);

-- 自检 1：唯一键已在（COUNT(DISTINCT index_name) 口径，V192 固化）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V199_UNIQUE_KEY_PASS'
            ELSE CONCAT('V199_UNIQUE_KEY_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
   AND index_name = 'uk_erp_stock_tenant_product_warehouse';

-- 自检 2：唯一键覆盖 (tenant_id, product_id, warehouse_id) 三列
SELECT CASE WHEN COUNT(*) = 3 THEN 'V199_UNIQUE_KEY_COLUMNS_PASS'
            ELSE CONCAT('V199_UNIQUE_KEY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
   AND index_name = 'uk_erp_stock_tenant_product_warehouse'
   AND column_name IN ('tenant_id', 'product_id', 'warehouse_id');

-- 自检 3：存量无重复维度（信息性，0 为预期）
SELECT CONCAT('V199_DUPLICATE_DIMENSIONS=', COUNT(*)) AS check_result
  FROM (SELECT tenant_id, product_id, warehouse_id FROM `erp_stock`
         GROUP BY tenant_id, product_id, warehouse_id HAVING COUNT(*) > 1) t;

-- 自检 4：旧的普通索引保留（查询仍走它，不误删）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V199_PLAIN_INDEX_KEPT_PASS'
            ELSE CONCAT('V199_PLAIN_INDEX_KEPT_FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'erp_stock'
   AND index_name = 'idx_erp_stock_warehouse_id_product_id';

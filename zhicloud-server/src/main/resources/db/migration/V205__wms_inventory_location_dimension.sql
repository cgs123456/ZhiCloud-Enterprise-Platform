-- ============================================================
-- V205：wms_inventory 库位维度接入 + wms_asn_order_detail 收货库位列
-- ============================================================
-- 依据：台账 §12.242.5-1/3/4/7 + §12.242 D4①② + §12.258.2 前置链
-- （C 组第 1 项，ADR Temp/zc_adr_259.md；收口六断点之「WMS 库位」根因面）。
--
-- 内容：
--   1. wms_inventory 加列 location_id（0 = 未定位；存量行默认即 0，基线行为零变化）
--   2. 唯一键 (sku_id, warehouse_id) -> (sku_id, warehouse_id, location_id)
--   3. 索引 idx_location_id（deleteLocation 冲洗 + 占用派生计数用）
--   4. wms_asn_order_detail 加列 location_id（收货库位落库，0 = 未指定）
--
-- 幂等：列/键/索引均带 information_schema 防重跑护栏（直灌重跑安全）。
-- 存量安全：唯一键换元前全表 location_id 恒 0，三元键天然无重复。
-- 回滚预案（如需）：恢复两列唯一键并 DROP COLUMN（数据不可逆，操作前快照）。
-- ============================================================

-- 1. wms_inventory 加列（护栏：列已存在则跳过）
SET @v205_col := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory' AND COLUMN_NAME = 'location_id');
SET @v205_sql := IF(@v205_col = 0,
    'ALTER TABLE `wms_inventory` ADD COLUMN `location_id` BIGINT NOT NULL DEFAULT 0 COMMENT \'库位编号，0 = 未定位（V205 库位维度接入）\' AFTER `warehouse_id`',
    'SELECT \'V205 skip: wms_inventory.location_id exists\'');
PREPARE v205_stmt FROM @v205_sql; EXECUTE v205_stmt; DEALLOCATE PREPARE v205_stmt;

-- 2. 唯一键换元（旧两列键删除 -> 新三元键）
SET @v205_old_uk := (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory' AND INDEX_NAME = 'uk_sku_warehouse');
SET @v205_sql := IF(@v205_old_uk > 0,
    'ALTER TABLE `wms_inventory` DROP INDEX `uk_sku_warehouse`',
    'SELECT \'V205 skip: uk_sku_warehouse absent\'');
PREPARE v205_stmt FROM @v205_sql; EXECUTE v205_stmt; DEALLOCATE PREPARE v205_stmt;

SET @v205_new_uk := (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory' AND INDEX_NAME = 'uk_sku_warehouse_location');
SET @v205_sql := IF(@v205_new_uk = 0,
    'ALTER TABLE `wms_inventory` ADD UNIQUE KEY `uk_sku_warehouse_location` (`sku_id`, `warehouse_id`, `location_id`)',
    'SELECT \'V205 skip: uk_sku_warehouse_location exists\'');
PREPARE v205_stmt FROM @v205_sql; EXECUTE v205_stmt; DEALLOCATE PREPARE v205_stmt;

-- 3. 库位维索引（护栏：已存在则跳过）
SET @v205_idx := (SELECT COUNT(1) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory' AND INDEX_NAME = 'idx_location_id');
SET @v205_sql := IF(@v205_idx = 0,
    'ALTER TABLE `wms_inventory` ADD KEY `idx_location_id` (`location_id`)',
    'SELECT \'V205 skip: idx_location_id exists\'');
PREPARE v205_stmt FROM @v205_sql; EXECUTE v205_stmt; DEALLOCATE PREPARE v205_stmt;

-- 4. ASN 明细加收货库位列（护栏同上）
SET @v205_col2 := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_asn_order_detail' AND COLUMN_NAME = 'location_id');
SET @v205_sql := IF(@v205_col2 = 0,
    'ALTER TABLE `wms_asn_order_detail` ADD COLUMN `location_id` BIGINT NOT NULL DEFAULT 0 COMMENT \'收货库位编号，0 = 未指定（V205 收货库位落库）\'',
    'SELECT \'V205 skip: wms_asn_order_detail.location_id exists\'');
PREPARE v205_stmt FROM @v205_sql; EXECUTE v205_stmt; DEALLOCATE PREPARE v205_stmt;

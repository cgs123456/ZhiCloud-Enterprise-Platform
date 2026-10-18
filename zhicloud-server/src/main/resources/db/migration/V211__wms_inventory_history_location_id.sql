-- ============================================================
-- V211：wms_inventory_history 库位维度接入（行级流水）
-- ============================================================
-- 依据：ADR 269（D3 WMS 库位维度后继：行级流水；台账 §12.269）——
-- C 组第 1 项 ADR 259 D7「行级细粒度流水属后续项」的后续项。
--
-- 内容：
--   1. wms_inventory_history 加列 location_id（0 = 未定位；存量行默认即 0，基线行为零变化）
--      P-deduct 跨位分配由「明细聚合单条流水」改「参与行逐行一条」，各带行库位；
--      历史行无库位可回溯，不回填（未定位语义）。
--
-- 幂等：列带 information_schema 防重跑护栏（直灌重跑安全）。
-- 存量安全：新增列默认 0，无 NOT NULL 违反面；无键/索引变更。
-- 回滚预案（如需）：ALTER TABLE ... DROP COLUMN location_id（数据不可逆，操作前快照）。
-- ============================================================

-- 1. wms_inventory_history 加列（护栏：列已存在则跳过）
SET @v211_col := (SELECT COUNT(1) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_history' AND COLUMN_NAME = 'location_id');
SET @v211_sql := IF(@v211_col = 0,
    'ALTER TABLE `wms_inventory_history` ADD COLUMN `location_id` BIGINT NOT NULL DEFAULT 0 COMMENT \'库位编号，0 = 未定位（V211 行级流水库位维度）\' AFTER `warehouse_id`',
    'SELECT \'V211 skip: wms_inventory_history.location_id exists\'');
PREPARE v211_stmt FROM @v211_sql; EXECUTE v211_stmt; DEALLOCATE PREPARE v211_stmt;

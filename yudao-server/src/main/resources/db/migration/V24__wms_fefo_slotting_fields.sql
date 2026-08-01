-- ============================================================
-- V24: WMS FEFO 保质期 + 上架 Slotting 字段（P0-13）
--
-- 为 wms_receipt_order_detail 添加：
--   batch_no        VARCHAR(64)  批次号
--   production_date DATE         生产日期
--   expiry_date     DATE         过期日期（空表示无保质期管理）
--
-- 设计说明：
--   1) batch_no 配合 wms_inventory_batch.batch_no 实现批次合并/追溯
--   2) production_date / expiry_date 用于 FEFO 先到期先出策略
--   3) 收货时录入批次信息，PDA 上架时携带批次到 wms_inventory_batch
--
-- 注意：wms_inventory_batch 表已在 V16__wms_ddl.sql 中创建，包含 batch_no /
--       production_date / expiry_date / quantity / locked_quantity / status 字段。
--       本脚本无需修改批次表，仅扩展收货单明细的批次录入能力。
-- ============================================================

DROP PROCEDURE IF EXISTS p_wms_fefo_add_columns;
DELIMITER $$
CREATE PROCEDURE p_wms_fefo_add_columns(IN tableName VARCHAR(128), IN columnName VARCHAR(64),
                                        IN columnDef TEXT, IN columnComment VARCHAR(255))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = tableName)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND column_name = columnName) THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDef,
                         ' COMMENT ''', columnComment, '''');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- wms_receipt_order_detail 新增 3 个批次/效期字段
CALL p_wms_fefo_add_columns('wms_receipt_order_detail', 'batch_no', 'VARCHAR(64) NULL', '批次号（FEFO 出库 + 上架 Slotting 用）');
CALL p_wms_fefo_add_columns('wms_receipt_order_detail', 'production_date', 'DATE NULL', '生产日期');
CALL p_wms_fefo_add_columns('wms_receipt_order_detail', 'expiry_date', 'DATE NULL', '过期日期（空表示无保质期管理）');

-- 为批次号添加索引（按批次号查询收货明细）
DROP PROCEDURE IF EXISTS p_wms_fefo_add_index;
DELIMITER $$
CREATE PROCEDURE p_wms_fefo_add_index(IN tableName VARCHAR(128), IN indexName VARCHAR(128), IN columns TEXT)
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = tableName)
       AND NOT EXISTS (SELECT 1 FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND index_name = indexName) THEN
        SET @sql = CONCAT('CREATE INDEX `', indexName, '` ON `', tableName, '` (', columns, ')');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL p_wms_fefo_add_index('wms_receipt_order_detail', 'idx_receipt_detail_batch_no', '`batch_no`');

DROP PROCEDURE IF EXISTS p_wms_fefo_add_columns;
DROP PROCEDURE IF EXISTS p_wms_fefo_add_index;

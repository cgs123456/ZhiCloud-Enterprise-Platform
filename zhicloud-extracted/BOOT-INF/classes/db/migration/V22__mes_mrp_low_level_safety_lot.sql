-- ============================================================
-- V22: MES MRP 低层码 + 安全库存 + 批量规则字段（P0-11）
--
-- 为 mes_md_item 添加：
--   low_level_code   INT           低层码
--   safety_stock     DECIMAL(20,4) 安全库存
--   lot_size_rule    VARCHAR(32)   批量规则（LFL/FOQ/POQ/MULTIPLES）
--   fixed_lot_size   DECIMAL(20,4) 固定批量
--   lot_size_multiple DECIMAL(20,4) 批量倍数
--   lead_time_days   INT           提前期（天）
--   scrap_rate       DECIMAL(10,4) 损耗率（百分比）
--
-- 为 mes_pro_mrp_result 添加：
--   safety_stock     DECIMAL(20,4) 安全库存
--   lot_size_rule    VARCHAR(32)   批量规则
-- ============================================================

DROP PROCEDURE IF EXISTS p_mes_mrp_add_columns;
DELIMITER $$
CREATE PROCEDURE p_mes_mrp_add_columns(IN tableName VARCHAR(128), IN columnName VARCHAR(64),
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

-- mes_md_item 7 个新字段
CALL p_mes_mrp_add_columns('mes_md_item', 'low_level_code', 'INT NULL', '低层码 Low Level Code（0=顶层，越大越深）');
CALL p_mes_mrp_add_columns('mes_md_item', 'safety_stock', 'DECIMAL(20,4) NULL DEFAULT 0.0000', '安全库存量');
CALL p_mes_mrp_add_columns('mes_md_item', 'lot_size_rule', 'VARCHAR(32) NULL DEFAULT ''LFL''', '批量规则 LFL/FOQ/POQ/MULTIPLES');
CALL p_mes_mrp_add_columns('mes_md_item', 'fixed_lot_size', 'DECIMAL(20,4) NULL', '固定批量大小（FOQ 用）');
CALL p_mes_mrp_add_columns('mes_md_item', 'lot_size_multiple', 'DECIMAL(20,4) NULL', '批量倍数（MULTIPLES 用）');
CALL p_mes_mrp_add_columns('mes_md_item', 'lead_time_days', 'INT NULL DEFAULT 7', '采购/制造提前期（天）');
CALL p_mes_mrp_add_columns('mes_md_item', 'scrap_rate', 'DECIMAL(10,4) NULL DEFAULT 0.0000', '损耗率（百分比 0-100）');

-- mes_pro_mrp_result 2 个新字段
CALL p_mes_mrp_add_columns('mes_pro_mrp_result', 'safety_stock', 'DECIMAL(20,4) NULL DEFAULT 0.0000', '安全库存');
CALL p_mes_mrp_add_columns('mes_pro_mrp_result', 'lot_size_rule', 'VARCHAR(32) NULL DEFAULT ''LFL''', '批量规则');

DROP PROCEDURE IF EXISTS p_mes_mrp_add_columns;

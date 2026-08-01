-- ============================================================
-- V20: ERP 采购/销售单据补充多币种字段（P0-8）
--
-- 为 6 张业务单据表添加 3 个字段：
--   currency_id              BIGINT       币种编号
--   exchange_rate            DECIMAL(20,8) 汇率（外币 → 本位币）
--   base_currency_total_price DECIMAL(20,4) 本位币折算总金额
--
-- 涉及表：
--   erp_purchase_order / erp_purchase_in / erp_purchase_return
--   erp_sale_order / erp_sale_out / erp_sale_return
-- ============================================================

-- 存储过程：当表存在且列不存在时添加列
DROP PROCEDURE IF EXISTS p_erp_add_currency_columns;
DELIMITER $$
CREATE PROCEDURE p_erp_add_currency_columns(IN tableName VARCHAR(128))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = tableName) THEN
        -- currency_id
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND column_name = 'currency_id') THEN
            SET @sql = CONCAT('ALTER TABLE `', tableName,
                '` ADD COLUMN `currency_id` BIGINT NULL COMMENT ''币种编号（关联 erp_currency.id，空则使用本位币）''');
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;
        -- exchange_rate
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND column_name = 'exchange_rate') THEN
            SET @sql = CONCAT('ALTER TABLE `', tableName,
                '` ADD COLUMN `exchange_rate` DECIMAL(20,8) NULL DEFAULT 1.00000000 COMMENT ''汇率（外币 → 本位币，冗余当时汇率）''');
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;
        -- base_currency_total_price
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND column_name = 'base_currency_total_price') THEN
            SET @sql = CONCAT('ALTER TABLE `', tableName,
                '` ADD COLUMN `base_currency_total_price` DECIMAL(20,4) NULL DEFAULT 0.0000 COMMENT ''按本位币折算后的总金额''');
            PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END$$
DELIMITER ;

-- 采购侧 3 张表
CALL p_erp_add_currency_columns('erp_purchase_order');
CALL p_erp_add_currency_columns('erp_purchase_in');
CALL p_erp_add_currency_columns('erp_purchase_return');

-- 销售侧 3 张表
CALL p_erp_add_currency_columns('erp_sale_order');
CALL p_erp_add_currency_columns('erp_sale_out');
CALL p_erp_add_currency_columns('erp_sale_return');

-- 清理
DROP PROCEDURE IF EXISTS p_erp_add_currency_columns;

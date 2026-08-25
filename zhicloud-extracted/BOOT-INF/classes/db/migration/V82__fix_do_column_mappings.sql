-- ============================================================
-- V80: 修复 DO 实体字段与 DDL 列名不匹配（运行时 Unknown column 根因）
--
-- 背景：Java DO（业务契约，字段如 orderId/count/productPrice）与 V10 等
-- 早期迁移脚本的列名（master_id/purchase_count/purchase_price）系统性漂移，
-- MyBatis-Plus 按 camelCase→snake_case 推导列名，导致所有 CRUD 抛
-- SQLException: Unknown column。
--
-- 策略：以 Java DO 为真相源，对齐数据库列：
--   1) 语义重命名：新增语义化列并从旧列迁移数据（旧列保留不删，兼容存量读法）
--   2) 真缺失列：幂等新增
-- 全部通过存储过程 + information_schema 校验实现幂等，可安全重复执行。
-- 生成：scripts/gen_v80.py（2026-08-23）
-- ============================================================

DROP PROCEDURE IF EXISTS p_v80_add_column;
DELIMITER $$
CREATE PROCEDURE p_v80_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
        SET @v80_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE v80_stmt FROM @v80_sql;
        EXECUTE v80_stmt;
        DEALLOCATE PREPARE v80_stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 1. 语义重命名列（新增列 + 从旧列复制数据）
-- ============================================================

-- erp_account
CALL p_v80_add_column('erp_account', 'no', 'VARCHAR(255) DEFAULT NULL COMMENT ''no（原列 code）''');
CALL p_v80_add_column('erp_account', 'default_status', 'TINYINT(1) DEFAULT NULL COMMENT ''default_status（原列 default_flag）''');

-- erp_customer
CALL p_v80_add_column('erp_customer', 'telephone', 'VARCHAR(255) DEFAULT NULL COMMENT ''telephone（原列 phone）''');

-- erp_finance_payment
CALL p_v80_add_column('erp_finance_payment', 'payment_time', 'DATETIME DEFAULT NULL COMMENT ''payment_time（原列 finance_time）''');

-- erp_finance_payment_item
CALL p_v80_add_column('erp_finance_payment_item', 'payment_id', 'BIGINT DEFAULT NULL COMMENT ''payment_id（原列 master_id）''');
CALL p_v80_add_column('erp_finance_payment_item', 'biz_type', 'INT DEFAULT NULL COMMENT ''biz_type（原列 bill_type）''');
CALL p_v80_add_column('erp_finance_payment_item', 'biz_id', 'BIGINT DEFAULT NULL COMMENT ''biz_id（原列 bill_id）''');

-- erp_finance_receipt
CALL p_v80_add_column('erp_finance_receipt', 'receipt_time', 'DATETIME DEFAULT NULL COMMENT ''receipt_time（原列 finance_time）''');

-- erp_finance_receipt_item
CALL p_v80_add_column('erp_finance_receipt_item', 'receipt_id', 'BIGINT DEFAULT NULL COMMENT ''receipt_id（原列 master_id）''');
CALL p_v80_add_column('erp_finance_receipt_item', 'biz_type', 'INT DEFAULT NULL COMMENT ''biz_type（原列 bill_type）''');
CALL p_v80_add_column('erp_finance_receipt_item', 'biz_id', 'BIGINT DEFAULT NULL COMMENT ''biz_id（原列 bill_id）''');

-- erp_product
CALL p_v80_add_column('erp_product', 'bar_code', 'VARCHAR(255) DEFAULT NULL COMMENT ''bar_code（原列 barcode）''');
CALL p_v80_add_column('erp_product', 'standard', 'VARCHAR(255) DEFAULT NULL COMMENT ''standard（原列 spec）''');

-- erp_purchase_in_items
CALL p_v80_add_column('erp_purchase_in_items', 'in_id', 'BIGINT DEFAULT NULL COMMENT ''in_id（原列 master_id）''');
CALL p_v80_add_column('erp_purchase_in_items', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price（原列 purchase_price）''');
CALL p_v80_add_column('erp_purchase_in_items', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 purchase_count）''');

-- erp_purchase_order_items
CALL p_v80_add_column('erp_purchase_order_items', 'order_id', 'BIGINT DEFAULT NULL COMMENT ''order_id（原列 master_id）''');
CALL p_v80_add_column('erp_purchase_order_items', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price（原列 purchase_price）''');
CALL p_v80_add_column('erp_purchase_order_items', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 purchase_count）''');

-- erp_purchase_return_items
CALL p_v80_add_column('erp_purchase_return_items', 'return_id', 'BIGINT DEFAULT NULL COMMENT ''return_id（原列 master_id）''');
CALL p_v80_add_column('erp_purchase_return_items', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price（原列 purchase_price）''');
CALL p_v80_add_column('erp_purchase_return_items', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 purchase_count）''');

-- erp_sale_order_items
CALL p_v80_add_column('erp_sale_order_items', 'order_id', 'BIGINT DEFAULT NULL COMMENT ''order_id（原列 master_id）''');
CALL p_v80_add_column('erp_sale_order_items', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price（原列 sale_price）''');
CALL p_v80_add_column('erp_sale_order_items', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 sale_count）''');

-- erp_sale_out_items
CALL p_v80_add_column('erp_sale_out_items', 'out_id', 'BIGINT DEFAULT NULL COMMENT ''out_id（原列 master_id）''');
CALL p_v80_add_column('erp_sale_out_items', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price（原列 sale_price）''');
CALL p_v80_add_column('erp_sale_out_items', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 sale_count）''');

-- erp_sale_return_items
CALL p_v80_add_column('erp_sale_return_items', 'return_id', 'BIGINT DEFAULT NULL COMMENT ''return_id（原列 master_id）''');
CALL p_v80_add_column('erp_sale_return_items', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price（原列 sale_price）''');
CALL p_v80_add_column('erp_sale_return_items', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 sale_count）''');

-- erp_stock_check_item
CALL p_v80_add_column('erp_stock_check_item', 'check_id', 'BIGINT DEFAULT NULL COMMENT ''check_id（原列 master_id）''');
CALL p_v80_add_column('erp_stock_check_item', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count（原列 diff_count）''');

-- erp_stock_in_item
CALL p_v80_add_column('erp_stock_in_item', 'in_id', 'BIGINT DEFAULT NULL COMMENT ''in_id（原列 master_id）''');

-- erp_stock_move_item
CALL p_v80_add_column('erp_stock_move_item', 'move_id', 'BIGINT DEFAULT NULL COMMENT ''move_id（原列 master_id）''');

-- erp_stock_out_item
CALL p_v80_add_column('erp_stock_out_item', 'out_id', 'BIGINT DEFAULT NULL COMMENT ''out_id（原列 master_id）''');

-- erp_stock_record
CALL p_v80_add_column('erp_stock_record', 'biz_id', 'BIGINT DEFAULT NULL COMMENT ''biz_id（原列 bill_id）''');
CALL p_v80_add_column('erp_stock_record', 'biz_item_id', 'BIGINT DEFAULT NULL COMMENT ''biz_item_id（原列 bill_item_id）''');

-- erp_supplier
CALL p_v80_add_column('erp_supplier', 'telephone', 'VARCHAR(255) DEFAULT NULL COMMENT ''telephone（原列 phone）''');

-- 数据迁移：旧列值复制到新列（仅新列为空时）
UPDATE `erp_account` SET `no` = `code` WHERE `no` IS NULL AND `code` IS NOT NULL;
UPDATE `erp_account` SET `default_status` = `default_flag` WHERE `default_status` IS NULL AND `default_flag` IS NOT NULL;
UPDATE `erp_customer` SET `telephone` = `phone` WHERE `telephone` IS NULL AND `phone` IS NOT NULL;
UPDATE `erp_finance_payment` SET `payment_time` = `finance_time` WHERE `payment_time` IS NULL AND `finance_time` IS NOT NULL;
UPDATE `erp_finance_payment_item` SET `payment_id` = `master_id` WHERE `payment_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_finance_payment_item` SET `biz_type` = `bill_type` WHERE `biz_type` IS NULL AND `bill_type` IS NOT NULL;
UPDATE `erp_finance_payment_item` SET `biz_id` = `bill_id` WHERE `biz_id` IS NULL AND `bill_id` IS NOT NULL;
UPDATE `erp_finance_receipt` SET `receipt_time` = `finance_time` WHERE `receipt_time` IS NULL AND `finance_time` IS NOT NULL;
UPDATE `erp_finance_receipt_item` SET `receipt_id` = `master_id` WHERE `receipt_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_finance_receipt_item` SET `biz_type` = `bill_type` WHERE `biz_type` IS NULL AND `bill_type` IS NOT NULL;
UPDATE `erp_finance_receipt_item` SET `biz_id` = `bill_id` WHERE `biz_id` IS NULL AND `bill_id` IS NOT NULL;
UPDATE `erp_product` SET `bar_code` = `barcode` WHERE `bar_code` IS NULL AND `barcode` IS NOT NULL;
UPDATE `erp_product` SET `standard` = `spec` WHERE `standard` IS NULL AND `spec` IS NOT NULL;
UPDATE `erp_purchase_in_items` SET `in_id` = `master_id` WHERE `in_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_purchase_in_items` SET `product_price` = `purchase_price` WHERE `product_price` IS NULL AND `purchase_price` IS NOT NULL;
UPDATE `erp_purchase_in_items` SET `count` = `purchase_count` WHERE `count` IS NULL AND `purchase_count` IS NOT NULL;
UPDATE `erp_purchase_order_items` SET `order_id` = `master_id` WHERE `order_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_purchase_order_items` SET `product_price` = `purchase_price` WHERE `product_price` IS NULL AND `purchase_price` IS NOT NULL;
UPDATE `erp_purchase_order_items` SET `count` = `purchase_count` WHERE `count` IS NULL AND `purchase_count` IS NOT NULL;
UPDATE `erp_purchase_return_items` SET `return_id` = `master_id` WHERE `return_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_purchase_return_items` SET `product_price` = `purchase_price` WHERE `product_price` IS NULL AND `purchase_price` IS NOT NULL;
UPDATE `erp_purchase_return_items` SET `count` = `purchase_count` WHERE `count` IS NULL AND `purchase_count` IS NOT NULL;
UPDATE `erp_sale_order_items` SET `order_id` = `master_id` WHERE `order_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_sale_order_items` SET `product_price` = `sale_price` WHERE `product_price` IS NULL AND `sale_price` IS NOT NULL;
UPDATE `erp_sale_order_items` SET `count` = `sale_count` WHERE `count` IS NULL AND `sale_count` IS NOT NULL;
UPDATE `erp_sale_out_items` SET `out_id` = `master_id` WHERE `out_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_sale_out_items` SET `product_price` = `sale_price` WHERE `product_price` IS NULL AND `sale_price` IS NOT NULL;
UPDATE `erp_sale_out_items` SET `count` = `sale_count` WHERE `count` IS NULL AND `sale_count` IS NOT NULL;
UPDATE `erp_sale_return_items` SET `return_id` = `master_id` WHERE `return_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_sale_return_items` SET `product_price` = `sale_price` WHERE `product_price` IS NULL AND `sale_price` IS NOT NULL;
UPDATE `erp_sale_return_items` SET `count` = `sale_count` WHERE `count` IS NULL AND `sale_count` IS NOT NULL;
UPDATE `erp_stock_check_item` SET `check_id` = `master_id` WHERE `check_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_stock_check_item` SET `count` = `diff_count` WHERE `count` IS NULL AND `diff_count` IS NOT NULL;
UPDATE `erp_stock_in_item` SET `in_id` = `master_id` WHERE `in_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_stock_move_item` SET `move_id` = `master_id` WHERE `move_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_stock_out_item` SET `out_id` = `master_id` WHERE `out_id` IS NULL AND `master_id` IS NOT NULL;
UPDATE `erp_stock_record` SET `biz_id` = `bill_id` WHERE `biz_id` IS NULL AND `bill_id` IS NOT NULL;
UPDATE `erp_stock_record` SET `biz_item_id` = `bill_item_id` WHERE `biz_item_id` IS NULL AND `bill_item_id` IS NOT NULL;
UPDATE `erp_supplier` SET `telephone` = `phone` WHERE `telephone` IS NULL AND `phone` IS NOT NULL;

-- ============================================================
-- 2. 真缺失列（幂等新增）
-- ============================================================

-- crm_business
CALL p_v80_add_column('crm_business', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_business', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT ''name''');
CALL p_v80_add_column('crm_business', 'customer_id', 'BIGINT DEFAULT NULL COMMENT ''customer_id''');
CALL p_v80_add_column('crm_business', 'follow_up_status', 'TINYINT(1) DEFAULT NULL COMMENT ''follow_up_status''');
CALL p_v80_add_column('crm_business', 'contact_last_time', 'DATETIME DEFAULT NULL COMMENT ''contact_last_time''');
CALL p_v80_add_column('crm_business', 'contact_next_time', 'DATETIME DEFAULT NULL COMMENT ''contact_next_time''');
CALL p_v80_add_column('crm_business', 'owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''owner_user_id''');
CALL p_v80_add_column('crm_business', 'status_type_id', 'BIGINT DEFAULT NULL COMMENT ''status_type_id''');
CALL p_v80_add_column('crm_business', 'status_id', 'BIGINT DEFAULT NULL COMMENT ''status_id''');
CALL p_v80_add_column('crm_business', 'end_status', 'INT DEFAULT NULL COMMENT ''end_status''');
CALL p_v80_add_column('crm_business', 'end_remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''end_remark''');
CALL p_v80_add_column('crm_business', 'deal_time', 'DATETIME DEFAULT NULL COMMENT ''deal_time''');
CALL p_v80_add_column('crm_business', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_product_price''');
CALL p_v80_add_column('crm_business', 'discount_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''discount_percent''');
CALL p_v80_add_column('crm_business', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('crm_business', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');

-- crm_business_product
CALL p_v80_add_column('crm_business_product', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_business_product', 'business_id', 'BIGINT DEFAULT NULL COMMENT ''business_id''');
CALL p_v80_add_column('crm_business_product', 'product_id', 'BIGINT DEFAULT NULL COMMENT ''product_id''');
CALL p_v80_add_column('crm_business_product', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price''');
CALL p_v80_add_column('crm_business_product', 'business_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''business_price''');
CALL p_v80_add_column('crm_business_product', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count''');
CALL p_v80_add_column('crm_business_product', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');

-- crm_business_status
CALL p_v80_add_column('crm_business_status', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_business_status', 'type_id', 'BIGINT DEFAULT NULL COMMENT ''type_id''');
CALL p_v80_add_column('crm_business_status', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT ''name''');
CALL p_v80_add_column('crm_business_status', 'percent', 'INT DEFAULT NULL COMMENT ''percent''');
CALL p_v80_add_column('crm_business_status', 'sort', 'INT DEFAULT NULL COMMENT ''sort''');

-- crm_clue
CALL p_v80_add_column('crm_clue', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_clue', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT ''name''');
CALL p_v80_add_column('crm_clue', 'follow_up_status', 'TINYINT(1) DEFAULT NULL COMMENT ''follow_up_status''');
CALL p_v80_add_column('crm_clue', 'contact_last_time', 'DATETIME DEFAULT NULL COMMENT ''contact_last_time''');
CALL p_v80_add_column('crm_clue', 'contact_last_content', 'VARCHAR(255) DEFAULT NULL COMMENT ''contact_last_content''');
CALL p_v80_add_column('crm_clue', 'contact_next_time', 'DATETIME DEFAULT NULL COMMENT ''contact_next_time''');
CALL p_v80_add_column('crm_clue', 'owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''owner_user_id''');
CALL p_v80_add_column('crm_clue', 'transform_status', 'TINYINT(1) DEFAULT NULL COMMENT ''transform_status''');
CALL p_v80_add_column('crm_clue', 'customer_id', 'BIGINT DEFAULT NULL COMMENT ''customer_id''');
CALL p_v80_add_column('crm_clue', 'mobile', 'VARCHAR(255) DEFAULT NULL COMMENT ''mobile''');
CALL p_v80_add_column('crm_clue', 'telephone', 'VARCHAR(255) DEFAULT NULL COMMENT ''telephone''');
CALL p_v80_add_column('crm_clue', 'qq', 'VARCHAR(255) DEFAULT NULL COMMENT ''qq''');
CALL p_v80_add_column('crm_clue', 'wechat', 'VARCHAR(255) DEFAULT NULL COMMENT ''wechat''');
CALL p_v80_add_column('crm_clue', 'email', 'VARCHAR(255) DEFAULT NULL COMMENT ''email''');
CALL p_v80_add_column('crm_clue', 'area_id', 'INT DEFAULT NULL COMMENT ''area_id''');
CALL p_v80_add_column('crm_clue', 'detail_address', 'VARCHAR(255) DEFAULT NULL COMMENT ''detail_address''');
CALL p_v80_add_column('crm_clue', 'industry_id', 'INT DEFAULT NULL COMMENT ''industry_id''');
CALL p_v80_add_column('crm_clue', 'level', 'INT DEFAULT NULL COMMENT ''level''');
CALL p_v80_add_column('crm_clue', 'source', 'INT DEFAULT NULL COMMENT ''source''');
CALL p_v80_add_column('crm_clue', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');

-- crm_contact
CALL p_v80_add_column('crm_contact', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_contact', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT ''name''');
CALL p_v80_add_column('crm_contact', 'customer_id', 'BIGINT DEFAULT NULL COMMENT ''customer_id''');
CALL p_v80_add_column('crm_contact', 'contact_last_time', 'DATETIME DEFAULT NULL COMMENT ''contact_last_time''');
CALL p_v80_add_column('crm_contact', 'contact_last_content', 'VARCHAR(255) DEFAULT NULL COMMENT ''contact_last_content''');
CALL p_v80_add_column('crm_contact', 'contact_next_time', 'DATETIME DEFAULT NULL COMMENT ''contact_next_time''');
CALL p_v80_add_column('crm_contact', 'owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''owner_user_id''');
CALL p_v80_add_column('crm_contact', 'mobile', 'VARCHAR(255) DEFAULT NULL COMMENT ''mobile''');
CALL p_v80_add_column('crm_contact', 'telephone', 'VARCHAR(255) DEFAULT NULL COMMENT ''telephone''');
CALL p_v80_add_column('crm_contact', 'email', 'VARCHAR(255) DEFAULT NULL COMMENT ''email''');
CALL p_v80_add_column('crm_contact', 'qq', 'BIGINT DEFAULT NULL COMMENT ''qq''');
CALL p_v80_add_column('crm_contact', 'wechat', 'VARCHAR(255) DEFAULT NULL COMMENT ''wechat''');
CALL p_v80_add_column('crm_contact', 'area_id', 'INT DEFAULT NULL COMMENT ''area_id''');
CALL p_v80_add_column('crm_contact', 'detail_address', 'VARCHAR(255) DEFAULT NULL COMMENT ''detail_address''');
CALL p_v80_add_column('crm_contact', 'sex', 'INT DEFAULT NULL COMMENT ''sex''');
CALL p_v80_add_column('crm_contact', 'master', 'TINYINT(1) DEFAULT NULL COMMENT ''master''');
CALL p_v80_add_column('crm_contact', 'post', 'VARCHAR(255) DEFAULT NULL COMMENT ''post''');
CALL p_v80_add_column('crm_contact', 'parent_id', 'BIGINT DEFAULT NULL COMMENT ''parent_id''');
CALL p_v80_add_column('crm_contact', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');

-- crm_contact_business
CALL p_v80_add_column('crm_contact_business', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_contact_business', 'contact_id', 'BIGINT DEFAULT NULL COMMENT ''contact_id''');
CALL p_v80_add_column('crm_contact_business', 'business_id', 'BIGINT DEFAULT NULL COMMENT ''business_id''');

-- crm_contract_config
CALL p_v80_add_column('crm_contract_config', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');

-- crm_contract_product
CALL p_v80_add_column('crm_contract_product', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_contract_product', 'contract_id', 'BIGINT DEFAULT NULL COMMENT ''contract_id''');
CALL p_v80_add_column('crm_contract_product', 'product_id', 'BIGINT DEFAULT NULL COMMENT ''product_id''');
CALL p_v80_add_column('crm_contract_product', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price''');
CALL p_v80_add_column('crm_contract_product', 'contract_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''contract_price''');
CALL p_v80_add_column('crm_contract_product', 'count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''count''');
CALL p_v80_add_column('crm_contract_product', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');

-- crm_owner_record
CALL p_v80_add_column('crm_owner_record', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_owner_record', 'biz_type', 'INT DEFAULT NULL COMMENT ''biz_type''');
CALL p_v80_add_column('crm_owner_record', 'biz_id', 'BIGINT DEFAULT NULL COMMENT ''biz_id''');
CALL p_v80_add_column('crm_owner_record', 'pre_owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''pre_owner_user_id''');
CALL p_v80_add_column('crm_owner_record', 'post_owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''post_owner_user_id''');

-- crm_performance_config
CALL p_v80_add_column('crm_performance_config', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_performance_config', 'biz_type', 'INT DEFAULT NULL COMMENT ''biz_type''');
CALL p_v80_add_column('crm_performance_config', 'object_id', 'BIGINT DEFAULT NULL COMMENT ''object_id''');
CALL p_v80_add_column('crm_performance_config', 'object_type', 'INT DEFAULT NULL COMMENT ''object_type''');
CALL p_v80_add_column('crm_performance_config', 'year', 'INT DEFAULT NULL COMMENT ''year''');
CALL p_v80_add_column('crm_performance_config', 'year_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''year_target_price''');
CALL p_v80_add_column('crm_performance_config', 'january_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''january_target_price''');
CALL p_v80_add_column('crm_performance_config', 'february_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''february_target_price''');
CALL p_v80_add_column('crm_performance_config', 'march_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''march_target_price''');
CALL p_v80_add_column('crm_performance_config', 'april_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''april_target_price''');
CALL p_v80_add_column('crm_performance_config', 'may_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''may_target_price''');
CALL p_v80_add_column('crm_performance_config', 'june_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''june_target_price''');
CALL p_v80_add_column('crm_performance_config', 'july_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''july_target_price''');
CALL p_v80_add_column('crm_performance_config', 'august_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''august_target_price''');
CALL p_v80_add_column('crm_performance_config', 'september_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''september_target_price''');
CALL p_v80_add_column('crm_performance_config', 'october_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''october_target_price''');
CALL p_v80_add_column('crm_performance_config', 'november_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''november_target_price''');
CALL p_v80_add_column('crm_performance_config', 'december_target_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''december_target_price''');

-- crm_permission
CALL p_v80_add_column('crm_permission', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_permission', 'biz_type', 'INT DEFAULT NULL COMMENT ''biz_type''');
CALL p_v80_add_column('crm_permission', 'biz_id', 'BIGINT DEFAULT NULL COMMENT ''biz_id''');
CALL p_v80_add_column('crm_permission', 'user_id', 'BIGINT DEFAULT NULL COMMENT ''user_id''');
CALL p_v80_add_column('crm_permission', 'level', 'INT DEFAULT NULL COMMENT ''level''');

-- crm_product
CALL p_v80_add_column('crm_product', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_product', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT ''name''');
CALL p_v80_add_column('crm_product', 'no', 'VARCHAR(255) DEFAULT NULL COMMENT ''no''');
CALL p_v80_add_column('crm_product', 'unit', 'INT DEFAULT NULL COMMENT ''unit''');
CALL p_v80_add_column('crm_product', 'price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''price''');
CALL p_v80_add_column('crm_product', 'status', 'INT DEFAULT NULL COMMENT ''status''');
CALL p_v80_add_column('crm_product', 'category_id', 'BIGINT DEFAULT NULL COMMENT ''category_id''');
CALL p_v80_add_column('crm_product', 'description', 'VARCHAR(255) DEFAULT NULL COMMENT ''description''');
CALL p_v80_add_column('crm_product', 'owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''owner_user_id''');

-- crm_product_category
CALL p_v80_add_column('crm_product_category', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_product_category', 'name', 'VARCHAR(255) DEFAULT NULL COMMENT ''name''');
CALL p_v80_add_column('crm_product_category', 'parent_id', 'BIGINT DEFAULT NULL COMMENT ''parent_id''');

-- crm_receivable
CALL p_v80_add_column('crm_receivable', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_receivable', 'no', 'VARCHAR(255) DEFAULT NULL COMMENT ''no''');
CALL p_v80_add_column('crm_receivable', 'plan_id', 'BIGINT DEFAULT NULL COMMENT ''plan_id''');
CALL p_v80_add_column('crm_receivable', 'customer_id', 'BIGINT DEFAULT NULL COMMENT ''customer_id''');
CALL p_v80_add_column('crm_receivable', 'contract_id', 'BIGINT DEFAULT NULL COMMENT ''contract_id''');
CALL p_v80_add_column('crm_receivable', 'owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''owner_user_id''');
CALL p_v80_add_column('crm_receivable', 'return_time', 'DATETIME DEFAULT NULL COMMENT ''return_time''');
CALL p_v80_add_column('crm_receivable', 'return_type', 'INT DEFAULT NULL COMMENT ''return_type''');
CALL p_v80_add_column('crm_receivable', 'price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''price''');
CALL p_v80_add_column('crm_receivable', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');
CALL p_v80_add_column('crm_receivable', 'process_instance_id', 'VARCHAR(255) DEFAULT NULL COMMENT ''process_instance_id''');
CALL p_v80_add_column('crm_receivable', 'audit_status', 'INT DEFAULT NULL COMMENT ''audit_status''');

-- crm_receivable_plan
CALL p_v80_add_column('crm_receivable_plan', 'id', 'BIGINT DEFAULT NULL COMMENT ''id''');
CALL p_v80_add_column('crm_receivable_plan', 'period', 'INT DEFAULT NULL COMMENT ''period''');
CALL p_v80_add_column('crm_receivable_plan', 'customer_id', 'BIGINT DEFAULT NULL COMMENT ''customer_id''');
CALL p_v80_add_column('crm_receivable_plan', 'contract_id', 'BIGINT DEFAULT NULL COMMENT ''contract_id''');
CALL p_v80_add_column('crm_receivable_plan', 'owner_user_id', 'BIGINT DEFAULT NULL COMMENT ''owner_user_id''');
CALL p_v80_add_column('crm_receivable_plan', 'return_time', 'DATETIME DEFAULT NULL COMMENT ''return_time''');
CALL p_v80_add_column('crm_receivable_plan', 'return_type', 'INT DEFAULT NULL COMMENT ''return_type''');
CALL p_v80_add_column('crm_receivable_plan', 'price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''price''');
CALL p_v80_add_column('crm_receivable_plan', 'receivable_id', 'BIGINT DEFAULT NULL COMMENT ''receivable_id''');
CALL p_v80_add_column('crm_receivable_plan', 'remind_days', 'INT DEFAULT NULL COMMENT ''remind_days''');
CALL p_v80_add_column('crm_receivable_plan', 'remind_time', 'DATETIME DEFAULT NULL COMMENT ''remind_time''');
CALL p_v80_add_column('crm_receivable_plan', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');

-- erp_account
CALL p_v80_add_column('erp_account', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');
CALL p_v80_add_column('erp_account', 'sort', 'INT DEFAULT NULL COMMENT ''sort''');

-- erp_customer
CALL p_v80_add_column('erp_customer', 'fax', 'VARCHAR(255) DEFAULT NULL COMMENT ''fax''');
CALL p_v80_add_column('erp_customer', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');
CALL p_v80_add_column('erp_customer', 'sort', 'INT DEFAULT NULL COMMENT ''sort''');
CALL p_v80_add_column('erp_customer', 'tax_no', 'VARCHAR(255) DEFAULT NULL COMMENT ''tax_no''');
CALL p_v80_add_column('erp_customer', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_customer', 'bank_address', 'VARCHAR(255) DEFAULT NULL COMMENT ''bank_address''');

-- erp_finance_payment
CALL p_v80_add_column('erp_finance_payment', 'finance_user_id', 'BIGINT DEFAULT NULL COMMENT ''finance_user_id''');
CALL p_v80_add_column('erp_finance_payment', 'discount_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''discount_price''');

-- erp_finance_payment_item
CALL p_v80_add_column('erp_finance_payment_item', 'biz_no', 'VARCHAR(255) DEFAULT NULL COMMENT ''biz_no''');
CALL p_v80_add_column('erp_finance_payment_item', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('erp_finance_payment_item', 'paid_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''paid_price''');

-- erp_finance_receipt
CALL p_v80_add_column('erp_finance_receipt', 'finance_user_id', 'BIGINT DEFAULT NULL COMMENT ''finance_user_id''');
CALL p_v80_add_column('erp_finance_receipt', 'discount_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''discount_price''');
CALL p_v80_add_column('erp_finance_receipt', 'receipt_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''receipt_price''');

-- erp_finance_receipt_item
CALL p_v80_add_column('erp_finance_receipt_item', 'biz_no', 'VARCHAR(255) DEFAULT NULL COMMENT ''biz_no''');
CALL p_v80_add_column('erp_finance_receipt_item', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('erp_finance_receipt_item', 'receipted_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''receipted_price''');
CALL p_v80_add_column('erp_finance_receipt_item', 'receipt_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''receipt_price''');

-- erp_product
CALL p_v80_add_column('erp_product', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');
CALL p_v80_add_column('erp_product', 'expiry_day', 'INT DEFAULT NULL COMMENT ''expiry_day''');
CALL p_v80_add_column('erp_product', 'weight', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''weight''');
CALL p_v80_add_column('erp_product', 'min_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''min_price''');

-- erp_purchase_in_items
CALL p_v80_add_column('erp_purchase_in_items', 'order_item_id', 'BIGINT DEFAULT NULL COMMENT ''order_item_id''');
CALL p_v80_add_column('erp_purchase_in_items', 'warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''warehouse_id''');
CALL p_v80_add_column('erp_purchase_in_items', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_purchase_in_items', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_purchase_in_items', 'tax_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_price''');

-- erp_purchase_order_items
CALL p_v80_add_column('erp_purchase_order_items', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_purchase_order_items', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_purchase_order_items', 'tax_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_price''');
CALL p_v80_add_column('erp_purchase_order_items', 'in_count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''in_count''');
CALL p_v80_add_column('erp_purchase_order_items', 'return_count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''return_count''');

-- erp_purchase_return_items
CALL p_v80_add_column('erp_purchase_return_items', 'order_item_id', 'BIGINT DEFAULT NULL COMMENT ''order_item_id''');
CALL p_v80_add_column('erp_purchase_return_items', 'warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''warehouse_id''');
CALL p_v80_add_column('erp_purchase_return_items', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_purchase_return_items', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_purchase_return_items', 'tax_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_price''');

-- erp_sale_order_items
CALL p_v80_add_column('erp_sale_order_items', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_sale_order_items', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_sale_order_items', 'tax_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_price''');
CALL p_v80_add_column('erp_sale_order_items', 'out_count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''out_count''');
CALL p_v80_add_column('erp_sale_order_items', 'return_count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''return_count''');

-- erp_sale_out_items
CALL p_v80_add_column('erp_sale_out_items', 'order_item_id', 'BIGINT DEFAULT NULL COMMENT ''order_item_id''');
CALL p_v80_add_column('erp_sale_out_items', 'warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''warehouse_id''');
CALL p_v80_add_column('erp_sale_out_items', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_sale_out_items', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_sale_out_items', 'tax_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_price''');

-- erp_sale_return_items
CALL p_v80_add_column('erp_sale_return_items', 'order_item_id', 'BIGINT DEFAULT NULL COMMENT ''order_item_id''');
CALL p_v80_add_column('erp_sale_return_items', 'warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''warehouse_id''');
CALL p_v80_add_column('erp_sale_return_items', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_sale_return_items', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_sale_return_items', 'tax_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_price''');

-- erp_stock
CALL p_v80_add_column('erp_stock', 'locked_count', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''locked_count''');

-- erp_stock_check
CALL p_v80_add_column('erp_stock_check', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('erp_stock_check', 'file_url', 'VARCHAR(255) DEFAULT NULL COMMENT ''file_url''');

-- erp_stock_check_item
CALL p_v80_add_column('erp_stock_check_item', 'warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''warehouse_id''');
CALL p_v80_add_column('erp_stock_check_item', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_stock_check_item', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price''');
CALL p_v80_add_column('erp_stock_check_item', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');

-- erp_stock_in
CALL p_v80_add_column('erp_stock_in', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('erp_stock_in', 'file_url', 'VARCHAR(255) DEFAULT NULL COMMENT ''file_url''');

-- erp_stock_in_item
CALL p_v80_add_column('erp_stock_in_item', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_stock_in_item', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price''');
CALL p_v80_add_column('erp_stock_in_item', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');

-- erp_stock_move
CALL p_v80_add_column('erp_stock_move', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('erp_stock_move', 'file_url', 'VARCHAR(255) DEFAULT NULL COMMENT ''file_url''');

-- erp_stock_move_item
CALL p_v80_add_column('erp_stock_move_item', 'from_warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''from_warehouse_id''');
CALL p_v80_add_column('erp_stock_move_item', 'to_warehouse_id', 'BIGINT DEFAULT NULL COMMENT ''to_warehouse_id''');
CALL p_v80_add_column('erp_stock_move_item', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_stock_move_item', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price''');
CALL p_v80_add_column('erp_stock_move_item', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');

-- erp_stock_out
CALL p_v80_add_column('erp_stock_out', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');
CALL p_v80_add_column('erp_stock_out', 'file_url', 'VARCHAR(255) DEFAULT NULL COMMENT ''file_url''');

-- erp_stock_out_item
CALL p_v80_add_column('erp_stock_out_item', 'product_unit_id', 'BIGINT DEFAULT NULL COMMENT ''product_unit_id''');
CALL p_v80_add_column('erp_stock_out_item', 'product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''product_price''');
CALL p_v80_add_column('erp_stock_out_item', 'total_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''total_price''');

-- erp_stock_record
CALL p_v80_add_column('erp_stock_record', 'biz_no', 'VARCHAR(255) DEFAULT NULL COMMENT ''biz_no''');

-- erp_supplier
CALL p_v80_add_column('erp_supplier', 'fax', 'VARCHAR(255) DEFAULT NULL COMMENT ''fax''');
CALL p_v80_add_column('erp_supplier', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');
CALL p_v80_add_column('erp_supplier', 'sort', 'INT DEFAULT NULL COMMENT ''sort''');
CALL p_v80_add_column('erp_supplier', 'tax_no', 'VARCHAR(255) DEFAULT NULL COMMENT ''tax_no''');
CALL p_v80_add_column('erp_supplier', 'tax_percent', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''tax_percent''');
CALL p_v80_add_column('erp_supplier', 'bank_address', 'VARCHAR(255) DEFAULT NULL COMMENT ''bank_address''');

-- erp_warehouse
CALL p_v80_add_column('erp_warehouse', 'remark', 'VARCHAR(255) DEFAULT NULL COMMENT ''remark''');
CALL p_v80_add_column('erp_warehouse', 'principal', 'VARCHAR(255) DEFAULT NULL COMMENT ''principal''');
CALL p_v80_add_column('erp_warehouse', 'warehouse_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''warehouse_price''');
CALL p_v80_add_column('erp_warehouse', 'truckage_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''truckage_price''');
CALL p_v80_add_column('erp_warehouse', 'default_status', 'TINYINT(1) DEFAULT NULL COMMENT ''default_status''');

-- qms_inspection_order
CALL p_v80_add_column('qms_inspection_order', 'reject_quantity', 'INT DEFAULT NULL COMMENT ''reject_quantity''');
CALL p_v80_add_column('qms_inspection_order', 'biz_type', 'VARCHAR(255) DEFAULT NULL COMMENT ''biz_type''');
CALL p_v80_add_column('qms_inspection_order', 'biz_id', 'BIGINT DEFAULT NULL COMMENT ''biz_id''');

-- wms_receipt_order
CALL p_v80_add_column('wms_receipt_order', 'qc_biz_type', 'VARCHAR(255) DEFAULT NULL COMMENT ''qc_biz_type''');

DROP PROCEDURE IF EXISTS p_v80_add_column;

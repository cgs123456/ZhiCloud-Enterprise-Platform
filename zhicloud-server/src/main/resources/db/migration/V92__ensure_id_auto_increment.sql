-- ============================================================
-- V92: 为 95 张缺失 AUTO_INCREMENT 的表补回自增主键
--
-- 背景：application.yaml 的 mybatis-plus.global-config.db-config.id-type=NONE
-- 在 MySQL 下会被 IdTypeEnvironmentPostProcessor 解析为 AUTO；MyBatis-Plus
-- 因而不向 INSERT 提供 id。若列没有 AUTO_INCREMENT，严格模式报 1364，
-- 非严格模式可能写 0 并在第二行撞主键。
--
-- 证据：docs/enterprise-platform/evidence/id_auto_increment_audit.json
-- 生成器：script/enterprise-mock-data/tools/analyze_id_autoincrement.py --sql-out
-- 盘点：共 95 张表；模块分布 {'aimultiagent': 6, 'airag': 3, 'erp': 41, 'hr': 6, 'qms': 20, 'wms': 19}
--
-- 限界：静态解析未连真库；执行前必须跑 V92_verify.sql 预检。
-- 说明：ALTER MODIFY id AUTO_INCREMENT 可能触发表重建，生产应在维护窗口
--       分批执行或使用 gh-ost/pt-online-schema-change；不要不看表大小直接全量跑。
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS p_v92_ensure_auto_increment$$
CREATE PROCEDURE p_v92_ensure_auto_increment(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_column_type VARCHAR(128) DEFAULT NULL;
    DECLARE v_column_key VARCHAR(8) DEFAULT NULL;
    DECLARE v_extra VARCHAR(128) DEFAULT NULL;
    DECLARE v_comment VARCHAR(1024) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;
    DECLARE v_sql LONGTEXT;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.tables
     WHERE table_schema = DATABASE() AND table_name = p_table;
    IF v_exists = 0 THEN
        SET v_msg = CONCAT('V92 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    SELECT COLUMN_TYPE, COLUMN_KEY, EXTRA, COLUMN_COMMENT
      INTO v_column_type, v_column_key, v_extra, v_comment
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = 'id';
    IF v_column_type IS NULL THEN
        SET v_msg = CONCAT('V92 id column missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
    IF IFNULL(v_column_key, '') <> 'PRI' THEN
        SET v_msg = CONCAT('V92 id is not primary key: ', p_table, ' key=', v_column_key);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    -- 已自增则幂等跳过
    IF IFNULL(v_extra, '') NOT LIKE '%auto_increment%' THEN
        SET v_sql = CONCAT(
            'ALTER TABLE `', p_table, '` MODIFY COLUMN `id` ', v_column_type,
            ' NOT NULL AUTO_INCREMENT',
            IF(v_comment IS NULL OR v_comment = '', '', CONCAT(' COMMENT ', QUOTE(v_comment)))
        );
        SET @v92_sql = v_sql;
        PREPARE v92_stmt FROM @v92_sql;
        EXECUTE v92_stmt;
        DEALLOCATE PREPARE v92_stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- 逐表调用（顺序无依赖）
-- ------------------------------------------------------------
CALL p_v92_ensure_auto_increment('aimultiagent_checkpoint');
CALL p_v92_ensure_auto_increment('aimultiagent_execution_log');
CALL p_v92_ensure_auto_increment('aimultiagent_execution_span');
CALL p_v92_ensure_auto_increment('aimultiagent_skill');
CALL p_v92_ensure_auto_increment('aimultiagent_skill_group');
CALL p_v92_ensure_auto_increment('aimultiagent_topology');
CALL p_v92_ensure_auto_increment('airag_eval_dataset');
CALL p_v92_ensure_auto_increment('airag_eval_question');
CALL p_v92_ensure_auto_increment('airag_eval_report');
CALL p_v92_ensure_auto_increment('erp_account');
CALL p_v92_ensure_auto_increment('erp_cost_allocation');
CALL p_v92_ensure_auto_increment('erp_cost_center');
CALL p_v92_ensure_auto_increment('erp_currency');
CALL p_v92_ensure_auto_increment('erp_customer');
CALL p_v92_ensure_auto_increment('erp_exchange_rate');
CALL p_v92_ensure_auto_increment('erp_finance_payment');
CALL p_v92_ensure_auto_increment('erp_finance_payment_item');
CALL p_v92_ensure_auto_increment('erp_finance_receipt');
CALL p_v92_ensure_auto_increment('erp_finance_receipt_item');
CALL p_v92_ensure_auto_increment('erp_product');
CALL p_v92_ensure_auto_increment('erp_product_category');
CALL p_v92_ensure_auto_increment('erp_product_unit');
CALL p_v92_ensure_auto_increment('erp_profit_center');
CALL p_v92_ensure_auto_increment('erp_profitability_analysis');
CALL p_v92_ensure_auto_increment('erp_purchase_in');
CALL p_v92_ensure_auto_increment('erp_purchase_in_items');
CALL p_v92_ensure_auto_increment('erp_purchase_order');
CALL p_v92_ensure_auto_increment('erp_purchase_order_items');
CALL p_v92_ensure_auto_increment('erp_purchase_return');
CALL p_v92_ensure_auto_increment('erp_purchase_return_items');
CALL p_v92_ensure_auto_increment('erp_sale_order');
CALL p_v92_ensure_auto_increment('erp_sale_order_items');
CALL p_v92_ensure_auto_increment('erp_sale_out');
CALL p_v92_ensure_auto_increment('erp_sale_out_items');
CALL p_v92_ensure_auto_increment('erp_sale_return');
CALL p_v92_ensure_auto_increment('erp_sale_return_items');
CALL p_v92_ensure_auto_increment('erp_stock');
CALL p_v92_ensure_auto_increment('erp_stock_batch');
CALL p_v92_ensure_auto_increment('erp_stock_check');
CALL p_v92_ensure_auto_increment('erp_stock_check_item');
CALL p_v92_ensure_auto_increment('erp_stock_in');
CALL p_v92_ensure_auto_increment('erp_stock_in_item');
CALL p_v92_ensure_auto_increment('erp_stock_move');
CALL p_v92_ensure_auto_increment('erp_stock_move_item');
CALL p_v92_ensure_auto_increment('erp_stock_out');
CALL p_v92_ensure_auto_increment('erp_stock_out_item');
CALL p_v92_ensure_auto_increment('erp_stock_record');
CALL p_v92_ensure_auto_increment('erp_stock_serial');
CALL p_v92_ensure_auto_increment('erp_supplier');
CALL p_v92_ensure_auto_increment('erp_warehouse');
CALL p_v92_ensure_auto_increment('hr_attendance');
CALL p_v92_ensure_auto_increment('hr_department');
CALL p_v92_ensure_auto_increment('hr_employee');
CALL p_v92_ensure_auto_increment('hr_performance');
CALL p_v92_ensure_auto_increment('hr_position');
CALL p_v92_ensure_auto_increment('hr_salary');
CALL p_v92_ensure_auto_increment('qms_capa_document');
CALL p_v92_ensure_auto_increment('qms_customer_complaint');
CALL p_v92_ensure_auto_increment('qms_eight_d_report');
CALL p_v92_ensure_auto_increment('qms_electronic_signature_log');
CALL p_v92_ensure_auto_increment('qms_fmea_document');
CALL p_v92_ensure_auto_increment('qms_fmea_item');
CALL p_v92_ensure_auto_increment('qms_inspection_item');
CALL p_v92_ensure_auto_increment('qms_inspection_order');
CALL p_v92_ensure_auto_increment('qms_inspection_record');
CALL p_v92_ensure_auto_increment('qms_msa_measurement');
CALL p_v92_ensure_auto_increment('qms_msa_study');
CALL p_v92_ensure_auto_increment('qms_ncr_document');
CALL p_v92_ensure_auto_increment('qms_ncr_mrb_record');
CALL p_v92_ensure_auto_increment('qms_qualification');
CALL p_v92_ensure_auto_increment('qms_quality_cost');
CALL p_v92_ensure_auto_increment('qms_scar');
CALL p_v92_ensure_auto_increment('qms_supplier_audit');
CALL p_v92_ensure_auto_increment('qms_supplier_rating');
CALL p_v92_ensure_auto_increment('qms_training_plan');
CALL p_v92_ensure_auto_increment('qms_training_record');
CALL p_v92_ensure_auto_increment('wms_check_order');
CALL p_v92_ensure_auto_increment('wms_check_order_detail');
CALL p_v92_ensure_auto_increment('wms_inventory');
CALL p_v92_ensure_auto_increment('wms_inventory_batch');
CALL p_v92_ensure_auto_increment('wms_inventory_history');
CALL p_v92_ensure_auto_increment('wms_item');
CALL p_v92_ensure_auto_increment('wms_item_brand');
CALL p_v92_ensure_auto_increment('wms_item_category');
CALL p_v92_ensure_auto_increment('wms_item_sku');
CALL p_v92_ensure_auto_increment('wms_merchant');
CALL p_v92_ensure_auto_increment('wms_movement_order');
CALL p_v92_ensure_auto_increment('wms_movement_order_detail');
CALL p_v92_ensure_auto_increment('wms_receipt_order');
CALL p_v92_ensure_auto_increment('wms_receipt_order_detail');
CALL p_v92_ensure_auto_increment('wms_shipment_order');
CALL p_v92_ensure_auto_increment('wms_shipment_order_detail');
CALL p_v92_ensure_auto_increment('wms_warehouse');
CALL p_v92_ensure_auto_increment('wms_wave_order');
CALL p_v92_ensure_auto_increment('wms_wave_order_detail');

-- ------------------------------------------------------------
-- 自检：95 张表必须全部有 auto_increment；失败则让迁移 Job 阻断发布
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v92_expected;
CREATE TEMPORARY TABLE tmp_v92_expected (table_name VARCHAR(64) PRIMARY KEY);
INSERT INTO tmp_v92_expected (table_name) VALUES
    ('aimultiagent_checkpoint'),
    ('aimultiagent_execution_log'),
    ('aimultiagent_execution_span'),
    ('aimultiagent_skill'),
    ('aimultiagent_skill_group'),
    ('aimultiagent_topology'),
    ('airag_eval_dataset'),
    ('airag_eval_question'),
    ('airag_eval_report'),
    ('erp_account'),
    ('erp_cost_allocation'),
    ('erp_cost_center'),
    ('erp_currency'),
    ('erp_customer'),
    ('erp_exchange_rate'),
    ('erp_finance_payment'),
    ('erp_finance_payment_item'),
    ('erp_finance_receipt'),
    ('erp_finance_receipt_item'),
    ('erp_product'),
    ('erp_product_category'),
    ('erp_product_unit'),
    ('erp_profit_center'),
    ('erp_profitability_analysis'),
    ('erp_purchase_in'),
    ('erp_purchase_in_items'),
    ('erp_purchase_order'),
    ('erp_purchase_order_items'),
    ('erp_purchase_return'),
    ('erp_purchase_return_items'),
    ('erp_sale_order'),
    ('erp_sale_order_items'),
    ('erp_sale_out'),
    ('erp_sale_out_items'),
    ('erp_sale_return'),
    ('erp_sale_return_items'),
    ('erp_stock'),
    ('erp_stock_batch'),
    ('erp_stock_check'),
    ('erp_stock_check_item'),
    ('erp_stock_in'),
    ('erp_stock_in_item'),
    ('erp_stock_move'),
    ('erp_stock_move_item'),
    ('erp_stock_out'),
    ('erp_stock_out_item'),
    ('erp_stock_record'),
    ('erp_stock_serial'),
    ('erp_supplier'),
    ('erp_warehouse'),
    ('hr_attendance'),
    ('hr_department'),
    ('hr_employee'),
    ('hr_performance'),
    ('hr_position'),
    ('hr_salary'),
    ('qms_capa_document'),
    ('qms_customer_complaint'),
    ('qms_eight_d_report'),
    ('qms_electronic_signature_log'),
    ('qms_fmea_document'),
    ('qms_fmea_item'),
    ('qms_inspection_item'),
    ('qms_inspection_order'),
    ('qms_inspection_record'),
    ('qms_msa_measurement'),
    ('qms_msa_study'),
    ('qms_ncr_document'),
    ('qms_ncr_mrb_record'),
    ('qms_qualification'),
    ('qms_quality_cost'),
    ('qms_scar'),
    ('qms_supplier_audit'),
    ('qms_supplier_rating'),
    ('qms_training_plan'),
    ('qms_training_record'),
    ('wms_check_order'),
    ('wms_check_order_detail'),
    ('wms_inventory'),
    ('wms_inventory_batch'),
    ('wms_inventory_history'),
    ('wms_item'),
    ('wms_item_brand'),
    ('wms_item_category'),
    ('wms_item_sku'),
    ('wms_merchant'),
    ('wms_movement_order'),
    ('wms_movement_order_detail'),
    ('wms_receipt_order'),
    ('wms_receipt_order_detail'),
    ('wms_shipment_order'),
    ('wms_shipment_order_detail'),
    ('wms_warehouse'),
    ('wms_wave_order'),
    ('wms_wave_order_detail');
DELIMITER $$

DROP PROCEDURE IF EXISTS p_v92_self_check$$
CREATE PROCEDURE p_v92_self_check()
BEGIN
    DECLARE v_expected INT DEFAULT 0;
    DECLARE v_bad INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_expected FROM tmp_v92_expected;
    SELECT COUNT(*) INTO v_bad
      FROM tmp_v92_expected e
      LEFT JOIN information_schema.tables t
        ON t.table_schema = DATABASE() AND t.table_name = e.table_name
      LEFT JOIN information_schema.columns c
        ON c.table_schema = DATABASE() AND c.table_name = e.table_name
       AND c.column_name = 'id'
     WHERE t.table_name IS NULL OR c.column_name IS NULL
        OR IFNULL(c.EXTRA, '') NOT LIKE '%auto_increment%';

    IF v_expected <> 95 OR v_bad <> 0 THEN
        SET v_msg = CONCAT('V92 self-check failed: expected=', v_expected, ' bad=', v_bad);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
    SELECT 'V92 OK: all 95 expected id columns are AUTO_INCREMENT' AS v92_result;
END$$
DELIMITER ;

CALL p_v92_self_check();
DROP PROCEDURE IF EXISTS p_v92_self_check;
DROP TEMPORARY TABLE IF EXISTS tmp_v92_expected;
DROP PROCEDURE IF EXISTS p_v92_ensure_auto_increment;

SELECT @@GLOBAL.sql_mode AS global_sql_mode, @@SESSION.sql_mode AS session_sql_mode;

-- 回滚：V92 只添加 AUTO_INCREMENT 属性，不改类型/列名/数据。
-- 若必须回滚，逐列按原定义 MODIFY COLUMN id <原类型> NOT NULL COMMENT '<原注释>'。
-- 不提供自动反向脚本：误移除自增会导致新写入失败，回滚应先停写。

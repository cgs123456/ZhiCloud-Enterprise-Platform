-- ============================================================
-- V92 人工复核脚本（只读，不改任何业务数据）
--
-- 背景：[静态推导，需真库复核] tools/analyze_id_autoincrement.py 发现 95 张表的
--   id 列在 CREATE TABLE 中缺 AUTO_INCREMENT；而 application.yaml 配置
--   mybatis-plus.global-config.db-config.id-type: NONE，
--   IdTypeEnvironmentPostProcessor 在 MySQL 上把它改写成 IdType.AUTO，
--   即 MyBatis-Plus 不会在 INSERT 里提供 id（例外：@TableId(INPUT) 的 MpTagDO）。
--   => 严格 sql_mode 下报 ERROR 1364（id 无默认值）；非严格模式下写 0，
--      第二行即撞主键。两条路都不通，但**必须先用本脚本在真库确认**。
--
-- 用法：
--   预检（V92 执行前）→ 看 P1/P2/P3/P4/P5；
--   后检（V92 执行后）→ 看 Q1/Q2/Q3。
-- 期望：
--   * P2：expected_tables=95；no_primary_key=0；missing_table_or_column=0；
--         already_auto_increment 可为 0 或 95（真库可能已被手工修过）；
--   * P3/P4：0 行（有输出 = V92 的 PRI 守卫会 SIGNAL，必须先修主键）；
--   * Q1：auto_increment_ok=95、still_missing=0；Q2：0 行。
--
-- 限界：
--   * 只创建 TEMPORARY TABLE，不改业务数据；只读账号需 CREATE TEMPORARY TABLES。
--   * TABLE_ROWS / DATA_LENGTH 是 InnoDB 估算值，非精确行数；仅用于判断哪些表
--     需要维护窗口/gh-ost/pt-online-schema-change，不可用于对外报表。
--   * information_schema.tables.AUTO_INCREMENT 在表从未写入过数据时可能为 NULL；
--     权威判据是 information_schema.columns.EXTRA，表选项仅作参考。
--   * 本项目当前环境没有可用 MySQL 实例（无 Docker/JDBC），本脚本未在真库执行过。
-- ============================================================
SET @v92_verify_expected_table_count = 95;

DROP TEMPORARY TABLE IF EXISTS tmp_v92_verify_expected;
CREATE TEMPORARY TABLE tmp_v92_verify_expected (
    table_name VARCHAR(64) NOT NULL,
    module     VARCHAR(32) NOT NULL,
    PRIMARY KEY (table_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v92_verify_expected (table_name, module) VALUES
    ('aimultiagent_checkpoint', 'aimultiagent'),
    ('aimultiagent_execution_log', 'aimultiagent'),
    ('aimultiagent_execution_span', 'aimultiagent'),
    ('aimultiagent_skill', 'aimultiagent'),
    ('aimultiagent_skill_group', 'aimultiagent'),
    ('aimultiagent_topology', 'aimultiagent'),
    ('airag_eval_dataset', 'airag'),
    ('airag_eval_question', 'airag'),
    ('airag_eval_report', 'airag'),
    ('erp_account', 'erp'),
    ('erp_cost_allocation', 'erp'),
    ('erp_cost_center', 'erp'),
    ('erp_currency', 'erp'),
    ('erp_customer', 'erp'),
    ('erp_exchange_rate', 'erp'),
    ('erp_finance_payment', 'erp'),
    ('erp_finance_payment_item', 'erp'),
    ('erp_finance_receipt', 'erp'),
    ('erp_finance_receipt_item', 'erp'),
    ('erp_product', 'erp'),
    ('erp_product_category', 'erp'),
    ('erp_product_unit', 'erp'),
    ('erp_profit_center', 'erp'),
    ('erp_profitability_analysis', 'erp'),
    ('erp_purchase_in', 'erp'),
    ('erp_purchase_in_items', 'erp'),
    ('erp_purchase_order', 'erp'),
    ('erp_purchase_order_items', 'erp'),
    ('erp_purchase_return', 'erp'),
    ('erp_purchase_return_items', 'erp'),
    ('erp_sale_order', 'erp'),
    ('erp_sale_order_items', 'erp'),
    ('erp_sale_out', 'erp'),
    ('erp_sale_out_items', 'erp'),
    ('erp_sale_return', 'erp'),
    ('erp_sale_return_items', 'erp'),
    ('erp_stock', 'erp'),
    ('erp_stock_batch', 'erp'),
    ('erp_stock_check', 'erp'),
    ('erp_stock_check_item', 'erp'),
    ('erp_stock_in', 'erp'),
    ('erp_stock_in_item', 'erp'),
    ('erp_stock_move', 'erp'),
    ('erp_stock_move_item', 'erp'),
    ('erp_stock_out', 'erp'),
    ('erp_stock_out_item', 'erp'),
    ('erp_stock_record', 'erp'),
    ('erp_stock_serial', 'erp'),
    ('erp_supplier', 'erp'),
    ('erp_warehouse', 'erp'),
    ('hr_attendance', 'hr'),
    ('hr_department', 'hr'),
    ('hr_employee', 'hr'),
    ('hr_performance', 'hr'),
    ('hr_position', 'hr'),
    ('hr_salary', 'hr'),
    ('qms_capa_document', 'qms'),
    ('qms_customer_complaint', 'qms'),
    ('qms_eight_d_report', 'qms'),
    ('qms_electronic_signature_log', 'qms'),
    ('qms_fmea_document', 'qms'),
    ('qms_fmea_item', 'qms'),
    ('qms_inspection_item', 'qms'),
    ('qms_inspection_order', 'qms'),
    ('qms_inspection_record', 'qms'),
    ('qms_msa_measurement', 'qms'),
    ('qms_msa_study', 'qms'),
    ('qms_ncr_document', 'qms'),
    ('qms_ncr_mrb_record', 'qms'),
    ('qms_qualification', 'qms'),
    ('qms_quality_cost', 'qms'),
    ('qms_scar', 'qms'),
    ('qms_supplier_audit', 'qms'),
    ('qms_supplier_rating', 'qms'),
    ('qms_training_plan', 'qms'),
    ('qms_training_record', 'qms'),
    ('wms_check_order', 'wms'),
    ('wms_check_order_detail', 'wms'),
    ('wms_inventory', 'wms'),
    ('wms_inventory_batch', 'wms'),
    ('wms_inventory_history', 'wms'),
    ('wms_item', 'wms'),
    ('wms_item_brand', 'wms'),
    ('wms_item_category', 'wms'),
    ('wms_item_sku', 'wms'),
    ('wms_merchant', 'wms'),
    ('wms_movement_order', 'wms'),
    ('wms_movement_order_detail', 'wms'),
    ('wms_receipt_order', 'wms'),
    ('wms_receipt_order_detail', 'wms'),
    ('wms_shipment_order', 'wms'),
    ('wms_shipment_order_detail', 'wms'),
    ('wms_warehouse', 'wms'),
    ('wms_wave_order', 'wms'),
    ('wms_wave_order_detail', 'wms');

-- ------------------------------------------------------------
-- P1. 预检明细（期望 95 行）：主键风险优先 + 数据量倒序
-- ------------------------------------------------------------
SELECT e.table_name, e.module,
       c.COLUMN_TYPE, c.IS_NULLABLE, c.COLUMN_KEY, c.EXTRA,
       t.ENGINE, t.TABLE_ROWS AS estimated_rows,
       t.DATA_LENGTH AS data_bytes, t.INDEX_LENGTH AS index_bytes,
       t.AUTO_INCREMENT AS table_auto_increment,
       CASE
           WHEN c.COLUMN_NAME IS NULL THEN 'TABLE_OR_COLUMN_MISSING'
           WHEN IFNULL(c.COLUMN_KEY, '') <> 'PRI' THEN 'NO_PRIMARY_KEY_MIGRATION_WILL_FAIL'
           WHEN IFNULL(c.EXTRA, '') NOT LIKE '%auto_increment%' THEN 'NEEDS_V92'
           ELSE 'ALREADY_OK'
       END AS precheck_verdict
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = e.table_name AND c.COLUMN_NAME = 'id'
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = e.table_name
 ORDER BY (c.COLUMN_NAME IS NULL OR IFNULL(c.COLUMN_KEY, '') <> 'PRI') DESC,
          COALESCE(t.DATA_LENGTH, 0) DESC, e.table_name;

-- ------------------------------------------------------------
-- P2. 预检汇总：期望 expected_tables=95 / no_primary_key=0 / missing=0
-- ------------------------------------------------------------
SELECT COUNT(*) AS expected_tables,
       SUM(c.COLUMN_NAME IS NOT NULL) AS id_column_found,
       SUM(IFNULL(c.EXTRA, '') LIKE '%auto_increment%') AS already_auto_increment,
       SUM(c.COLUMN_NAME IS NOT NULL AND IFNULL(c.EXTRA, '') NOT LIKE '%auto_increment%') AS needs_v92,
       SUM(c.COLUMN_NAME IS NULL) AS missing_table_or_column,
       SUM(c.COLUMN_NAME IS NOT NULL AND IFNULL(c.COLUMN_KEY, '') <> 'PRI') AS no_primary_key,
       SUM(COALESCE(t.DATA_LENGTH, 0)) AS total_data_bytes,
       SUM(COALESCE(t.INDEX_LENGTH, 0)) AS total_index_bytes,
       SUM(COALESCE(t.TABLE_ROWS, 0)) AS total_estimated_rows
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = e.table_name AND c.COLUMN_NAME = 'id'
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = e.table_name;

-- ------------------------------------------------------------
-- P3. 高危明细：id 不是 PRIMARY KEY —— V92 的守卫会 SIGNAL 45000 阻断发布
--     期望 0 行。若不为 0，先单独补 PRIMARY KEY，再执行 V92。
-- ------------------------------------------------------------
SELECT e.table_name, e.module, c.COLUMN_TYPE, c.IS_NULLABLE,
       IFNULL(c.COLUMN_KEY, '<NULL>') AS column_key, 'NO_PRIMARY_KEY' AS verdict
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = e.table_name AND c.COLUMN_NAME = 'id'
 WHERE c.COLUMN_NAME IS NULL OR IFNULL(c.COLUMN_KEY, '') <> 'PRI'
 ORDER BY e.table_name;

-- ------------------------------------------------------------
-- P4. 独立主键索引检查（不依赖 columns.COLUMN_KEY）：期望 0 行
-- ------------------------------------------------------------
SELECT e.table_name, e.module, 'NO_PRIMARY_INDEX' AS verdict
  FROM tmp_v92_verify_expected e
  LEFT JOIN (SELECT DISTINCT TABLE_NAME
               FROM information_schema.statistics
              WHERE TABLE_SCHEMA = DATABASE() AND INDEX_NAME = 'PRIMARY') p
         ON p.TABLE_NAME = e.table_name
 WHERE p.TABLE_NAME IS NULL
 ORDER BY e.table_name;
-- ------------------------------------------------------------
-- P5. 大表预警（Top 30）：ALTER TABLE ... MODIFY id AUTO_INCREMENT 可能触发表重建
-- 经验阈值 [判断]：data+index > 1 GiB 视为需要 gh-ost / pt-online-schema-change
-- 或维护窗口；< 1 GiB 也建议先在预发布库计时，再决定是否直接 ALTER。
-- ------------------------------------------------------------
SELECT e.table_name, e.module,
       ROUND((COALESCE(t.DATA_LENGTH, 0) + COALESCE(t.INDEX_LENGTH, 0)) / 1024 / 1024, 1) AS total_mb,
       t.TABLE_ROWS AS estimated_rows,
       CASE WHEN (COALESCE(t.DATA_LENGTH, 0) + COALESCE(t.INDEX_LENGTH, 0)) > 1073741824
            THEN 'LARGE_TABLE_USE_ONLINE_DDL' ELSE 'NORMAL' END AS migration_risk
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = e.table_name
 ORDER BY total_mb DESC
 LIMIT 30;

-- ------------------------------------------------------------
-- Q1. 后检汇总（V92 执行后）：期望 expected_tables=95 / auto_increment_ok=95 / still_missing=0
-- ------------------------------------------------------------
SELECT COUNT(*) AS expected_tables,
       SUM(IFNULL(c.EXTRA, '') LIKE '%auto_increment%') AS auto_increment_ok,
       SUM(c.COLUMN_NAME IS NULL) AS missing_table_or_column,
       SUM(c.COLUMN_NAME IS NOT NULL AND IFNULL(c.EXTRA, '') NOT LIKE '%auto_increment%') AS still_missing,
       SUM(COALESCE(t.TABLE_ROWS, 0)) AS total_estimated_rows
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = e.table_name AND c.COLUMN_NAME = 'id'
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = e.table_name;

-- ------------------------------------------------------------
-- Q2. 后检明细：仍未自增 / 表或列缺失，期望 0 行
-- ------------------------------------------------------------
SELECT e.table_name, e.module, c.COLUMN_TYPE, c.COLUMN_KEY,
       IFNULL(c.EXTRA, '<NO_COLUMN>') AS extra,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING' ELSE 'STILL_NOT_AUTO' END AS verdict
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE() AND c.TABLE_NAME = e.table_name AND c.COLUMN_NAME = 'id'
 WHERE c.COLUMN_NAME IS NULL OR IFNULL(c.EXTRA, '') NOT LIKE '%auto_increment%'
 ORDER BY e.table_name;

-- ------------------------------------------------------------
-- Q3. 表选项 AUTO_INCREMENT 参考值（非门禁）
--     说明：InnoDB 在表从未写入过数据时可能返回 NULL，属正常现象；
--     「下一条会分配什么 id」以实际 INSERT + LAST_INSERT_ID() 为准。
-- ------------------------------------------------------------
SELECT e.table_name, e.module, t.AUTO_INCREMENT AS table_auto_increment,
       t.TABLE_ROWS AS estimated_rows,
       CASE WHEN t.AUTO_INCREMENT IS NULL THEN 'NULL_REFERENCE_ONLY' ELSE 'HAS_COUNTER' END AS note
  FROM tmp_v92_verify_expected e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = e.table_name
 ORDER BY e.module, e.table_name;

-- ------------------------------------------------------------
-- R. 应用层实证（把静态推导升级为动态证据的最短路径）
--
-- R1. 只读部分：确认 sql_mode 与 95 张表里至少一张「有业务数据」的表。
-- ------------------------------------------------------------
SELECT @@GLOBAL.sql_mode AS global_sql_mode, @@SESSION.sql_mode AS session_sql_mode, DATABASE() AS current_db;
SELECT e.table_name, e.module, t.TABLE_ROWS AS estimated_rows
  FROM tmp_v92_verify_expected e
  JOIN information_schema.tables t
    ON t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = e.table_name
 WHERE t.TABLE_ROWS > 0
 ORDER BY t.TABLE_ROWS DESC
 LIMIT 10;

-- R2. 写实证模板（**请手工执行，不要在只读脚本里自动跑**）：
--     * 选一张**预发布库**、有业务数据、且可回滚的受影响表；
--     * 用事务包住，插入后立刻回滚，避免留下脏数据；
--     * 只提供业务列、不提供 id —— 这正是 MyBatis-Plus 的插入形态。
--
--   START TRANSACTION;
--   INSERT INTO <table> (<business_col_1>, <business_col_2>, creator, create_time, updater,
--                        update_time, deleted, tenant_id)
--   VALUES (<value_1>, <value_2>, 'v92-probe', NOW(), 'v92-probe', NOW(), 0, 1);
--   SELECT LAST_INSERT_ID() AS new_id, ROW_COUNT() AS inserted_rows;
--   ROLLBACK;
--
--   判读：
--     * ERROR 1364 "Field 'id' doesn't have a default value" → 证实静态推导，
--       执行 V92 后重跑本实证，期望成功并返回 new_id >= 1；
--     * 若插入成功（无需 V92 也能自增）→ 说明真库与静态分析不一致
--       （例如该表实际已修过、或 sql_mode 非严格），必须以真库为准修正结论；
--     * 非严格模式下插入成功但 new_id = 0 → 仍是坏的，第二次插入会撞主键，
--       必须继续执行 V92。
--
-- R3. 单个表的结构核对（人工抽样，任选一张）：
--   SHOW CREATE TABLE <table>;
--   关注 `id` 定义是否含 AUTO_INCREMENT，以及 `PRIMARY KEY (id)` 是否存在。

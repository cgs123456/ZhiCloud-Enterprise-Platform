-- ============================================================
-- V94 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对六张会计底座表已进入 Flyway 权威迁移路径。
--   2. 核对关键列的类型、可空性、主键/自增属性。
--   3. 核对租户维度幂等唯一键和来源检索索引。
--   4. 核对 erp_gl_voucher 的来源字段与索引。
--
-- 用法：
--   * V94 执行前可先运行；若表不存在，汇总结果为 FAIL，这是预期预检结果。
--   * V94 执行后重跑，Q5 的 v94_verify_verdict 必须为 PASS。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE，不修改业务数据。
--   * information_schema 只证明结构，不证明 Java 过账链路、规则结果或业务数据正确。
--   * 对已有大表新增列/索引可能触发表重建或锁表，必须在预生产同规模数据上计时。
-- ============================================================

SET @v94_verify_expected_table_count = 6;

DROP TEMPORARY TABLE IF EXISTS tmp_v94_verify_expected_table;
CREATE TEMPORARY TABLE tmp_v94_verify_expected_table (
    table_name          VARCHAR(64) NOT NULL,
    expected_engine     VARCHAR(16) NOT NULL,
    expected_collation  VARCHAR(64) NOT NULL,
    PRIMARY KEY (table_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v94_verify_expected_table (table_name, expected_engine, expected_collation) VALUES
    ('erp_accounting_event',              'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_accounting_posting',            'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_posting_rule',                  'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_posting_rule_line',             'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_finance_settlement_allocation', 'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_tax_invoice_source',            'InnoDB', 'utf8mb4_unicode_ci');

DROP TEMPORARY TABLE IF EXISTS tmp_v94_verify_expected_column;
CREATE TEMPORARY TABLE tmp_v94_verify_expected_column (
    table_name       VARCHAR(64) NOT NULL,
    column_name      VARCHAR(64) NOT NULL,
    expected_type    VARCHAR(64) NOT NULL,
    expected_nullable VARCHAR(3) NOT NULL,
    expected_key     VARCHAR(3) NOT NULL DEFAULT '',
    extra_contains   VARCHAR(64) NOT NULL DEFAULT '',
    PRIMARY KEY (table_name, column_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v94_verify_expected_column
    (table_name, column_name, expected_type, expected_nullable, expected_key, extra_contains)
VALUES
    ('erp_accounting_event', 'id', 'bigint', 'NO', 'PRI', 'auto_increment'),
    ('erp_accounting_event', 'event_id', 'varchar(64)', 'NO', '', ''),
    ('erp_accounting_event', 'event_type', 'varchar(64)', 'NO', '', ''),
    ('erp_accounting_event', 'source_doc_type', 'varchar(64)', 'NO', '', ''),
    ('erp_accounting_event', 'source_doc_id', 'bigint', 'NO', '', ''),
    ('erp_accounting_event', 'source_doc_no', 'varchar(64)', 'NO', '', ''),
    ('erp_accounting_event', 'source_doc_line_id', 'bigint', 'NO', '', ''),
    ('erp_accounting_event', 'source_revision', 'int', 'NO', '', ''),
    ('erp_accounting_event', 'event_date', 'date', 'NO', '', ''),
    ('erp_accounting_event', 'accounting_date', 'date', 'NO', '', ''),
    ('erp_accounting_event', 'period_id', 'bigint', 'NO', '', ''),
    ('erp_accounting_event', 'period_code', 'varchar(32)', 'NO', '', ''),
    ('erp_accounting_event', 'amount_without_tax', 'decimal(20,4)', 'NO', '', ''),
    ('erp_accounting_event', 'tax_amount', 'decimal(20,4)', 'NO', '', ''),
    ('erp_accounting_event', 'amount_with_tax', 'decimal(20,4)', 'NO', '', ''),
    ('erp_accounting_event', 'cost_amount', 'decimal(20,4)', 'NO', '', ''),
    ('erp_accounting_event', 'currency', 'varchar(8)', 'NO', '', ''),
    ('erp_accounting_event', 'exchange_rate', 'decimal(20,8)', 'NO', '', ''),
    ('erp_accounting_event', 'status', 'tinyint', 'NO', '', ''),
    ('erp_accounting_event', 'idempotency_key', 'varchar(128)', 'NO', '', ''),
    ('erp_accounting_event', 'rule_set_version', 'varchar(32)', 'NO', '', ''),
    ('erp_accounting_event', 'payload_hash', 'char(64)', 'NO', '', ''),
    ('erp_accounting_event', 'reversal_of_event_id', 'bigint', 'NO', '', ''),
    ('erp_accounting_event', 'retry_count', 'int', 'NO', '', ''),
    ('erp_accounting_event', 'next_retry_time', 'datetime', 'YES', '', ''),
    ('erp_accounting_event', 'last_error', 'varchar(500)', 'NO', '', ''),
    ('erp_accounting_event', 'occurred_at', 'datetime', 'NO', '', ''),
    ('erp_accounting_event', 'posted_at', 'datetime', 'YES', '', ''),
    ('erp_accounting_event', 'creator', 'varchar(64)', 'YES', '', ''),
    ('erp_accounting_event', 'create_time', 'datetime', 'NO', '', ''),
    ('erp_accounting_event', 'updater', 'varchar(64)', 'YES', '', ''),
    ('erp_accounting_event', 'update_time', 'datetime', 'NO', '', 'on update current_timestamp'),
    ('erp_accounting_event', 'deleted', 'bit(1)', 'NO', '', ''),
    ('erp_accounting_event', 'tenant_id', 'bigint', 'NO', '', ''),

    ('erp_accounting_posting', 'id', 'bigint', 'NO', 'PRI', 'auto_increment'),
    ('erp_accounting_posting', 'event_id', 'varchar(64)', 'NO', '', ''),
    ('erp_accounting_posting', 'voucher_id', 'bigint', 'NO', '', ''),
    ('erp_accounting_posting', 'posting_rule_id', 'bigint', 'NO', '', ''),
    ('erp_accounting_posting', 'rule_version', 'varchar(32)', 'NO', '', ''),
    ('erp_accounting_posting', 'status', 'tinyint', 'NO', '', ''),
    ('erp_accounting_posting', 'attempt_no', 'int', 'NO', '', ''),
    ('erp_accounting_posting', 'error_code', 'varchar(64)', 'NO', '', ''),
    ('erp_accounting_posting', 'error_message', 'varchar(500)', 'NO', '', ''),
    ('erp_accounting_posting', 'posted_at', 'datetime', 'YES', '', ''),
    ('erp_accounting_posting', 'creator', 'varchar(64)', 'YES', '', ''),
    ('erp_accounting_posting', 'create_time', 'datetime', 'NO', '', ''),
    ('erp_accounting_posting', 'updater', 'varchar(64)', 'YES', '', ''),
    ('erp_accounting_posting', 'update_time', 'datetime', 'NO', '', 'on update current_timestamp'),
    ('erp_accounting_posting', 'deleted', 'bit(1)', 'NO', '', ''),
    ('erp_accounting_posting', 'tenant_id', 'bigint', 'NO', '', ''),

    ('erp_posting_rule', 'id', 'bigint', 'NO', 'PRI', 'auto_increment'),
    ('erp_posting_rule', 'rule_code', 'varchar(64)', 'NO', '', ''),
    ('erp_posting_rule', 'version', 'varchar(32)', 'NO', '', ''),
    ('erp_posting_rule', 'event_type', 'varchar(64)', 'NO', '', ''),
    ('erp_posting_rule', 'condition_json', 'text', 'YES', '', ''),
    ('erp_posting_rule', 'effective_from', 'datetime', 'NO', '', ''),
    ('erp_posting_rule', 'effective_to', 'datetime', 'YES', '', ''),
    ('erp_posting_rule', 'enabled', 'bit(1)', 'NO', '', ''),
    ('erp_posting_rule', 'published_at', 'datetime', 'YES', '', ''),
    ('erp_posting_rule', 'remark', 'varchar(500)', 'YES', '', ''),
    ('erp_posting_rule', 'creator', 'varchar(64)', 'YES', '', ''),
    ('erp_posting_rule', 'create_time', 'datetime', 'NO', '', ''),
    ('erp_posting_rule', 'updater', 'varchar(64)', 'YES', '', ''),
    ('erp_posting_rule', 'update_time', 'datetime', 'NO', '', 'on update current_timestamp'),
    ('erp_posting_rule', 'deleted', 'bit(1)', 'NO', '', ''),
    ('erp_posting_rule', 'tenant_id', 'bigint', 'NO', '', ''),

    ('erp_posting_rule_line', 'id', 'bigint', 'NO', 'PRI', 'auto_increment'),
    ('erp_posting_rule_line', 'rule_id', 'bigint', 'NO', '', ''),
    ('erp_posting_rule_line', 'line_no', 'int', 'NO', '', ''),
    ('erp_posting_rule_line', 'entry_side', 'tinyint', 'NO', '', ''),
    ('erp_posting_rule_line', 'account_code', 'varchar(50)', 'NO', '', ''),
    ('erp_posting_rule_line', 'account_resolver', 'varchar(64)', 'NO', '', ''),
    ('erp_posting_rule_line', 'amount_expression', 'varchar(255)', 'NO', '', ''),
    ('erp_posting_rule_line', 'condition_json', 'text', 'YES', '', ''),
    ('erp_posting_rule_line', 'remark', 'varchar(500)', 'YES', '', ''),
    ('erp_posting_rule_line', 'creator', 'varchar(64)', 'YES', '', ''),
    ('erp_posting_rule_line', 'create_time', 'datetime', 'NO', '', ''),
    ('erp_posting_rule_line', 'updater', 'varchar(64)', 'YES', '', ''),
    ('erp_posting_rule_line', 'update_time', 'datetime', 'NO', '', 'on update current_timestamp'),
    ('erp_posting_rule_line', 'deleted', 'bit(1)', 'NO', '', ''),
    ('erp_posting_rule_line', 'tenant_id', 'bigint', 'NO', '', ''),

    ('erp_finance_settlement_allocation', 'id', 'bigint', 'NO', 'PRI', 'auto_increment'),
    ('erp_finance_settlement_allocation', 'finance_doc_type', 'varchar(64)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'finance_doc_id', 'bigint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'finance_doc_item_id', 'bigint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'invoice_id', 'bigint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'invoice_line_id', 'bigint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'source_doc_type', 'varchar(64)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'source_doc_id', 'bigint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'allocated_amount', 'decimal(20,4)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'currency', 'varchar(8)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'exchange_rate', 'decimal(20,8)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'status', 'tinyint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'reversal_of_allocation_id', 'bigint', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'allocated_at', 'datetime', 'YES', '', ''),
    ('erp_finance_settlement_allocation', 'last_error', 'varchar(500)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'creator', 'varchar(64)', 'YES', '', ''),
    ('erp_finance_settlement_allocation', 'create_time', 'datetime', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'updater', 'varchar(64)', 'YES', '', ''),
    ('erp_finance_settlement_allocation', 'update_time', 'datetime', 'NO', '', 'on update current_timestamp'),
    ('erp_finance_settlement_allocation', 'deleted', 'bit(1)', 'NO', '', ''),
    ('erp_finance_settlement_allocation', 'tenant_id', 'bigint', 'NO', '', ''),

    ('erp_tax_invoice_source', 'id', 'bigint', 'NO', 'PRI', 'auto_increment'),
    ('erp_tax_invoice_source', 'invoice_id', 'bigint', 'NO', '', ''),
    ('erp_tax_invoice_source', 'invoice_line_id', 'bigint', 'NO', '', ''),
    ('erp_tax_invoice_source', 'source_doc_type', 'varchar(64)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'source_doc_id', 'bigint', 'NO', '', ''),
    ('erp_tax_invoice_source', 'source_doc_line_id', 'bigint', 'NO', '', ''),
    ('erp_tax_invoice_source', 'quantity', 'decimal(20,4)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'amount_without_tax', 'decimal(20,4)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'tax_amount', 'decimal(20,4)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'amount_with_tax', 'decimal(20,4)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'status', 'tinyint', 'NO', '', ''),
    ('erp_tax_invoice_source', 'last_error', 'varchar(500)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'creator', 'varchar(64)', 'YES', '', ''),
    ('erp_tax_invoice_source', 'create_time', 'datetime', 'NO', '', ''),
    ('erp_tax_invoice_source', 'updater', 'varchar(64)', 'YES', '', ''),
    ('erp_tax_invoice_source', 'update_time', 'datetime', 'NO', '', 'on update current_timestamp'),
    ('erp_tax_invoice_source', 'deleted', 'bit(1)', 'NO', '', ''),
    ('erp_tax_invoice_source', 'tenant_id', 'bigint', 'NO', '', ''),

    ('erp_gl_voucher', 'source_event_id', 'bigint', 'NO', '', ''),
    ('erp_gl_voucher', 'source_doc_type', 'varchar(64)', 'NO', '', ''),
    ('erp_gl_voucher', 'source_doc_id', 'bigint', 'NO', '', ''),
    ('erp_gl_voucher', 'source_doc_no', 'varchar(64)', 'NO', '', '');

DROP TEMPORARY TABLE IF EXISTS tmp_v94_verify_expected_index;
CREATE TEMPORARY TABLE tmp_v94_verify_expected_index (
    table_name        VARCHAR(64) NOT NULL,
    index_name        VARCHAR(64) NOT NULL,
    expected_columns  VARCHAR(500) NOT NULL,
    expected_non_unique TINYINT NOT NULL,
    PRIMARY KEY (table_name, index_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v94_verify_expected_index
    (table_name, index_name, expected_columns, expected_non_unique)
VALUES
    ('erp_accounting_event', 'uk_erp_accounting_event_business_id', 'tenant_id,event_id', 0),
    ('erp_accounting_event', 'uk_erp_accounting_event_idempotency', 'tenant_id,idempotency_key', 0),
    ('erp_accounting_event', 'uk_erp_accounting_event_source', 'tenant_id,event_type,source_doc_type,source_doc_id,source_doc_line_id,source_revision,sequence_no,reversal_of_event_id', 0),
    ('erp_accounting_posting', 'uk_erp_accounting_posting_event_rule', 'tenant_id,event_id,posting_rule_id,rule_version', 0),
    ('erp_posting_rule', 'uk_erp_posting_rule_code_version', 'tenant_id,rule_code,version', 0),
    ('erp_posting_rule_line', 'uk_erp_posting_rule_line_no', 'tenant_id,rule_id,line_no', 0),
    ('erp_finance_settlement_allocation', 'uk_erp_finance_settlement_allocation_identity', 'tenant_id,finance_doc_type,finance_doc_item_id,invoice_id,invoice_line_id,source_doc_type,source_doc_id,reversal_of_allocation_id', 0),
    ('erp_tax_invoice_source', 'uk_erp_tax_invoice_source_identity', 'tenant_id,invoice_line_id,source_doc_type,source_doc_id,source_doc_line_id', 0),
    ('erp_gl_voucher', 'idx_erp_gl_voucher_source_event', 'tenant_id,source_event_id,deleted', 1),
    ('erp_gl_voucher', 'idx_erp_gl_voucher_source_doc', 'tenant_id,source_doc_type,source_doc_id,deleted', 1);

-- ------------------------------------------------------------
-- Q1. 表级检查：缺失或表选项漂移。
-- ------------------------------------------------------------
SELECT e.table_name,
       t.ENGINE AS actual_engine,
       t.TABLE_COLLATION AS actual_collation,
       CASE WHEN t.TABLE_NAME IS NULL THEN 'MISSING_TABLE'
            WHEN LOWER(t.ENGINE) <> LOWER(e.expected_engine) THEN 'WRONG_ENGINE'
            WHEN LOWER(t.TABLE_COLLATION) <> LOWER(e.expected_collation) THEN 'WRONG_COLLATION'
            ELSE 'PASS' END AS verdict
  FROM tmp_v94_verify_expected_table e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE()
        AND t.TABLE_NAME = e.table_name
 ORDER BY e.table_name;

-- ------------------------------------------------------------
-- Q2. 列级检查：缺失、类型、可空性、主键和自增属性漂移。
-- ------------------------------------------------------------
SELECT e.table_name, e.column_name,
       c.COLUMN_TYPE AS actual_type,
       c.IS_NULLABLE AS actual_nullable,
       c.COLUMN_KEY AS actual_key,
       c.EXTRA AS actual_extra,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING_COLUMN'
            WHEN LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type) THEN 'WRONG_TYPE'
            WHEN UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG_NULLABILITY'
            WHEN e.expected_key <> '' AND UPPER(IFNULL(c.COLUMN_KEY, '')) <> UPPER(e.expected_key) THEN 'WRONG_KEY'
            WHEN e.extra_contains <> '' AND LOCATE(e.extra_contains, LOWER(IFNULL(c.EXTRA, ''))) = 0 THEN 'MISSING_EXTRA'
            ELSE 'PASS' END AS verdict
  FROM tmp_v94_verify_expected_column e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE()
        AND c.TABLE_NAME = e.table_name
        AND c.COLUMN_NAME = e.column_name
 WHERE c.COLUMN_NAME IS NULL
    OR LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
    OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)
    OR (e.expected_key <> '' AND UPPER(IFNULL(c.COLUMN_KEY, '')) <> UPPER(e.expected_key))
    OR (e.extra_contains <> '' AND LOCATE(e.extra_contains, LOWER(IFNULL(c.EXTRA, ''))) = 0)
 ORDER BY e.table_name, e.column_name;

-- ------------------------------------------------------------
-- Q3. 索引级检查：缺失、列顺序或唯一性漂移。
-- ------------------------------------------------------------
SELECT e.table_name, e.index_name,
       a.index_columns AS actual_columns,
       a.non_unique AS actual_non_unique,
       CASE WHEN a.index_name IS NULL THEN 'MISSING_INDEX'
            WHEN LOWER(a.index_columns) <> LOWER(e.expected_columns) THEN 'WRONG_COLUMN_OR_ORDER'
            WHEN a.non_unique <> e.expected_non_unique THEN 'WRONG_UNIQUENESS'
            ELSE 'PASS' END AS verdict
  FROM tmp_v94_verify_expected_index e
  LEFT JOIN (
      SELECT TABLE_NAME, INDEX_NAME,
             GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns,
             MAX(NON_UNIQUE) AS non_unique
        FROM information_schema.statistics
       WHERE TABLE_SCHEMA = DATABASE()
       GROUP BY TABLE_NAME, INDEX_NAME
  ) a ON a.TABLE_NAME = e.table_name
     AND a.INDEX_NAME = e.index_name
 WHERE a.INDEX_NAME IS NULL
    OR LOWER(a.index_columns) <> LOWER(e.expected_columns)
    OR a.non_unique <> e.expected_non_unique
 ORDER BY e.table_name, e.index_name;

-- ------------------------------------------------------------
-- Q4. 来源字段快速检索：应返回 4 列；缺列时 SQL 会显式报错。
-- ------------------------------------------------------------
SELECT source_event_id, source_doc_type, source_doc_id, source_doc_no
  FROM erp_gl_voucher
 LIMIT 0;

-- ------------------------------------------------------------
-- Q5. 后检汇总：所有门禁为 0 时 PASS。
--     注意：PASS 仅代表结构核对通过，不代表应用业务链路已联调或可上线。
-- ------------------------------------------------------------
WITH table_gate AS (
    SELECT SUM(IF(t.TABLE_NAME IS NULL, 1, 0)) AS missing_tables,
           SUM(IF(t.TABLE_NAME IS NOT NULL
                  AND (LOWER(t.ENGINE) <> LOWER(e.expected_engine)
                       OR LOWER(t.TABLE_COLLATION) <> LOWER(e.expected_collation)), 1, 0)) AS wrong_table_options
      FROM tmp_v94_verify_expected_table e
      LEFT JOIN information_schema.tables t
             ON t.TABLE_SCHEMA = DATABASE()
            AND t.TABLE_NAME = e.table_name
), column_gate AS (
    SELECT SUM(IF(c.COLUMN_NAME IS NULL, 1, 0)) AS missing_columns,
           SUM(IF(c.COLUMN_NAME IS NOT NULL
                  AND (LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                       OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)
                       OR (e.expected_key <> '' AND UPPER(IFNULL(c.COLUMN_KEY, '')) <> UPPER(e.expected_key))
                       OR (e.extra_contains <> '' AND LOCATE(e.extra_contains, LOWER(IFNULL(c.EXTRA, ''))) = 0)), 1, 0)) AS wrong_columns
      FROM tmp_v94_verify_expected_column e
      LEFT JOIN information_schema.columns c
             ON c.TABLE_SCHEMA = DATABASE()
            AND c.TABLE_NAME = e.table_name
            AND c.COLUMN_NAME = e.column_name
), index_gate AS (
    SELECT SUM(IF(a.INDEX_NAME IS NULL
                  OR LOWER(a.index_columns) <> LOWER(e.expected_columns)
                  OR a.non_unique <> e.expected_non_unique, 1, 0)) AS missing_or_wrong_indexes
      FROM tmp_v94_verify_expected_index e
      LEFT JOIN (
          SELECT TABLE_NAME, INDEX_NAME,
                 GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns,
                 MAX(NON_UNIQUE) AS non_unique
            FROM information_schema.statistics
           WHERE TABLE_SCHEMA = DATABASE()
           GROUP BY TABLE_NAME, INDEX_NAME
      ) a ON a.TABLE_NAME = e.table_name
         AND a.INDEX_NAME = e.index_name
)
SELECT @v94_verify_expected_table_count AS expected_tables,
       tg.missing_tables,
       tg.wrong_table_options,
       cg.missing_columns,
       cg.wrong_columns,
       ig.missing_or_wrong_indexes,
       CASE WHEN tg.missing_tables = 0
                  AND tg.wrong_table_options = 0
                  AND cg.missing_columns = 0
                  AND cg.wrong_columns = 0
                  AND ig.missing_or_wrong_indexes = 0
            THEN 'PASS' ELSE 'FAIL' END AS v94_verify_verdict
  FROM table_gate tg, column_gate cg, index_gate ig;

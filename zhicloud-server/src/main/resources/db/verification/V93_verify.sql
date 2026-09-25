-- ============================================================
-- V93 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对 erp_tax_rate / erp_tax_invoice / erp_tax_invoice_line 已进入
--      Flyway 权威迁移路径，并具备最终表结构。
--   2. 核对三张表的 id 已 AUTO_INCREMENT、tenant_id/deleted 列存在。
--   3. 核对三个租户维度唯一键的列顺序和唯一性。
--   4. 核对旧全局唯一键 uk_invoice_no_code 已移除，避免跨租户发票号冲突。
--
-- 用法：
--   * 在 V93 执行前运行：重点看 P1/P2/P3/P4，确认旧库可迁移。
--   * 在 V93 执行后运行：重点看 Q1/Q2/Q3/Q4/Q5/Q6；Q6 必须为 PASS。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE，不修改业务数据；只读账号需有
--     CREATE TEMPORARY TABLES 权限。
--   * information_schema 只证明结构，不证明应用端业务链路已联调。
--   * ALTER TABLE ... MODIFY/AUTO_INCREMENT 在已有大量数据时可能触发表重建或
--     锁表；上线前必须在预生产同规模数据上计时，必要时使用 gh-ost /
--     pt-online-schema-change。
-- ============================================================

SET @v93_verify_expected_table_count = 3;

DROP TEMPORARY TABLE IF EXISTS tmp_v93_verify_expected_table;
CREATE TEMPORARY TABLE tmp_v93_verify_expected_table (
    table_name      VARCHAR(64)  NOT NULL,
    expected_engine VARCHAR(16)  NOT NULL,
    expected_collation VARCHAR(64) NOT NULL,
    PRIMARY KEY (table_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v93_verify_expected_table (table_name, expected_engine, expected_collation) VALUES
    ('erp_tax_rate',         'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_tax_invoice',      'InnoDB', 'utf8mb4_unicode_ci'),
    ('erp_tax_invoice_line', 'InnoDB', 'utf8mb4_unicode_ci');

DROP TEMPORARY TABLE IF EXISTS tmp_v93_verify_expected_column;
CREATE TEMPORARY TABLE tmp_v93_verify_expected_column (
    table_name      VARCHAR(64)  NOT NULL,
    column_name     VARCHAR(64)  NOT NULL,
    expected_type   VARCHAR(64)  NOT NULL,
    expected_nullable VARCHAR(3) NOT NULL,
    expected_key    VARCHAR(3)   NOT NULL DEFAULT '',
    extra_contains  VARCHAR(64)  NOT NULL DEFAULT '',
    PRIMARY KEY (table_name, column_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v93_verify_expected_column
    (table_name, column_name, expected_type, expected_nullable, expected_key, extra_contains)
VALUES
    ('erp_tax_rate', 'id',             'bigint',        'NO',  'PRI', 'auto_increment'),
    ('erp_tax_rate', 'code',           'varchar(64)',   'NO',  '',    ''),
    ('erp_tax_rate', 'name',           'varchar(128)',  'NO',  '',    ''),
    ('erp_tax_rate', 'rate_type',      'tinyint',       'NO',  '',    ''),
    ('erp_tax_rate', 'rate',           'decimal(10,6)', 'NO',  '',    ''),
    ('erp_tax_rate', 'is_default',     'tinyint',       'NO',  '',    ''),
    ('erp_tax_rate', 'effective_date', 'date',          'YES', '',    ''),
    ('erp_tax_rate', 'expiry_date',    'date',          'YES', '',    ''),
    ('erp_tax_rate', 'remark',         'varchar(500)',  'YES', '',    ''),
    ('erp_tax_rate', 'status',         'tinyint',       'NO',  '',    ''),
    ('erp_tax_rate', 'creator',        'varchar(64)',   'YES', '',    ''),
    ('erp_tax_rate', 'create_time',    'datetime',      'NO',  '',    ''),
    ('erp_tax_rate', 'updater',        'varchar(64)',   'YES', '',    ''),
    ('erp_tax_rate', 'update_time',    'datetime',      'NO',  '',    'on update current_timestamp'),
    ('erp_tax_rate', 'deleted',        'bit(1)',        'NO',  '',    ''),
    ('erp_tax_rate', 'tenant_id',      'bigint',        'NO',  '',    ''),

    ('erp_tax_invoice', 'id',                'bigint',        'NO',  'PRI', 'auto_increment'),
    ('erp_tax_invoice', 'invoice_no',        'varchar(64)',   'NO',  '',    ''),
    ('erp_tax_invoice', 'invoice_code',      'varchar(64)',   'NO',  '',    ''),
    ('erp_tax_invoice', 'invoice_type',      'tinyint',       'NO',  '',    ''),
    ('erp_tax_invoice', 'buyer_name',        'varchar(128)',  'YES', '',    ''),
    ('erp_tax_invoice', 'buyer_tax_no',      'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice', 'seller_name',       'varchar(128)',  'YES', '',    ''),
    ('erp_tax_invoice', 'seller_tax_no',     'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice', 'invoice_date',      'date',          'YES', '',    ''),
    ('erp_tax_invoice', 'amount_without_tax','decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice', 'tax_amount',        'decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice', 'amount_with_tax',   'decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice', 'status',            'tinyint',       'NO',  '',    ''),
    ('erp_tax_invoice', 'source_order_type', 'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice', 'source_order_id',   'bigint',        'YES', '',    ''),
    ('erp_tax_invoice', 'remark',            'varchar(500)',  'YES', '',    ''),
    ('erp_tax_invoice', 'creator',           'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice', 'create_time',       'datetime',      'NO',  '',    ''),
    ('erp_tax_invoice', 'updater',           'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice', 'update_time',       'datetime',      'NO',  '',    'on update current_timestamp'),
    ('erp_tax_invoice', 'deleted',           'bit(1)',        'NO',  '',    ''),
    ('erp_tax_invoice', 'tenant_id',         'bigint',        'NO',  '',    ''),

    ('erp_tax_invoice_line', 'id',                'bigint',        'NO',  'PRI', 'auto_increment'),
    ('erp_tax_invoice_line', 'invoice_id',        'bigint',        'NO',  '',    ''),
    ('erp_tax_invoice_line', 'line_no',           'int',           'NO',  '',    ''),
    ('erp_tax_invoice_line', 'product_name',      'varchar(128)',  'NO',  '',    ''),
    ('erp_tax_invoice_line', 'specification',     'varchar(255)',  'YES', '',    ''),
    ('erp_tax_invoice_line', 'unit',              'varchar(32)',   'YES', '',    ''),
    ('erp_tax_invoice_line', 'quantity',          'decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice_line', 'unit_price',        'decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice_line', 'amount_without_tax','decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice_line', 'tax_rate',          'decimal(10,6)', 'NO',  '',    ''),
    ('erp_tax_invoice_line', 'tax_amount',        'decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice_line', 'amount_with_tax',   'decimal(20,4)', 'NO',  '',    ''),
    ('erp_tax_invoice_line', 'remark',            'varchar(500)',  'YES', '',    ''),
    ('erp_tax_invoice_line', 'creator',           'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice_line', 'create_time',       'datetime',      'NO',  '',    ''),
    ('erp_tax_invoice_line', 'updater',           'varchar(64)',   'YES', '',    ''),
    ('erp_tax_invoice_line', 'update_time',       'datetime',      'NO',  '',    'on update current_timestamp'),
    ('erp_tax_invoice_line', 'deleted',           'bit(1)',        'NO',  '',    ''),
    ('erp_tax_invoice_line', 'tenant_id',         'bigint',        'NO',  '',    '');

DROP TEMPORARY TABLE IF EXISTS tmp_v93_verify_expected_index;
CREATE TEMPORARY TABLE tmp_v93_verify_expected_index (
    table_name  VARCHAR(64) NOT NULL,
    index_name  VARCHAR(64) NOT NULL,
    seq_in_index INT        NOT NULL,
    column_name VARCHAR(64) NOT NULL,
    non_unique  TINYINT     NOT NULL,
    PRIMARY KEY (table_name, index_name, seq_in_index)
) ENGINE = MEMORY;

INSERT INTO tmp_v93_verify_expected_index
    (table_name, index_name, seq_in_index, column_name, non_unique)
VALUES
    ('erp_tax_rate', 'uk_erp_tax_rate_code_tenant', 1, 'tenant_id', 0),
    ('erp_tax_rate', 'uk_erp_tax_rate_code_tenant', 2, 'code',      0),
    ('erp_tax_invoice', 'uk_erp_tax_invoice_no_code_tenant', 1, 'tenant_id',     0),
    ('erp_tax_invoice', 'uk_erp_tax_invoice_no_code_tenant', 2, 'invoice_no',    0),
    ('erp_tax_invoice', 'uk_erp_tax_invoice_no_code_tenant', 3, 'invoice_code',  0),
    ('erp_tax_invoice_line', 'uk_erp_tax_invoice_line_invoice_line_tenant', 1, 'tenant_id', 0),
    ('erp_tax_invoice_line', 'uk_erp_tax_invoice_line_invoice_line_tenant', 2, 'invoice_id', 0),
    ('erp_tax_invoice_line', 'uk_erp_tax_invoice_line_invoice_line_tenant', 3, 'line_no',    0);

-- ------------------------------------------------------------
-- P1. 执行前：确认三张旧表是否存在，以及是否已具备 AUTO_INCREMENT。
--     期望：V93 前 table_exists=3；already_auto_increment 可为 0..3。
-- ------------------------------------------------------------
SELECT e.table_name,
       IF(t.TABLE_NAME IS NULL, 0, 1) AS table_exists,
       t.ENGINE AS engine,
       t.TABLE_COLLATION AS table_collation,
       IFNULL(c.EXTRA, '<NO_ID_COLUMN>') AS id_extra,
       IF(LOWER(IFNULL(c.EXTRA, '')) LIKE '%auto_increment%', 1, 0) AS already_auto_increment
  FROM tmp_v93_verify_expected_table e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE()
        AND t.TABLE_NAME = e.table_name
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE()
        AND c.TABLE_NAME = e.table_name
        AND c.COLUMN_NAME = 'id'
 ORDER BY e.table_name;

-- ------------------------------------------------------------
-- P2. 执行前：旧全局唯一键 uk_invoice_no_code 是否存在。
--     存在 = 旧库需要 V93 迁移；不存在不一定是错误，但需结合历史 DDL 判断。
-- ------------------------------------------------------------
SELECT s.TABLE_NAME, s.INDEX_NAME, s.NON_UNIQUE,
       GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) AS index_columns
  FROM information_schema.statistics s
 WHERE s.TABLE_SCHEMA = DATABASE()
   AND s.TABLE_NAME = 'erp_tax_invoice'
   AND s.INDEX_NAME = 'uk_invoice_no_code'
 GROUP BY s.TABLE_NAME, s.INDEX_NAME, s.NON_UNIQUE;

-- ------------------------------------------------------------
-- ------------------------------------------------------------
-- P3. 执行前：目标列中仍为 NULL-able 的列。旧手工脚本通常会让金额、状态、
--     deleted、tenant_id、审计时间列可空；V93 会将其收敛为 NOT NULL。
--     若这些列已有 NULL 数据，V93 会 SIGNAL 并停止，需先修复数据。
-- ------------------------------------------------------------
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE,
       c.COLUMN_DEFAULT, c.EXTRA
  FROM information_schema.columns c
 WHERE c.TABLE_SCHEMA = DATABASE()
   AND (
       (c.TABLE_NAME = 'erp_tax_rate'
        AND c.COLUMN_NAME IN ('is_default', 'status', 'create_time', 'update_time', 'deleted', 'tenant_id'))
       OR (c.TABLE_NAME = 'erp_tax_invoice'
           AND c.COLUMN_NAME IN ('amount_without_tax', 'tax_amount', 'amount_with_tax', 'status',
                                 'create_time', 'update_time', 'deleted', 'tenant_id'))
       OR (c.TABLE_NAME = 'erp_tax_invoice_line'
           AND c.COLUMN_NAME IN ('quantity', 'unit_price', 'amount_without_tax', 'tax_rate',
                                 'tax_amount', 'amount_with_tax', 'create_time', 'update_time',
                                 'deleted', 'tenant_id'))
   )
   AND c.IS_NULLABLE = 'YES'
 ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION;
-- P4. 执行前：表规模与在线 DDL 风险参考。TABLE_ROWS/DATA_LENGTH 是 InnoDB 估算值。
-- ------------------------------------------------------------
SELECT e.table_name,
       COALESCE(t.TABLE_ROWS, 0) AS estimated_rows,
       ROUND((COALESCE(t.DATA_LENGTH, 0) + COALESCE(t.INDEX_LENGTH, 0)) / 1024 / 1024, 1) AS total_mb,
       CASE WHEN (COALESCE(t.DATA_LENGTH, 0) + COALESCE(t.INDEX_LENGTH, 0)) > 1073741824
            THEN 'LARGE_TABLE_USE_ONLINE_DDL' ELSE 'NORMAL' END AS migration_risk
  FROM tmp_v93_verify_expected_table e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE()
        AND t.TABLE_NAME = e.table_name
 ORDER BY total_mb DESC;

-- ------------------------------------------------------------
-- Q1. 后检：表存在、引擎与排序规则。期望 missing_table=0、wrong_options=0。
-- ------------------------------------------------------------
SELECT e.table_name,
       IF(t.TABLE_NAME IS NULL, 0, 1) AS table_exists,
       t.ENGINE AS actual_engine,
       t.TABLE_COLLATION AS actual_collation,
       CASE WHEN t.TABLE_NAME IS NOT NULL
                  AND LOWER(t.ENGINE) = LOWER(e.expected_engine)
                  AND LOWER(t.TABLE_COLLATION) = LOWER(e.expected_collation)
            THEN 'PASS' ELSE 'FAIL' END AS verdict
  FROM tmp_v93_verify_expected_table e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE()
        AND t.TABLE_NAME = e.table_name
 ORDER BY e.table_name;

-- ------------------------------------------------------------
-- Q2. 后检：关键列缺失或定义不符。期望 0 行。
-- ------------------------------------------------------------
SELECT e.table_name, e.column_name, e.expected_type, e.expected_nullable,
       e.expected_key, e.extra_contains,
       c.COLUMN_TYPE AS actual_type,
       c.IS_NULLABLE AS actual_nullable,
       IFNULL(c.COLUMN_KEY, '<NULL>') AS actual_key,
       IFNULL(c.EXTRA, '<NULL>') AS actual_extra,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type) THEN 'WRONG_TYPE'
            WHEN UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG_NULLABILITY'
            WHEN e.expected_key <> '' AND UPPER(IFNULL(c.COLUMN_KEY, '')) <> UPPER(e.expected_key) THEN 'WRONG_KEY'
            WHEN e.extra_contains <> ''
                 AND LOCATE(e.extra_contains, LOWER(IFNULL(c.EXTRA, ''))) = 0 THEN 'WRONG_EXTRA'
            ELSE 'PASS' END AS verdict
  FROM tmp_v93_verify_expected_column e
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
-- Q3. 后检：id 必须为 AUTO_INCREMENT。期望 3 行、auto_increment_ok=3。
-- ------------------------------------------------------------
SELECT COUNT(*) AS expected_tables,
       SUM(IF(LOWER(IFNULL(c.EXTRA, '')) LIKE '%auto_increment%', 1, 0)) AS auto_increment_ok,
       SUM(IF(c.COLUMN_NAME IS NULL, 1, 0)) AS missing_id_column,
       SUM(IF(c.COLUMN_NAME IS NOT NULL
              AND LOWER(IFNULL(c.EXTRA, '')) NOT LIKE '%auto_increment%', 1, 0)) AS still_not_auto
  FROM tmp_v93_verify_expected_table e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE()
        AND c.TABLE_NAME = e.table_name
        AND c.COLUMN_NAME = 'id';

-- ------------------------------------------------------------
-- Q4. 后检：三个租户唯一键的列顺序/唯一性。期望 0 行。
-- ------------------------------------------------------------
SELECT e.table_name, e.index_name, e.seq_in_index, e.column_name,
       s.COLUMN_NAME AS actual_column,
       s.NON_UNIQUE AS actual_non_unique,
       CASE WHEN s.INDEX_NAME IS NULL THEN 'MISSING_INDEX_ENTRY'
            WHEN LOWER(s.COLUMN_NAME) <> LOWER(e.column_name) THEN 'WRONG_COLUMN_OR_ORDER'
            WHEN s.NON_UNIQUE <> e.non_unique THEN 'NOT_UNIQUE'
            ELSE 'PASS' END AS verdict
  FROM tmp_v93_verify_expected_index e
  LEFT JOIN information_schema.statistics s
         ON s.TABLE_SCHEMA = DATABASE()
        AND s.TABLE_NAME = e.table_name
        AND s.INDEX_NAME = e.index_name
        AND s.SEQ_IN_INDEX = e.seq_in_index
 WHERE s.INDEX_NAME IS NULL
    OR LOWER(s.COLUMN_NAME) <> LOWER(e.column_name)
    OR s.NON_UNIQUE <> e.non_unique
 ORDER BY e.table_name, e.index_name, e.seq_in_index;

-- ------------------------------------------------------------
-- Q5. 后检：旧全局唯一键必须移除。期望 0 行。
-- ------------------------------------------------------------
SELECT s.TABLE_NAME, s.INDEX_NAME, s.NON_UNIQUE,
       GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) AS index_columns
  FROM information_schema.statistics s
 WHERE s.TABLE_SCHEMA = DATABASE()
   AND s.TABLE_NAME = 'erp_tax_invoice'
   AND s.INDEX_NAME = 'uk_invoice_no_code'
 GROUP BY s.TABLE_NAME, s.INDEX_NAME, s.NON_UNIQUE;

-- ------------------------------------------------------------
-- Q6. 后检汇总：所有门禁为 0 时 PASS。
--     注意：PASS 仅代表结构核对通过，不代表应用业务链路已联调或可上线。
-- ------------------------------------------------------------
WITH table_gate AS (
    SELECT SUM(IF(t.TABLE_NAME IS NULL, 1, 0)) AS missing_tables,
           SUM(IF(t.TABLE_NAME IS NOT NULL
                  AND (LOWER(t.ENGINE) <> LOWER(e.expected_engine)
                       OR LOWER(t.TABLE_COLLATION) <> LOWER(e.expected_collation)), 1, 0)) AS wrong_table_options
      FROM tmp_v93_verify_expected_table e
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
      FROM tmp_v93_verify_expected_column e
      LEFT JOIN information_schema.columns c
             ON c.TABLE_SCHEMA = DATABASE()
            AND c.TABLE_NAME = e.table_name
            AND c.COLUMN_NAME = e.column_name
), index_gate AS (
    SELECT SUM(IF(s.INDEX_NAME IS NULL
                  OR LOWER(s.COLUMN_NAME) <> LOWER(e.column_name)
                  OR s.NON_UNIQUE <> e.non_unique, 1, 0)) AS missing_or_wrong_index_entries
      FROM tmp_v93_verify_expected_index e
      LEFT JOIN information_schema.statistics s
             ON s.TABLE_SCHEMA = DATABASE()
            AND s.TABLE_NAME = e.table_name
            AND s.INDEX_NAME = e.index_name
            AND s.SEQ_IN_INDEX = e.seq_in_index
), legacy_index_gate AS (
    SELECT COUNT(*) AS legacy_global_unique_index_count
      FROM information_schema.statistics s
     WHERE s.TABLE_SCHEMA = DATABASE()
       AND s.TABLE_NAME = 'erp_tax_invoice'
       AND s.INDEX_NAME = 'uk_invoice_no_code'
)
SELECT @v93_verify_expected_table_count AS expected_tables,
       tg.missing_tables,
       tg.wrong_table_options,
       cg.missing_columns,
       cg.wrong_columns,
       ig.missing_or_wrong_index_entries,
       lg.legacy_global_unique_index_count,
       CASE WHEN tg.missing_tables = 0
                  AND tg.wrong_table_options = 0
                  AND cg.missing_columns = 0
                  AND cg.wrong_columns = 0
                  AND ig.missing_or_wrong_index_entries = 0
                  AND lg.legacy_global_unique_index_count = 0
            THEN 'PASS' ELSE 'FAIL' END AS v93_verify_verdict
  FROM table_gate tg, column_gate cg, index_gate ig, legacy_index_gate lg;

-- ------------------------------------------------------------
-- Q7. 参考值：表级 AUTO_INCREMENT 计数器。NULL 只表示尚无已分配计数，
--     不是失败；权威判据是 Q3 的 information_schema.columns.EXTRA。
-- ------------------------------------------------------------
SELECT e.table_name,
       t.AUTO_INCREMENT AS table_auto_increment,
       t.TABLE_ROWS AS estimated_rows
  FROM tmp_v93_verify_expected_table e
  LEFT JOIN information_schema.tables t
         ON t.TABLE_SCHEMA = DATABASE()
        AND t.TABLE_NAME = e.table_name
 ORDER BY e.table_name;
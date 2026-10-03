-- ============================================================
-- V95 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 先检查依赖表和关键列，缺表/缺列时直接输出 FAIL，不继续查询业务表。
--   2. 核对 tenant_id=0 的默认科目模板和版本化规则头已存在。
--   3. 核对规则行的借贷方向、科目编码、金额表达式与迁移契约一致。
--   4. 输出 PASS/FAIL，并列出缺失或漂移项。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE 和临时存储过程，不修改业务数据。
--   * 默认规则通过不代表法人会计政策已获财务确认。
-- ============================================================

DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_failure;
CREATE TEMPORARY TABLE tmp_v95_verify_failure (
    check_name VARCHAR(64) NOT NULL,
    details    VARCHAR(500) NOT NULL
) ENGINE = MEMORY;

-- 1. 结构预检：只查询 information_schema，不引用可能不存在的业务表。
INSERT INTO tmp_v95_verify_failure (check_name, details)
SELECT 'table_missing', e.table_name
FROM (
    SELECT 'erp_gl_account' AS table_name
    UNION ALL SELECT 'erp_posting_rule'
    UNION ALL SELECT 'erp_posting_rule_line'
) e
LEFT JOIN information_schema.tables t
       ON t.table_schema = DATABASE()
      AND t.table_name = e.table_name
      AND t.table_type = 'BASE TABLE'
WHERE t.table_name IS NULL;

INSERT INTO tmp_v95_verify_failure (check_name, details)
SELECT 'column_missing', CONCAT(e.table_name, '.', e.column_name)
FROM (
    SELECT 'erp_gl_account' AS table_name, 'code' AS column_name
    UNION ALL SELECT 'erp_gl_account', 'name'
    UNION ALL SELECT 'erp_gl_account', 'type'
    UNION ALL SELECT 'erp_gl_account', 'balance_direction'
    UNION ALL SELECT 'erp_gl_account', 'is_leaf'
    UNION ALL SELECT 'erp_gl_account', 'status'
    UNION ALL SELECT 'erp_gl_account', 'deleted'
    UNION ALL SELECT 'erp_gl_account', 'tenant_id'
    UNION ALL SELECT 'erp_posting_rule', 'rule_code'
    UNION ALL SELECT 'erp_posting_rule', 'version'
    UNION ALL SELECT 'erp_posting_rule', 'event_type'
    UNION ALL SELECT 'erp_posting_rule', 'condition_json'
    UNION ALL SELECT 'erp_posting_rule', 'effective_from'
    UNION ALL SELECT 'erp_posting_rule', 'effective_to'
    UNION ALL SELECT 'erp_posting_rule', 'enabled'
    UNION ALL SELECT 'erp_posting_rule', 'deleted'
    UNION ALL SELECT 'erp_posting_rule', 'tenant_id'
    UNION ALL SELECT 'erp_posting_rule_line', 'rule_id'
    UNION ALL SELECT 'erp_posting_rule_line', 'line_no'
    UNION ALL SELECT 'erp_posting_rule_line', 'entry_side'
    UNION ALL SELECT 'erp_posting_rule_line', 'account_code'
    UNION ALL SELECT 'erp_posting_rule_line', 'account_resolver'
    UNION ALL SELECT 'erp_posting_rule_line', 'amount_expression'
    UNION ALL SELECT 'erp_posting_rule_line', 'condition_json'
    UNION ALL SELECT 'erp_posting_rule_line', 'deleted'
    UNION ALL SELECT 'erp_posting_rule_line', 'tenant_id'
) e
LEFT JOIN information_schema.columns c
       ON c.table_schema = DATABASE()
      AND c.table_name = e.table_name
      AND c.column_name = e.column_name
WHERE c.column_name IS NULL;

SELECT COUNT(*) INTO @v95_schema_fail_count
FROM tmp_v95_verify_failure;

-- 2. 构造与迁移契约一致的期望数据。此段不依赖业务表。
DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_expected_account;
CREATE TEMPORARY TABLE tmp_v95_verify_expected_account (
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(100) NOT NULL,
    account_type TINYINT NOT NULL,
    balance_direction TINYINT NOT NULL,
    PRIMARY KEY (account_code)
) ENGINE = MEMORY;

INSERT INTO tmp_v95_verify_expected_account
    (account_code, account_name, account_type, balance_direction)
VALUES
    ('1403', '原材料', 10, 10),
    ('22210101', '应交增值税（进项税额）', 20, 10),
    ('1122', '应收账款', 10, 10),
    ('6001', '主营业务收入', 40, 20),
    ('22210102', '应交增值税（销项税额）', 20, 20),
    ('6401', '主营业务成本', 50, 10),
    ('1405', '库存商品', 10, 10);

DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_expected_rule;
CREATE TEMPORARY TABLE tmp_v95_verify_expected_rule (
    rule_code  VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    version    VARCHAR(32) NOT NULL,
    PRIMARY KEY (rule_code, version)
) ENGINE = MEMORY;

INSERT INTO tmp_v95_verify_expected_rule (rule_code, event_type, version)
VALUES
    ('ERP_PURCHASE_RECEIPT_ACCRUAL', 'PURCHASE_RECEIPT_ACCRUAL', 'v1'),
    ('ERP_PAYMENT_POSTED', 'PAYMENT_POSTED', 'v1'),
    ('ERP_SALES_SHIPMENT_POSTED', 'SALES_SHIPMENT_POSTED', 'v1'),
    ('ERP_RECEIPT_POSTED', 'RECEIPT_POSTED', 'v1');

DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_expected_line;
CREATE TEMPORARY TABLE tmp_v95_verify_expected_line (
    rule_code         VARCHAR(64) NOT NULL,
    line_no           INT NOT NULL,
    entry_side        TINYINT NOT NULL,
    account_code      VARCHAR(50) NOT NULL,
    amount_expression VARCHAR(255) NOT NULL,
    PRIMARY KEY (rule_code, line_no)
) ENGINE = MEMORY;

INSERT INTO tmp_v95_verify_expected_line
    (rule_code, line_no, entry_side, account_code, amount_expression)
VALUES
    ('ERP_PURCHASE_RECEIPT_ACCRUAL', 1, 10, '1403', 'amountWithoutTax'),
    ('ERP_PURCHASE_RECEIPT_ACCRUAL', 2, 10, '22210101', 'taxAmount'),
    ('ERP_PURCHASE_RECEIPT_ACCRUAL', 3, 20, '2202', 'amountWithTax'),
    ('ERP_PAYMENT_POSTED', 1, 10, '2202', 'amountWithTax'),
    ('ERP_PAYMENT_POSTED', 2, 20, '1002', 'amountWithTax'),
    ('ERP_SALES_SHIPMENT_POSTED', 1, 10, '1122', 'amountWithTax'),
    ('ERP_SALES_SHIPMENT_POSTED', 2, 10, '6401', 'costAmount'),
    ('ERP_SALES_SHIPMENT_POSTED', 3, 20, '6001', 'amountWithoutTax'),
    ('ERP_SALES_SHIPMENT_POSTED', 4, 20, '22210102', 'taxAmount'),
    ('ERP_SALES_SHIPMENT_POSTED', 5, 20, '1405', 'costAmount'),
    ('ERP_RECEIPT_POSTED', 1, 10, '1002', 'amountWithTax'),
    ('ERP_RECEIPT_POSTED', 2, 20, '1122', 'amountWithTax');

-- 3. 只有结构预检通过后才执行业务查询，避免缺表时先抛出 SQL 错误。
DROP PROCEDURE IF EXISTS v95_verify_run;
DELIMITER $$
CREATE PROCEDURE v95_verify_run()
BEGIN
    INSERT INTO tmp_v95_verify_failure (check_name, details)
    SELECT 'account_missing_or_mismatch', CONCAT(e.account_code, ':', IFNULL(a.name, 'NULL'))
    FROM tmp_v95_verify_expected_account e
    LEFT JOIN erp_gl_account a
           ON a.tenant_id = 0
          AND a.code COLLATE utf8mb4_unicode_ci = e.account_code COLLATE utf8mb4_unicode_ci
          AND a.deleted = 0
    WHERE a.id IS NULL
       OR a.name COLLATE utf8mb4_unicode_ci <> e.account_name COLLATE utf8mb4_unicode_ci
       OR a.type <> e.account_type
       OR a.balance_direction <> e.balance_direction
       OR a.is_leaf <> 1
       OR a.status <> 0;

    INSERT INTO tmp_v95_verify_failure (check_name, details)
    SELECT 'rule_missing_or_mismatch',
           CONCAT(e.rule_code, '/', e.version, ':', IFNULL(r.event_type, 'NULL'))
    FROM tmp_v95_verify_expected_rule e
    LEFT JOIN erp_posting_rule r
           ON r.tenant_id = 0
          AND r.rule_code COLLATE utf8mb4_unicode_ci = e.rule_code COLLATE utf8mb4_unicode_ci
          AND r.version COLLATE utf8mb4_unicode_ci = e.version COLLATE utf8mb4_unicode_ci
          AND r.deleted = 0
    WHERE r.id IS NULL
       OR r.event_type COLLATE utf8mb4_unicode_ci <> e.event_type COLLATE utf8mb4_unicode_ci
       OR r.enabled <> 1
       OR r.condition_json IS NOT NULL
       OR r.effective_from IS NULL
       OR r.effective_to IS NOT NULL;

    INSERT INTO tmp_v95_verify_failure (check_name, details)
    SELECT 'rule_line_missing_or_mismatch',
           CONCAT(e.rule_code, '#', e.line_no, ':',
                  IFNULL(CAST(l.entry_side AS CHAR), 'NULL'), '/',
                  IFNULL(l.account_code, 'NULL'), '/',
                  IFNULL(l.amount_expression, 'NULL'))
    FROM tmp_v95_verify_expected_line e
    LEFT JOIN erp_posting_rule r
           ON r.tenant_id = 0
          AND r.rule_code COLLATE utf8mb4_unicode_ci = e.rule_code COLLATE utf8mb4_unicode_ci
          AND r.version = 'v1'
          AND r.deleted = 0
    LEFT JOIN erp_posting_rule_line l
           ON l.tenant_id = 0
          AND l.rule_id = r.id
          AND l.line_no = e.line_no
          AND l.deleted = 0
    WHERE r.id IS NULL
       OR l.id IS NULL
       OR l.entry_side <> e.entry_side
       OR l.account_code COLLATE utf8mb4_unicode_ci <> e.account_code COLLATE utf8mb4_unicode_ci
       OR l.amount_expression COLLATE utf8mb4_unicode_ci <> e.amount_expression COLLATE utf8mb4_unicode_ci
       OR l.account_resolver <> ''
       OR l.condition_json IS NOT NULL;

    INSERT INTO tmp_v95_verify_failure (check_name, details)
    SELECT 'rule_line_count_mismatch',
           CONCAT(e.rule_code, ':expected=', e.expected_count, ',actual=', IFNULL(actual.actual_count, 0))
    FROM (
        SELECT rule_code, COUNT(*) AS expected_count
        FROM tmp_v95_verify_expected_line
        GROUP BY rule_code
    ) e
    LEFT JOIN (
        SELECT r.rule_code, COUNT(l.id) AS actual_count
        FROM erp_posting_rule r
        LEFT JOIN erp_posting_rule_line l
               ON l.tenant_id = r.tenant_id
              AND l.rule_id = r.id
              AND l.deleted = 0
        WHERE r.tenant_id = 0
          AND r.rule_code IN ('ERP_PURCHASE_RECEIPT_ACCRUAL', 'ERP_PAYMENT_POSTED', 'ERP_SALES_SHIPMENT_POSTED', 'ERP_RECEIPT_POSTED')
          AND r.version = 'v1'
          AND r.deleted = 0
        GROUP BY r.rule_code
    ) actual ON actual.rule_code COLLATE utf8mb4_unicode_ci = e.rule_code COLLATE utf8mb4_unicode_ci
    WHERE IFNULL(actual.actual_count, 0) <> e.expected_count;

    INSERT INTO tmp_v95_verify_failure (check_name, details)
    SELECT 'rule_line_account_missing',
           CONCAT(e.rule_code, '#', e.line_no, ':', e.account_code)
    FROM tmp_v95_verify_expected_line e
    LEFT JOIN erp_gl_account a
           ON a.tenant_id = 0
          AND a.code COLLATE utf8mb4_unicode_ci = e.account_code COLLATE utf8mb4_unicode_ci
          AND a.deleted = 0
    WHERE a.id IS NULL;
END$$
DELIMITER ;

SET @v95_verify_sql = IF(@v95_schema_fail_count = 0,
                         'CALL v95_verify_run()',
                         'SELECT 1 WHERE 0');
PREPARE v95_verify_stmt FROM @v95_verify_sql;
EXECUTE v95_verify_stmt;
DEALLOCATE PREPARE v95_verify_stmt;

SELECT check_name, details
FROM tmp_v95_verify_failure
ORDER BY check_name, details;

SELECT CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS v95_verify_verdict
FROM tmp_v95_verify_failure;

DROP PROCEDURE IF EXISTS v95_verify_run;
DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_failure;
DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_expected_account;
DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_expected_rule;
DROP TEMPORARY TABLE IF EXISTS tmp_v95_verify_expected_line;

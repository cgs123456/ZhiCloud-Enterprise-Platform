-- ============================================================
-- V97 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对销售退货和采购退货默认规则头已发布。
--   2. 核对两条规则的规则行数量、借贷方向与金额表达式组合符合红字口径。
--   3. 核对规则引用的科目在默认科目表中存在，金额表达式在白名单内。
--
-- 用法：
--   * V97 执行前可先运行；若规则缺失，汇总结果为 FAIL，这是预期预检结果。
--   * V97 执行后重跑，Q4 的 v97_verify_verdict 必须为 PASS。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE，不修改业务数据，不变更结构。
--   * 规则存在不代表会计政策已确认。脚本按“期望借贷映射”逐行核对方向与金额
--     表达式，不校验单据金额累计是否超过原单据，也不校验科目余额方向。
-- ============================================================

SET @v97_verify_expected_rule_count = 2;

DROP TEMPORARY TABLE IF EXISTS tmp_v97_verify_expected_rule;
CREATE TEMPORARY TABLE tmp_v97_verify_expected_rule (
    rule_code          VARCHAR(64) NOT NULL,
    event_type         VARCHAR(64) NOT NULL,
    expected_lines     INT NOT NULL,
    expected_side_map  VARCHAR(255) NOT NULL,
    PRIMARY KEY (rule_code)
) ENGINE = MEMORY;

-- expected_side_map 口径：按 line_no 升序把规则行拼成 "<entry_side>:<amount_expression>"，
-- entry_side 10=借、20=贷，行间分隔符为 |。任意一行的借贷方向或金额表达式被改动，
-- 映射都会失配，Q3/Q4 立即判 WRONG / FAIL。
INSERT INTO tmp_v97_verify_expected_rule
    (rule_code, event_type, expected_lines, expected_side_map)
VALUES
    ('ERP_SALES_RETURN_POSTED', 'SALES_RETURN_POSTED', 5,
     '10:amountWithoutTax|10:taxAmount|20:amountWithTax|10:costAmount|20:costAmount'),
    ('ERP_PURCHASE_RETURN_POSTED', 'PURCHASE_RETURN_POSTED', 3,
     '20:amountWithoutTax|20:taxAmount|10:amountWithTax');

-- ------------------------------------------------------------
-- Q1. 规则头与行数量核对
-- ------------------------------------------------------------
SELECT e.rule_code,
       e.event_type,
       e.expected_lines,
       r.id          AS actual_rule_id,
       COUNT(l.id)   AS actual_lines,
       CASE WHEN r.id IS NULL THEN 'MISSING'
            WHEN COUNT(l.id) <> e.expected_lines THEN 'WRONG'
            ELSE 'OK' END AS rule_verdict
  FROM tmp_v97_verify_expected_rule e
  LEFT JOIN erp_posting_rule r
         ON r.tenant_id = 0
        AND r.rule_code = e.rule_code
        AND r.version = 'v1'
        AND r.deleted = 0
  LEFT JOIN erp_posting_rule_line l
         ON l.tenant_id = 0
        AND l.rule_id = r.id
        AND l.deleted = 0
 GROUP BY e.rule_code, e.event_type, e.expected_lines, r.id;

-- ------------------------------------------------------------
-- Q2. 规则行明细核对：科目必须存在，金额表达式必须在白名单内
-- ------------------------------------------------------------
SELECT r.rule_code,
       l.line_no,
       l.entry_side,
       l.account_code,
       l.amount_expression,
       a.id AS account_id,
       CASE WHEN a.id IS NULL THEN 'MISSING_ACCOUNT'
            WHEN l.amount_expression NOT IN
                 ('amountWithoutTax', 'taxAmount', 'amountWithTax', 'costAmount') THEN 'BAD_EXPRESSION'
            ELSE 'OK' END AS line_verdict
  FROM erp_posting_rule r
  JOIN erp_posting_rule_line l
    ON l.tenant_id = 0
   AND l.rule_id = r.id
   AND l.deleted = 0
  LEFT JOIN erp_gl_account a
         ON a.tenant_id = 0
        AND a.code = l.account_code
        AND a.deleted = 0
 WHERE r.tenant_id = 0
   AND r.deleted = 0
   AND r.version = 'v1'
   AND r.rule_code IN ('ERP_SALES_RETURN_POSTED', 'ERP_PURCHASE_RETURN_POSTED')
 ORDER BY r.rule_code, l.line_no;

-- ------------------------------------------------------------
-- Q3. 借贷方向核对（只读）：逐行比对期望借贷映射
--     注意：此处只核对“方向 + 金额表达式”的组合，不校验金额是否超过原单据。
-- ------------------------------------------------------------
SELECT e.rule_code,
       e.expected_side_map,
       IFNULL(GROUP_CONCAT(CONCAT(l.entry_side, ':', l.amount_expression)
                           ORDER BY l.line_no SEPARATOR '|'), '') AS actual_side_map,
       CASE WHEN r.id IS NULL THEN 'MISSING'
            WHEN IFNULL(GROUP_CONCAT(CONCAT(l.entry_side, ':', l.amount_expression)
                                     ORDER BY l.line_no SEPARATOR '|'), '')
                 <> e.expected_side_map THEN 'WRONG'
            ELSE 'OK' END AS direction_verdict
  FROM tmp_v97_verify_expected_rule e
  LEFT JOIN erp_posting_rule r
         ON r.tenant_id = 0
        AND r.rule_code = e.rule_code
        AND r.version = 'v1'
        AND r.deleted = 0
  LEFT JOIN erp_posting_rule_line l
         ON l.tenant_id = 0
        AND l.rule_id = r.id
        AND l.deleted = 0
 GROUP BY e.rule_code, e.expected_side_map, r.id;

-- ------------------------------------------------------------
-- Q4. 后检汇总：所有门禁为 0 时 PASS。
--     注意：PASS 仅代表规则已发布、行数与借贷方向结构正确，
--     不代表会计政策已签字确认。
-- ------------------------------------------------------------
WITH rule_gate AS (
    SELECT SUM(IF(r.id IS NULL, 1, 0)) AS missing_rules,
           SUM(IF(r.id IS NOT NULL AND COUNT(l.id) <> e.expected_lines, 1, 0)) AS wrong_line_counts,
           SUM(IF(r.id IS NOT NULL
                  AND IFNULL(GROUP_CONCAT(CONCAT(l.entry_side, ':', l.amount_expression)
                                          ORDER BY l.line_no SEPARATOR '|'), '')
                      <> e.expected_side_map, 1, 0)) AS wrong_direction_maps
      FROM tmp_v97_verify_expected_rule e
      LEFT JOIN erp_posting_rule r
             ON r.tenant_id = 0
            AND r.rule_code = e.rule_code
            AND r.version = 'v1'
            AND r.deleted = 0
      LEFT JOIN erp_posting_rule_line l
             ON l.tenant_id = 0
            AND l.rule_id = r.id
            AND l.deleted = 0
     GROUP BY e.rule_code, e.expected_lines, e.expected_side_map, r.id
), line_gate AS (
    SELECT SUM(IF(a.id IS NULL, 1, 0)) AS missing_accounts,
           SUM(IF(a.id IS NOT NULL
                  AND l.amount_expression NOT IN
                      ('amountWithoutTax', 'taxAmount', 'amountWithTax', 'costAmount'), 1, 0)) AS bad_expressions,
           COUNT(*) AS checked_lines
      FROM erp_posting_rule r
      JOIN erp_posting_rule_line l
        ON l.tenant_id = 0
       AND l.rule_id = r.id
       AND l.deleted = 0
      LEFT JOIN erp_gl_account a
             ON a.tenant_id = 0
            AND a.code = l.account_code
            AND a.deleted = 0
     WHERE r.tenant_id = 0
       AND r.deleted = 0
       AND r.version = 'v1'
       AND r.rule_code IN ('ERP_SALES_RETURN_POSTED', 'ERP_PURCHASE_RETURN_POSTED')
)
SELECT @v97_verify_expected_rule_count AS expected_rules,
       rg.missing_rules,
       rg.wrong_line_counts,
       rg.wrong_direction_maps,
       lg.checked_lines,
       lg.missing_accounts,
       lg.bad_expressions,
       CASE WHEN rg.missing_rules = 0
                  AND rg.wrong_line_counts = 0
                  AND rg.wrong_direction_maps = 0
                  AND lg.missing_accounts = 0
                  AND lg.bad_expressions = 0
                  AND lg.checked_lines = 8
            THEN 'PASS' ELSE 'FAIL' END AS v97_verify_verdict
  FROM rule_gate rg, line_gate lg;

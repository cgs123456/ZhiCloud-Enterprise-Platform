-- ============================================================
-- V99 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对 erp_accounting_event 已具备 doc_total_amount 金额勾稽总额列。
--   2. 只读体检：在已接入勾稽守卫的事件组上，核对「同源单据同事件类型」的
--      带符号累计金额绝对值是否超过勾稽总额，并列出基线不一致的分组。
--   3. 统计未接入守卫（doc_total_amount IS NULL）的事件数量，作为覆盖率观察项。
--
-- 用法：
--   * V99 执行前可先运行；若列缺失，Q4 汇总为 FAIL，这是预期预检结果。
--   * V99 执行后重跑，Q4 的 v99_verify_verdict 必须为 PASS。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE，不修改业务数据。
--   * information_schema 只证明结构存在，不证明应用层守卫已接线。
--   * Q2 的「无违规」只在当前数据快照上成立；它是对账体检，不是实时约束，
--     并发写入下的超额仍须依赖应用层守卫（唯一键只能防重复，不能防超额）。
-- ============================================================

SET @v99_verify_expected_column_count = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_v99_verify_expected_column;
CREATE TEMPORARY TABLE tmp_v99_verify_expected_column (
    table_name        VARCHAR(64) NOT NULL,
    column_name       VARCHAR(64) NOT NULL,
    expected_type     VARCHAR(64) NOT NULL,
    expected_nullable VARCHAR(3) NOT NULL,
    PRIMARY KEY (table_name, column_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v99_verify_expected_column
    (table_name, column_name, expected_type, expected_nullable)
VALUES
    ('erp_accounting_event', 'doc_total_amount', 'decimal(20,4)', 'YES');

-- ------------------------------------------------------------
-- Q1. 勾稽总额列明细核对
-- ------------------------------------------------------------
SELECT e.table_name,
       e.column_name,
       e.expected_type,
       e.expected_nullable,
       c.COLUMN_TYPE    AS actual_type,
       c.IS_NULLABLE    AS actual_nullable,
       c.COLUMN_DEFAULT AS actual_default,
       c.COLUMN_COMMENT AS actual_comment,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                 OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG'
            ELSE 'OK' END AS column_verdict
  FROM tmp_v99_verify_expected_column e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE()
        AND c.TABLE_NAME = e.table_name
        AND c.COLUMN_NAME = e.column_name;

-- ------------------------------------------------------------
-- Q2. 勾稽体检明细（只读）：只体检已接入守卫的分组（基线非 NULL）。
--     带符号求和使「原事件 + 冲销事件」自动抵消，故已冲销额度可重新使用。
--     刻意不按 reversal_of_event_id 过滤正向事件：冲销事件是负向事件，承担“释放额度”
--     的作用；一旦把它排除在求和之外，反审核后重新过账 / 重新占用已释放额度都会被
--     误判为超额，与应用层守卫口径不一致，会产出假阳性 FAIL。
-- ------------------------------------------------------------
SELECT tenant_id,
       event_type,
       source_doc_type,
       source_doc_id,
       MIN(doc_total_amount)                    AS baseline_min,
       MAX(doc_total_amount)                    AS baseline_max,
       SUM(amount_with_tax)                     AS signed_amount_sum,
       ABS(SUM(amount_with_tax))                AS signed_amount_abs,
       COUNT(*)                                 AS event_count,
       CASE WHEN MIN(doc_total_amount) <> MAX(doc_total_amount) THEN 'BASELINE_INCONSISTENT'
            WHEN ABS(SUM(amount_with_tax)) > MAX(doc_total_amount) THEN 'EXCEEDED'
            ELSE 'OK' END AS group_verdict
  FROM erp_accounting_event
 WHERE deleted = 0
   AND doc_total_amount IS NOT NULL
 GROUP BY tenant_id, event_type, source_doc_type, source_doc_id
HAVING group_verdict <> 'OK'
 ORDER BY group_verdict, ABS(SUM(amount_with_tax)) - MAX(doc_total_amount) DESC;

-- ------------------------------------------------------------
-- Q3. 守卫覆盖率观察项（只读）：NULL 基线不参与守卫，数量应随调用方接线推进下降。
-- ------------------------------------------------------------
SELECT COUNT(*)                                        AS total_events,
       SUM(IF(doc_total_amount IS NULL, 1, 0))         AS events_without_baseline,
       SUM(IF(doc_total_amount IS NOT NULL, 1, 0))     AS events_with_baseline,
       COUNT(DISTINCT CASE WHEN doc_total_amount IS NOT NULL
                           THEN CONCAT(event_type, '|', source_doc_type) END) AS covered_source_kinds
  FROM erp_accounting_event
 WHERE deleted = 0;

-- ------------------------------------------------------------
-- Q4. 后检汇总：结构与体检门禁全为 0 时 PASS。
--     注意：PASS 仅代表「当前快照结构正确且无超额」，不代表守卫已在所有
--     调用方接线，也不代表部分结算业务口径已确认或可上线。
-- ------------------------------------------------------------
WITH column_gate AS (
    SELECT SUM(IF(c.COLUMN_NAME IS NULL, 1, 0)) AS missing_columns,
           SUM(IF(c.COLUMN_NAME IS NOT NULL
                  AND (LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                       OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)), 1, 0)) AS wrong_columns
      FROM tmp_v99_verify_expected_column e
      LEFT JOIN information_schema.columns c
             ON c.TABLE_SCHEMA = DATABASE()
            AND c.TABLE_NAME = e.table_name
            AND c.COLUMN_NAME = e.column_name
), group_gate AS (
    SELECT SUM(IF(MIN(doc_total_amount) <> MAX(doc_total_amount), 1, 0)) AS inconsistent_baselines,
           SUM(IF(MIN(doc_total_amount) = MAX(doc_total_amount)
                  AND ABS(SUM(amount_with_tax)) > MAX(doc_total_amount), 1, 0)) AS exceeded_groups
      FROM (
            SELECT tenant_id,
                   event_type,
                   source_doc_type,
                   source_doc_id,
                   MIN(doc_total_amount) AS doc_total_min,
                   MAX(doc_total_amount) AS doc_total_max,
                   SUM(amount_with_tax)  AS signed_amount_sum
              FROM erp_accounting_event
             WHERE deleted = 0
                           AND doc_total_amount IS NOT NULL
             GROUP BY tenant_id, event_type, source_doc_type, source_doc_id
           ) g
)
SELECT @v99_verify_expected_column_count AS expected_columns,
       cg.missing_columns,
       cg.wrong_columns,
       gg.inconsistent_baselines,
       gg.exceeded_groups,
       CASE WHEN cg.missing_columns = 0
                  AND cg.wrong_columns = 0
                  AND gg.inconsistent_baselines = 0
                  AND gg.exceeded_groups = 0
            THEN 'PASS' ELSE 'FAIL' END AS v99_verify_verdict
  FROM column_gate cg, group_gate gg;

-- ============================================================
-- V96 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对 erp_accounting_event 已具备 sequence_no 期次列。
--   2. 核对来源唯一键已扩展为包含 sequence_no，支持部分开票/收款/付款按期次落地。
--   3. 只读统计已落地的期次事件数量，用于确认迁移未改写历史事件。
--
-- 用法：
--   * V96 执行前可先运行；若列或索引缺失，汇总结果为 FAIL，这是预期预检结果。
--   * V96 执行后重跑，Q4 的 v96_verify_verdict 必须为 PASS。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE，不修改业务数据。
--   * information_schema 只证明结构，不证明部分结算金额累计不超额，
--     也不证明应用侧已正确写入期次。
-- ============================================================

SET @v96_verify_expected_column_count = 1;
SET @v96_verify_expected_index_count = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_v96_verify_expected_column;
CREATE TEMPORARY TABLE tmp_v96_verify_expected_column (
    table_name        VARCHAR(64) NOT NULL,
    column_name       VARCHAR(64) NOT NULL,
    expected_type     VARCHAR(64) NOT NULL,
    expected_nullable VARCHAR(3) NOT NULL,
    expected_key      VARCHAR(3) NOT NULL DEFAULT '',
    extra_contains    VARCHAR(64) NOT NULL DEFAULT '',
    PRIMARY KEY (table_name, column_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v96_verify_expected_column
    (table_name, column_name, expected_type, expected_nullable, expected_key, extra_contains)
VALUES
    ('erp_accounting_event', 'sequence_no', 'int', 'NO', '', '');

DROP TEMPORARY TABLE IF EXISTS tmp_v96_verify_expected_index;
CREATE TEMPORARY TABLE tmp_v96_verify_expected_index (
    table_name         VARCHAR(64) NOT NULL,
    index_name         VARCHAR(64) NOT NULL,
    expected_columns   VARCHAR(500) NOT NULL,
    expected_non_unique TINYINT NOT NULL,
    PRIMARY KEY (table_name, index_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v96_verify_expected_index
    (table_name, index_name, expected_columns, expected_non_unique)
VALUES
    ('erp_accounting_event', 'uk_erp_accounting_event_source',
     'tenant_id,event_type,source_doc_type,source_doc_id,source_doc_line_id,source_revision,sequence_no,reversal_of_event_id',
     0);

-- ------------------------------------------------------------
-- Q1. 期次列明细核对
-- ------------------------------------------------------------
SELECT e.table_name,
       e.column_name,
       e.expected_type,
       e.expected_nullable,
       c.COLUMN_TYPE    AS actual_type,
       c.IS_NULLABLE    AS actual_nullable,
       c.COLUMN_DEFAULT AS actual_default,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                 OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG'
            ELSE 'OK' END AS column_verdict
  FROM tmp_v96_verify_expected_column e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE()
        AND c.TABLE_NAME = e.table_name
        AND c.COLUMN_NAME = e.column_name;

-- ------------------------------------------------------------
-- Q2. 来源唯一键明细核对
-- ------------------------------------------------------------
SELECT e.table_name,
       e.index_name,
       e.expected_columns,
       a.index_columns   AS actual_columns,
       a.non_unique      AS actual_non_unique,
       CASE WHEN a.INDEX_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(a.index_columns) <> LOWER(e.expected_columns)
                 OR a.non_unique <> e.expected_non_unique THEN 'WRONG'
            ELSE 'OK' END AS index_verdict
  FROM tmp_v96_verify_expected_index e
  LEFT JOIN (
        SELECT TABLE_NAME,
               INDEX_NAME,
               GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns,
               MAX(NON_UNIQUE) AS non_unique
          FROM information_schema.statistics
         WHERE TABLE_SCHEMA = DATABASE()
         GROUP BY TABLE_NAME, INDEX_NAME
       ) a ON a.TABLE_NAME = e.table_name
          AND a.INDEX_NAME = e.index_name;

-- ------------------------------------------------------------
-- Q3. 期次数据体检（只读）：迁移不应改写历史事件，sequence_no 只能由应用写入。
-- ------------------------------------------------------------
SELECT COUNT(*) AS total_events,
       SUM(IF(sequence_no = 0, 1, 0)) AS main_events,
       SUM(IF(sequence_no > 0, 1, 0)) AS installment_events
  FROM erp_accounting_event;

-- ------------------------------------------------------------
-- Q4. 后检汇总：所有门禁为 0 时 PASS。
--     注意：PASS 仅代表结构核对通过，不代表部分结算业务口径已确认或可上线。
-- ------------------------------------------------------------
WITH column_gate AS (
    SELECT SUM(IF(c.COLUMN_NAME IS NULL, 1, 0)) AS missing_columns,
           SUM(IF(c.COLUMN_NAME IS NOT NULL
                  AND (LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                       OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)), 1, 0)) AS wrong_columns
      FROM tmp_v96_verify_expected_column e
      LEFT JOIN information_schema.columns c
             ON c.TABLE_SCHEMA = DATABASE()
            AND c.TABLE_NAME = e.table_name
            AND c.COLUMN_NAME = e.column_name
), index_gate AS (
    SELECT SUM(IF(a.INDEX_NAME IS NULL
                  OR LOWER(a.index_columns) <> LOWER(e.expected_columns)
                  OR a.non_unique <> e.expected_non_unique, 1, 0)) AS missing_or_wrong_indexes
      FROM tmp_v96_verify_expected_index e
      LEFT JOIN (
          SELECT TABLE_NAME,
                 INDEX_NAME,
                 GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS index_columns,
                 MAX(NON_UNIQUE) AS non_unique
            FROM information_schema.statistics
           WHERE TABLE_SCHEMA = DATABASE()
           GROUP BY TABLE_NAME, INDEX_NAME
      ) a ON a.TABLE_NAME = e.table_name
         AND a.INDEX_NAME = e.index_name
)
SELECT @v96_verify_expected_column_count AS expected_columns,
       @v96_verify_expected_index_count  AS expected_indexes,
       cg.missing_columns,
       cg.wrong_columns,
       ig.missing_or_wrong_indexes,
       CASE WHEN cg.missing_columns = 0
                  AND cg.wrong_columns = 0
                  AND ig.missing_or_wrong_indexes = 0
            THEN 'PASS' ELSE 'FAIL' END AS v96_verify_verdict
  FROM column_gate cg, index_gate ig;

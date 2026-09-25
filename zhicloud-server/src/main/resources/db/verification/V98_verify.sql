-- ============================================================
-- V98 人工复核脚本（只读，不改业务数据、不改结构）
--
-- 目标：
--   1. Q0 预检 V98 未创建的唯一键是否存在重复数据（唯一键延后迁移的前置条件）。
--   2. Q1 核对 3 张冲突表的最终列集合（两种迁移历史必须收敛到同一列集合）。
--   3. Q2 核对 8 张“两份定义逐字节相同”的表的列数（同构组不需要补列）。
--   4. Q3 核对 erp_period / erp_period_close 的 id 已带 AUTO_INCREMENT。
--   5. Q4 核对 V98 应创建的非唯一索引已存在。
--   6. Q5 统计回填残留（需要人工补录的行）。
--   7. Q6 给出 PASS / FAIL 总判定。
--
-- 用法：
--   * V98 执行前可先运行；缺列/缺自增会判 FAIL，这是预期预检结果。
--   * V98 执行后重跑，Q6 的 v98_verify_verdict 必须为 PASS；
--     Q5 残留计数只作提示，不参与 PASS/FAIL 判定，但必须人工清零。
--
-- 限界：
--   * 本脚本只创建 TEMPORARY TABLE，不修改业务数据，不变更结构。
--   * 结构一致不代表会计期间数据正确；期间口径仍需财务确认。
-- ============================================================

-- ------------------------------------------------------------
-- Q0. 唯一键前置预检：重复数据决定唯一键能否建立
-- ------------------------------------------------------------
SELECT 'erp_period(tenant_id,code,deleted)' AS candidate_key,
       COUNT(*) AS duplicate_groups
  FROM (SELECT tenant_id, code, deleted
          FROM erp_period
         GROUP BY tenant_id, code, deleted
        HAVING COUNT(*) > 1) d;

SELECT 'erp_period_close(tenant_id,period_id,type,deleted)' AS candidate_key,
       COUNT(*) AS duplicate_groups
  FROM (SELECT tenant_id, period_id, type, deleted
          FROM erp_period_close
         GROUP BY tenant_id, period_id, type, deleted
        HAVING COUNT(*) > 1) d;

-- ------------------------------------------------------------
-- Q1. 冲突表列存在性核对
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v98_expected_column;
CREATE TEMPORARY TABLE tmp_v98_expected_column (
    table_name  VARCHAR(64) NOT NULL,
    column_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (table_name, column_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v98_expected_column (table_name, column_name) VALUES
    ('erp_period', 'id'),
    ('erp_period', 'year'),
    ('erp_period', 'month'),
    ('erp_period', 'code'),
    ('erp_period', 'start_date'),
    ('erp_period', 'end_date'),
    ('erp_period', 'status'),
    ('erp_period', 'closed_by'),
    ('erp_period', 'closed_time'),
    ('erp_period', 'remark'),
    ('erp_period', 'name'),
    ('erp_period', 'start_time'),
    ('erp_period', 'end_time'),
    ('erp_period', 'creator'),
    ('erp_period', 'create_time'),
    ('erp_period', 'updater'),
    ('erp_period', 'update_time'),
    ('erp_period', 'deleted'),
    ('erp_period', 'tenant_id'),
    ('erp_period_close', 'id'),
    ('erp_period_close', 'period_id'),
    ('erp_period_close', 'period_code'),
    ('erp_period_close', 'type'),
    ('erp_period_close', 'executed_by'),
    ('erp_period_close', 'executed_time'),
    ('erp_period_close', 'process_status'),
    ('erp_period_close', 'summary'),
    ('erp_period_close', 'adjustment_amount'),
    ('erp_period_close', 'remark'),
    ('erp_period_close', 'close_time'),
    ('erp_period_close', 'close_user'),
    ('erp_period_close', 'creator'),
    ('erp_period_close', 'create_time'),
    ('erp_period_close', 'updater'),
    ('erp_period_close', 'update_time'),
    ('erp_period_close', 'deleted'),
    ('erp_period_close', 'tenant_id'),
    ('ai_nl2sql_query_history', 'id'),
    ('ai_nl2sql_query_history', 'natural_language'),
    ('ai_nl2sql_query_history', 'sql'),
    ('ai_nl2sql_query_history', 'data_source'),
    ('ai_nl2sql_query_history', 'status'),
    ('ai_nl2sql_query_history', 'row_count'),
    ('ai_nl2sql_query_history', 'error_msg'),
    ('ai_nl2sql_query_history', 'cost_ms'),
    ('ai_nl2sql_query_history', 'creator'),
    ('ai_nl2sql_query_history', 'create_time'),
    ('ai_nl2sql_query_history', 'updater'),
    ('ai_nl2sql_query_history', 'update_time'),
    ('ai_nl2sql_query_history', 'deleted'),
    ('ai_nl2sql_query_history', 'tenant_id');

SELECT e.table_name,
       e.column_name,
       CASE WHEN c.column_name IS NULL THEN 'MISSING' ELSE 'OK' END AS column_verdict
  FROM tmp_v98_expected_column e
  LEFT JOIN information_schema.columns c
         ON c.table_schema = DATABASE()
        AND c.table_name = e.table_name
        AND c.column_name = e.column_name
 ORDER BY e.table_name, e.column_name;

-- ------------------------------------------------------------
-- Q2. 同构组列数核对（两份 CREATE TABLE 定义逐字节相同，只需核对列数）
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v98_expected_count;
CREATE TEMPORARY TABLE tmp_v98_expected_count (
    table_name            VARCHAR(64) NOT NULL,
    expected_column_count INT NOT NULL,
    PRIMARY KEY (table_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v98_expected_count (table_name, expected_column_count) VALUES
    ('qms_inspection_item', 18),
    ('qms_inspection_order', 17),
    ('qms_inspection_record', 14),
    ('qms_capa_document', 19),
    ('aimultiagent_topology', 14),
    ('aimultiagent_execution_log', 17),
    ('wms_wave_order', 18),
    ('wms_wave_order_detail', 13);

SELECT e.table_name,
       e.expected_column_count,
       COUNT(c.column_name) AS actual_column_count,
       CASE WHEN COUNT(c.column_name) = 0 THEN 'MISSING'
            WHEN COUNT(c.column_name) <> e.expected_column_count THEN 'WRONG'
            ELSE 'OK' END AS count_verdict
  FROM tmp_v98_expected_count e
  LEFT JOIN information_schema.columns c
         ON c.table_schema = DATABASE()
        AND c.table_name = e.table_name
 GROUP BY e.table_name, e.expected_column_count
 ORDER BY e.table_name;

-- ------------------------------------------------------------
-- Q3. 主键自增核对（V10 结构生效时 id 没有 AUTO_INCREMENT）
-- ------------------------------------------------------------
SELECT table_name,
       COLUMN_TYPE AS id_type,
       COLUMN_KEY  AS id_key,
       IFNULL(EXTRA, '') AS id_extra,
       CASE WHEN COLUMN_KEY <> 'PRI' THEN 'NOT_PK'
            WHEN IFNULL(EXTRA, '') LIKE '%auto_increment%' THEN 'OK'
            ELSE 'MISSING_AUTO_INCREMENT' END AS auto_increment_verdict
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name IN ('erp_period', 'erp_period_close')
   AND column_name = 'id'
 ORDER BY table_name;

-- ------------------------------------------------------------
-- Q4. 索引核对
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v98_expected_index;
CREATE TEMPORARY TABLE tmp_v98_expected_index (
    table_name VARCHAR(64) NOT NULL,
    index_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (table_name, index_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v98_expected_index (table_name, index_name) VALUES
    ('erp_period', 'idx_erp_period_tenant_code'),
    ('erp_period', 'idx_erp_period_year_month'),
    ('erp_period_close', 'idx_erp_period_close_period_code');

SELECT e.table_name,
       e.index_name,
       CASE WHEN s.index_name IS NULL THEN 'MISSING' ELSE 'OK' END AS index_verdict
  FROM tmp_v98_expected_index e
  LEFT JOIN information_schema.statistics s
         ON s.table_schema = DATABASE()
        AND s.table_name = e.table_name
        AND s.index_name = e.index_name
 GROUP BY e.table_name, e.index_name, s.index_name
 ORDER BY e.table_name, e.index_name;

-- 延后迁移的唯一键：此处只报告状态，不参与 PASS/FAIL
SELECT 'uk_tenant_code(erp_period)' AS deferred_key,
       CASE WHEN COUNT(*) = 0 THEN 'MISSING' ELSE 'EXISTS' END AS key_state
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND table_name = 'erp_period'
   AND index_name = 'uk_tenant_code';

-- ------------------------------------------------------------
-- Q5. 回填残留统计（不参与判定，但必须人工清零）
-- ------------------------------------------------------------
SELECT 'erp_period.year/month' AS residual_item,
       COUNT(*) AS residual_rows
  FROM erp_period
 WHERE year IS NULL OR month IS NULL;

SELECT 'erp_period.start_date/end_date' AS residual_item,
       COUNT(*) AS residual_rows
  FROM erp_period
 WHERE start_date IS NULL OR end_date IS NULL;

SELECT 'erp_period_close.period_code' AS residual_item,
       COUNT(*) AS residual_rows
  FROM erp_period_close
 WHERE period_code IS NULL;

-- ------------------------------------------------------------
-- Q6. 总判定
SELECT CASE
         WHEN EXISTS (SELECT 1
                        FROM tmp_v98_expected_column e
                        LEFT JOIN information_schema.columns c
                               ON c.table_schema = DATABASE()
                              AND c.table_name = e.table_name
                              AND c.column_name = e.column_name
                       WHERE c.column_name IS NULL) THEN 'FAIL'
         WHEN EXISTS (SELECT 1
                        FROM tmp_v98_expected_count e
                        LEFT JOIN information_schema.columns c
                               ON c.table_schema = DATABASE()
                              AND c.table_name = e.table_name
                       GROUP BY e.table_name, e.expected_column_count
                      HAVING COUNT(c.column_name) <> e.expected_column_count) THEN 'FAIL'
         WHEN EXISTS (SELECT 1
                        FROM information_schema.columns
                       WHERE table_schema = DATABASE()
                         AND table_name IN ('erp_period', 'erp_period_close')
                         AND column_name = 'id'
                         AND IFNULL(EXTRA, '') NOT LIKE '%auto_increment%') THEN 'FAIL'
         WHEN EXISTS (SELECT 1
                        FROM tmp_v98_expected_index e
                        LEFT JOIN information_schema.statistics s
                               ON s.table_schema = DATABASE()
                              AND s.table_name = e.table_name
                              AND s.index_name = e.index_name
                       WHERE s.index_name IS NULL) THEN 'FAIL'
         ELSE 'PASS'
       END AS v98_verify_verdict;

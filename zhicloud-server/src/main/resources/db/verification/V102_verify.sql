-- ============================================================
-- V102 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对模拟映射行已按预期组数灌入（PRODUCT->SKU 13 / WAREHOUSE->WAREHOUSE 3 / CUSTOMER->MERCHANT 2）。
--   2. 核对没有重复键、没有非 VALID 行（模拟数据应当是干净生效的）。
--   3. 给出 PASS/FAIL 门禁结论。
--
-- 说明：
--   * 本脚本只用于演示/预发环境核对模拟数据；生产环境不应存在 V102 的数据，
--     若生产上本脚本返回「全部缺失」是正确的。
-- ============================================================

-- ------------------------------------------------------------
-- Q1. 分组行数核对：期望 vs 实际
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v102_verify_expected_group;
CREATE TEMPORARY TABLE tmp_v102_verify_expected_group (
    source_entity VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    target_entity VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    expected_rows INT         NOT NULL
);

INSERT INTO tmp_v102_verify_expected_group (source_entity, target_entity, expected_rows)
VALUES
    ('PRODUCT',   'SKU',       13),
    ('WAREHOUSE', 'WAREHOUSE', 3),
    ('CUSTOMER',  'MERCHANT',  2);

SELECT e.source_entity,
       e.target_entity,
       e.expected_rows,
       COUNT(r.id) AS actual_rows,
       CASE WHEN COUNT(r.id) = e.expected_rows THEN 'OK' ELSE 'MISMATCH' END AS row_verdict
  FROM tmp_v102_verify_expected_group e
  LEFT JOIN mdm_cross_ref r
         ON r.deleted = 0
        AND r.source_system = 'ERP'
        AND r.target_system = 'WMS'
        AND r.source_entity = e.source_entity
        AND r.target_entity = e.target_entity
        AND r.status = 'VALID'
 GROUP BY e.source_entity, e.target_entity, e.expected_rows
 ORDER BY e.source_entity;

-- ------------------------------------------------------------
-- Q2. 重复键与非 VALID 行检查（唯一键已保证重复，这里用于人工确认）
-- ------------------------------------------------------------
SELECT r.source_entity,
       r.target_entity,
       r.source_id,
       COUNT(*) AS row_count,
       CASE WHEN COUNT(*) > 1 THEN 'DUPLICATE' ELSE 'OK' END AS duplicate_verdict
  FROM mdm_cross_ref r
 WHERE r.deleted = 0
 GROUP BY r.tenant_id, r.source_system, r.source_entity, r.source_id, r.target_system, r.target_entity
HAVING COUNT(*) > 1;

SELECT SUM(IF(r.status <> 'VALID', 1, 0)) AS invalid_rows,
       SUM(IF(r.status = 'VALID', 1, 0))  AS valid_rows,
       MIN(r.id)                          AS min_id,
       MAX(r.id)                          AS max_id
  FROM mdm_cross_ref r
 WHERE r.deleted = 0;

-- ------------------------------------------------------------
-- Q3. 后检汇总：分组行数全部一致且无重复键时 PASS。
--     PASS 仅代表「模拟数据已按预期灌入」，不代表这些 id 在你的库里真实存在。
-- ------------------------------------------------------------
WITH row_gate AS (
    SELECT IFNULL(SUM(IF(x.actual_rows <> x.expected_rows, 1, 0)), 0) AS mismatch_groups
      FROM (
            SELECT e.source_entity,
                   e.target_entity,
                   e.expected_rows,
                   COUNT(r.id) AS actual_rows
              FROM tmp_v102_verify_expected_group e
              LEFT JOIN mdm_cross_ref r
                     ON r.deleted = 0
                    AND r.source_system = 'ERP'
                    AND r.target_system = 'WMS'
                    AND r.source_entity = e.source_entity
                    AND r.target_entity = e.target_entity
                    AND r.status = 'VALID'
             GROUP BY e.source_entity, e.target_entity, e.expected_rows
           ) x
), dup_gate AS (
    SELECT COUNT(*) AS duplicate_groups
      FROM (
            SELECT r.source_entity, r.target_entity, r.source_id
              FROM mdm_cross_ref r
             WHERE r.deleted = 0
             GROUP BY r.tenant_id, r.source_system, r.source_entity, r.source_id, r.target_system, r.target_entity
            HAVING COUNT(*) > 1
           ) d
)
SELECT rg.mismatch_groups,
       dg.duplicate_groups,
       CASE WHEN rg.mismatch_groups = 0 AND dg.duplicate_groups = 0
            THEN 'PASS' ELSE 'FAIL' END AS v102_verify_verdict
  FROM row_gate rg, dup_gate dg;

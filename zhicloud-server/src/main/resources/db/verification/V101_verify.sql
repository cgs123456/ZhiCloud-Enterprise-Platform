-- ============================================================
-- V101 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对 mdm_cross_ref 的结构、唯一键与索引已就位。
--   2. 只读体检：观察映射覆盖情况与失效映射数量。
--   3. 给出 PASS/FAIL 门禁结论，供预生产/生产执行前后人工复核使用。
--
-- 说明：
--   * 结构门禁（Q1/Q2）是硬门禁：缺表、缺列、缺唯一键都必须 FAIL。
--   * 运行观察（Q3）刻意不作为 FAIL 条件：映射行数为 0 在「尚未维护主数据」时是正常的，
--     但是否异常取决于业务是否已接入集成链路——真正需要告警的是「链路已接入但映射为 0」，
--     应接入监控而不是靠本脚本的静态阈值判断。
-- ============================================================

-- ------------------------------------------------------------
-- Q1. 结构与列校验：期望列 vs information_schema
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v101_verify_expected_column;
CREATE TEMPORARY TABLE tmp_v101_verify_expected_column (
    table_name        VARCHAR(64)  NOT NULL,
    column_name       VARCHAR(64)  NOT NULL,
    expected_type     VARCHAR(64)  NOT NULL,
    expected_nullable VARCHAR(3)   NOT NULL
);

INSERT INTO tmp_v101_verify_expected_column
    (table_name, column_name, expected_type, expected_nullable)
VALUES
    ('mdm_cross_ref', 'id',            'bigint',      'NO'),
    ('mdm_cross_ref', 'tenant_id',     'bigint',      'NO'),
    ('mdm_cross_ref', 'source_system', 'varchar(32)', 'NO'),
    ('mdm_cross_ref', 'source_entity', 'varchar(32)', 'NO'),
    ('mdm_cross_ref', 'source_id',     'varchar(64)', 'NO'),
    ('mdm_cross_ref', 'target_system', 'varchar(32)', 'NO'),
    ('mdm_cross_ref', 'target_entity', 'varchar(32)', 'NO'),
    ('mdm_cross_ref', 'target_id',     'varchar(64)', 'NO'),
    ('mdm_cross_ref', 'status',        'varchar(16)', 'NO'),
    ('mdm_cross_ref', 'remark',        'varchar(500)','YES'),
    ('mdm_cross_ref', 'deleted',       'bit',         'NO');

SELECT @v101_verify_expected_column_count := COUNT(*)
  FROM tmp_v101_verify_expected_column;

SELECT e.table_name,
       e.column_name,
       e.expected_type,
       c.COLUMN_TYPE   AS actual_type,
       c.IS_NULLABLE   AS actual_nullable,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                 OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG'
            ELSE 'OK' END AS column_verdict
  FROM tmp_v101_verify_expected_column e
  LEFT JOIN information_schema.columns c
         ON c.TABLE_SCHEMA = DATABASE()
        AND c.TABLE_NAME = e.table_name
        AND c.COLUMN_NAME = e.column_name
 WHERE c.COLUMN_NAME IS NULL
    OR LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
    OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)
 ORDER BY e.table_name, e.column_name;

-- ------------------------------------------------------------
-- Q2. 唯一键与索引校验：去重能力与扫描路径必须存在
--    uk_cross_ref     ：同一源实体在同一目标系统内只有一个当前映射
--    idx_tenant_source：解析路径（正向查询）
--    idx_target       ：反向追溯路径（给定 WMS SKU 反查 ERP 产品）
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v101_verify_expected_index;
CREATE TEMPORARY TABLE tmp_v101_verify_expected_index (
    table_name     VARCHAR(64) NOT NULL,
    index_name     VARCHAR(64) NOT NULL,
    column_names   VARCHAR(255) NOT NULL
);

INSERT INTO tmp_v101_verify_expected_index (table_name, index_name, column_names)
VALUES
    ('mdm_cross_ref', 'uk_cross_ref',     'tenant_id,source_system,source_entity,source_id,target_system,target_entity'),
    ('mdm_cross_ref', 'idx_tenant_source','tenant_id,source_system,source_entity,source_id'),
    ('mdm_cross_ref', 'idx_target',       'target_system,target_entity,target_id');

SELECT e.table_name,
       e.index_name,
       e.column_names AS expected_columns,
       s.INDEX_NAME   AS actual_index,
       GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) AS actual_columns,
       CASE WHEN s.INDEX_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(REPLACE(GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX), ' ', ''))
                 <> LOWER(REPLACE(e.column_names, ' ', '')) THEN 'WRONG'
            ELSE 'OK' END AS index_verdict
  FROM tmp_v101_verify_expected_index e
  LEFT JOIN information_schema.statistics s
         ON s.TABLE_SCHEMA = DATABASE()
        AND s.TABLE_NAME = e.table_name
        AND s.INDEX_NAME = e.index_name
 GROUP BY e.table_name, e.index_name, e.column_names, s.INDEX_NAME
 ORDER BY e.table_name, e.index_name;

-- ------------------------------------------------------------
-- Q3. 运行观察（只读，不参与 PASS/FAIL）：
--     映射覆盖量与失效量应接入监控；「链路已接入但映射为 0」才是异常。
-- ------------------------------------------------------------
SELECT r.source_system,
       r.source_entity,
       r.target_system,
       r.target_entity,
       SUM(IF(r.status = 'VALID', 1, 0))   AS valid_count,
       SUM(IF(r.status <> 'VALID', 1, 0))  AS invalid_count,
       MIN(r.create_time)                  AS first_created_at,
       MAX(r.update_time)                  AS last_updated_at
  FROM mdm_cross_ref r
 WHERE r.deleted = 0
 GROUP BY r.source_system, r.source_entity, r.target_system, r.target_entity
 ORDER BY valid_count DESC, r.source_system, r.source_entity;

-- ------------------------------------------------------------
-- Q4. 后检汇总：结构与索引全为 0 异常时 PASS。
--     PASS 仅代表「结构正确」，不代表映射已维护、链路已接入或映射无冲突。
-- ------------------------------------------------------------
WITH column_gate AS (
    SELECT SUM(IF(c.COLUMN_NAME IS NULL, 1, 0)) AS missing_columns,
           SUM(IF(c.COLUMN_NAME IS NOT NULL
                  AND (LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                       OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)), 1, 0)) AS wrong_columns
      FROM tmp_v101_verify_expected_column e
      LEFT JOIN information_schema.columns c
             ON c.TABLE_SCHEMA = DATABASE()
            AND c.TABLE_NAME = e.table_name
            AND c.COLUMN_NAME = e.column_name
), index_gate AS (
    SELECT SUM(IF(s.INDEX_NAME IS NULL, 1, 0)) AS missing_indexes
      FROM tmp_v101_verify_expected_index e
      LEFT JOIN information_schema.statistics s
             ON s.TABLE_SCHEMA = DATABASE()
            AND s.TABLE_NAME = e.table_name
            AND s.INDEX_NAME = e.index_name
)
SELECT @v101_verify_expected_column_count AS expected_columns,
       cg.missing_columns,
       cg.wrong_columns,
       ig.missing_indexes,
       CASE WHEN cg.missing_columns = 0
                  AND cg.wrong_columns = 0
                  AND ig.missing_indexes = 0
            THEN 'PASS' ELSE 'FAIL' END AS v101_verify_verdict
  FROM column_gate cg, index_gate ig;

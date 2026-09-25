-- ============================================================
-- V100 人工复核脚本（只读业务数据，不改业务数据）
--
-- 目标：
--   1. 核对 event_outbox / event_inbox 两张表的结构、唯一键与索引已就位。
--   2. 只读体检：观察 Outbox 堆积、死信、失败消费与最老未投递事件年龄。
--   3. 给出 PASS/FAIL 门禁结论，供预生产/生产执行前后人工复核使用。
--
-- 说明：
--   * 结构门禁（Q1/Q2）是硬门禁：缺表、缺列、缺唯一键都必须 FAIL。
--   * 运行观察（Q3）刻意不作为 FAIL 条件：NEW 事件短暂堆积是正常现象，
--     是否异常取决于业务峰值与调度周期，不能用静态阈值误判。真正需要
--     告警的是「最老未投递事件年龄」与 DEAD 数量，应接入监控而不是本脚本。
-- ============================================================

-- ------------------------------------------------------------
-- Q1. 结构与列校验：期望列 vs information_schema
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v100_verify_expected_column;
CREATE TEMPORARY TABLE tmp_v100_verify_expected_column (
    table_name        VARCHAR(64)  NOT NULL,
    column_name       VARCHAR(64)  NOT NULL,
    expected_type     VARCHAR(64)  NOT NULL,
    expected_nullable VARCHAR(3)   NOT NULL
);

INSERT INTO tmp_v100_verify_expected_column
    (table_name, column_name, expected_type, expected_nullable)
VALUES
    ('event_outbox', 'id',             'bigint',       'NO'),
    ('event_outbox', 'event_id',       'varchar(64)',  'NO'),
    ('event_outbox', 'tenant_id',      'bigint',       'NO'),
    ('event_outbox', 'aggregate_type', 'varchar(64)',  'NO'),
    ('event_outbox', 'aggregate_id',   'varchar(64)',  'NO'),
    ('event_outbox', 'event_type',     'varchar(128)', 'NO'),
    ('event_outbox', 'schema_version', 'varchar(16)',  'NO'),
    ('event_outbox', 'partition_key',  'varchar(128)', 'NO'),
    ('event_outbox', 'payload',        'longtext',     'NO'),
    ('event_outbox', 'status',         'varchar(16)',  'NO'),
    ('event_outbox', 'retry_count',    'int',          'NO'),
    ('event_outbox', 'next_retry_at',  'datetime',     'YES'),
    ('event_outbox', 'occurred_at',    'datetime',     'NO'),
    ('event_outbox', 'published_at',   'datetime',     'YES'),
    ('event_outbox', 'trace_id',       'varchar(64)',  'YES'),
    ('event_outbox', 'last_error',     'varchar(2000)','YES'),
    ('event_outbox', 'deleted',        'bit',          'NO'),
    ('event_inbox',  'id',             'bigint',       'NO'),
    ('event_inbox',  'tenant_id',      'bigint',       'NO'),
    ('event_inbox',  'consumer_name',  'varchar(64)',  'NO'),
    ('event_inbox',  'event_id',       'varchar(64)',  'NO'),
    ('event_inbox',  'event_type',     'varchar(128)', 'NO'),
    ('event_inbox',  'status',         'varchar(16)',  'NO'),
    ('event_inbox',  'retry_count',    'int',          'NO'),
    ('event_inbox',  'received_at',    'datetime',     'NO'),
    ('event_inbox',  'processed_at',   'datetime',     'YES'),
    ('event_inbox',  'error',          'varchar(2000)','YES'),
    ('event_inbox',  'deleted',        'bit',          'NO');

SELECT @v100_verify_expected_column_count := COUNT(*)
  FROM tmp_v100_verify_expected_column;

SELECT e.table_name,
       e.column_name,
       e.expected_type,
       c.COLUMN_TYPE   AS actual_type,
       c.IS_NULLABLE   AS actual_nullable,
       CASE WHEN c.COLUMN_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                 OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable) THEN 'WRONG'
            ELSE 'OK' END AS column_verdict
  FROM tmp_v100_verify_expected_column e
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
--    uk_event_id        ：outbox 事件唯一，重放与幂等的落库保证
--    uk_consumer_event  ：inbox 同消费者同事件只处理一次
--    idx_status_next_retry：投递扫描路径，缺失会导致全表扫描
-- ------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_v100_verify_expected_index;
CREATE TEMPORARY TABLE tmp_v100_verify_expected_index (
    table_name     VARCHAR(64) NOT NULL,
    index_name     VARCHAR(64) NOT NULL,
    column_names   VARCHAR(255) NOT NULL
);

INSERT INTO tmp_v100_verify_expected_index (table_name, index_name, column_names)
VALUES
    ('event_outbox', 'uk_event_id',          'event_id'),
    ('event_outbox', 'idx_status_next_retry','status,next_retry_at,id'),
    ('event_outbox', 'idx_tenant_aggregate', 'tenant_id,aggregate_type,aggregate_id,occurred_at'),
    ('event_inbox',  'uk_consumer_event',    'consumer_name,event_id'),
    ('event_inbox',  'idx_status_processed', 'status,processed_at'),
    ('event_inbox',  'idx_tenant_event',     'tenant_id,event_id');

SELECT e.table_name,
       e.index_name,
       e.column_names AS expected_columns,
       s.INDEX_NAME   AS actual_index,
       GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) AS actual_columns,
       CASE WHEN s.INDEX_NAME IS NULL THEN 'MISSING'
            WHEN LOWER(REPLACE(GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX), ' ', ''))
                 <> LOWER(REPLACE(e.column_names, ' ', '')) THEN 'WRONG'
            ELSE 'OK' END AS index_verdict
  FROM tmp_v100_verify_expected_index e
  LEFT JOIN information_schema.statistics s
         ON s.TABLE_SCHEMA = DATABASE()
        AND s.TABLE_NAME = e.table_name
        AND s.INDEX_NAME = e.index_name
 GROUP BY e.table_name, e.index_name, e.column_names, s.INDEX_NAME
 ORDER BY e.table_name, e.index_name;

-- ------------------------------------------------------------
-- Q3. 运行观察（只读，不参与 PASS/FAIL）：
--     堆积、死信、失败消费与最老未投递事件年龄应接入监控告警。
-- ------------------------------------------------------------
SELECT SUM(IF(o.status = 'NEW', 1, 0))                       AS pending_new,
       SUM(IF(o.status = 'PUBLISHING', 1, 0))                AS publishing,
       SUM(IF(o.status = 'DEAD', 1, 0))                      AS dead_letter,
       SUM(IF(o.next_retry_at IS NOT NULL
              AND o.next_retry_at > NOW()
              AND o.status IN ('NEW', 'PUBLISHING'), 1, 0))  AS waiting_backoff,
       MIN(CASE WHEN o.status IN ('NEW', 'PUBLISHING')
                THEN o.occurred_at END)                      AS oldest_pending_occurred_at,
       TIMESTAMPDIFF(MINUTE,
                     MIN(CASE WHEN o.status IN ('NEW', 'PUBLISHING')
                              THEN o.occurred_at END),
                     NOW())                                  AS oldest_pending_age_minutes,
       COUNT(*)                                              AS outbox_total
  FROM event_outbox o
 WHERE o.deleted = 0;

SELECT i.consumer_name,
       SUM(IF(i.status = 'PROCESSED', 1, 0)) AS processed,
       SUM(IF(i.status = 'FAILED', 1, 0))    AS failed,
       SUM(IF(i.status = 'DEAD', 1, 0))      AS dead,
       MAX(i.received_at)                    AS last_received_at
  FROM event_inbox i
 WHERE i.deleted = 0
 GROUP BY i.consumer_name
 ORDER BY failed DESC, i.consumer_name;

-- ------------------------------------------------------------
-- Q4. 后检汇总：结构与索引全为 0 异常时 PASS。
--     PASS 仅代表「结构正确」，不代表已有链路接入、调度在跑或事件无堆积。
-- ------------------------------------------------------------
WITH column_gate AS (
    SELECT SUM(IF(c.COLUMN_NAME IS NULL, 1, 0)) AS missing_columns,
           SUM(IF(c.COLUMN_NAME IS NOT NULL
                  AND (LOWER(c.COLUMN_TYPE) <> LOWER(e.expected_type)
                       OR UPPER(c.IS_NULLABLE) <> UPPER(e.expected_nullable)), 1, 0)) AS wrong_columns
      FROM tmp_v100_verify_expected_column e
      LEFT JOIN information_schema.columns c
             ON c.TABLE_SCHEMA = DATABASE()
            AND c.TABLE_NAME = e.table_name
            AND c.COLUMN_NAME = e.column_name
), index_gate AS (
    SELECT SUM(IF(s.INDEX_NAME IS NULL, 1, 0)) AS missing_indexes
      FROM tmp_v100_verify_expected_index e
      LEFT JOIN information_schema.statistics s
             ON s.TABLE_SCHEMA = DATABASE()
            AND s.TABLE_NAME = e.table_name
            AND s.INDEX_NAME = e.index_name
)
SELECT @v100_verify_expected_column_count AS expected_columns,
       cg.missing_columns,
       cg.wrong_columns,
       ig.missing_indexes,
       CASE WHEN cg.missing_columns = 0
                  AND cg.wrong_columns = 0
                  AND ig.missing_indexes = 0
            THEN 'PASS' ELSE 'FAIL' END AS v100_verify_verdict
  FROM column_gate cg, index_gate ig;

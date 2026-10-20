-- ============================================================
-- V218 验证脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 迁移 V218 已在 flyway_schema_history 成功记录；
--   2. Flowable 引擎核心表已存在（BPM 引擎 + 历史 + IDM）；
--   3. Flowable schema 版本行已写入（否则引擎启动会判定版本不匹配而报错）。
--
-- 说明：prod profile 禁用自动 DDL，Flowable 表只能由迁移链建出来；
--      缺表时应用启动直接失败，因此这里做成部署期硬门禁。
-- ============================================================

SELECT 'V218_FLYWAY_APPLIED' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM flyway_schema_history
                  WHERE version = '218' AND success = 1) = 1
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V218_FLOWABLE_CORE_TABLES' AS check_name,
       CASE
           WHEN (SELECT COUNT(DISTINCT table_name) FROM information_schema.tables
                  WHERE table_schema = DATABASE()
                    AND table_name IN ('ACT_GE_PROPERTY', 'ACT_RE_PROCDEF', 'ACT_RU_TASK',
                                       'ACT_RU_EXECUTION', 'ACT_RU_IDENTITYLINK', 'ACT_HI_PROCINST')) = 6
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V218_FLOWABLE_SCHEMA_VERSION' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM ACT_GE_PROPERTY WHERE NAME_ = 'schema.version') >= 1
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

SELECT 'V218_FLOWABLE_ENGINE_TABLES' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM information_schema.tables
                  WHERE table_schema = DATABASE()
                    AND (table_name LIKE 'ACT\_%' OR table_name LIKE 'FLW\_%')) >= 40
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;

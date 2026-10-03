-- 沙箱只读健康检查：仅查询系统变量与 information_schema，不做任何写入。
-- 输出为 TSV（metric<TAB>value），由 scripts/production/mysql-sandbox.ps1 解析成证据。
SELECT 'server_version' AS metric, VERSION() AS value
UNION ALL SELECT 'character_set_server', @@character_set_server
UNION ALL SELECT 'collation_server', @@collation_server
UNION ALL SELECT 'collation_connection', @@collation_connection
UNION ALL SELECT 'collation_database', @@collation_database
UNION ALL SELECT 'session_time_zone', @@session.time_zone
UNION ALL SELECT 'global_time_zone', @@global.time_zone
UNION ALL SELECT 'system_time_zone', @@system_time_zone
UNION ALL SELECT 'now_utc_offset', TIMEDIFF(NOW(), UTC_TIMESTAMP())
UNION ALL SELECT 'sql_mode', @@sql_mode
UNION ALL SELECT 'default_storage_engine', @@default_storage_engine
UNION ALL SELECT 'innodb_default_row_format', @@innodb_default_row_format
UNION ALL SELECT 'lower_case_table_names', @@lower_case_table_names
UNION ALL SELECT 'transaction_isolation', @@transaction_isolation
UNION ALL SELECT 'log_bin', @@log_bin
UNION ALL SELECT 'sandbox_database_present', IFNULL((
    SELECT 'yes' FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = 'zhicloud_sandbox' LIMIT 1
), 'no')
UNION ALL SELECT 'read_only', @@read_only;

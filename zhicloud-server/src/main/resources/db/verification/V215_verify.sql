-- V215 验证：mp_message.event / event_key 存在且类型符合 MpMessageDO
-- 迁移执行自检；生产仍需要备份、回滚演练与独立复核。

-- 1. Flyway 成功记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '215' AND success = 1) = 1
            THEN 'V215_FLYWAY_APPLIED PASS'
            ELSE 'V215_FLYWAY_APPLIED FAIL 版本 215 不在成功记录中' END AS check_result;

-- 2. 两列存在
SELECT CASE WHEN COUNT(*) = 2 THEN 'V215_MP_MESSAGE_EVENT_COLUMNS PASS'
            ELSE CONCAT('V215_MP_MESSAGE_EVENT_COLUMNS FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'mp_message'
   AND COLUMN_NAME IN ('event', 'event_key');

-- 3. 类型与可空性符合预期
SELECT CASE WHEN COUNT(*) = 2 THEN 'V215_MP_MESSAGE_EVENT_TYPES PASS'
            ELSE CONCAT('V215_MP_MESSAGE_EVENT_TYPES FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE()
   AND TABLE_NAME = 'mp_message'
   AND (
        (COLUMN_NAME = 'event' AND COLUMN_TYPE = 'varchar(64)' AND IS_NULLABLE = 'YES')
     OR (COLUMN_NAME = 'event_key' AND COLUMN_TYPE = 'varchar(255)' AND IS_NULLABLE = 'YES')
   );


-- V147 验证：Quartz 表纳入 Flyway 的行为断言
-- 1. Flyway 通道记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '147' AND success = 1) = 1
            THEN 'V147_FLYWAY_APPLIED PASS'
            ELSE 'V147_FLYWAY_APPLIED FAIL' END AS check_result;
-- 2. iot 调度器真正需要的锁表在（QRTZ_LOCKS）
SELECT CASE WHEN (SELECT COUNT(*) FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'QRTZ_LOCKS') = 1
            THEN 'V147_QRTZ_LOCKS_PASS'
            ELSE 'V147_QRTZ_LOCKS FAIL' END AS check_result;
-- 3. 商业域过期 cron 种子不在
SELECT CASE WHEN (SELECT COUNT(*) FROM QRTZ_CRON_TRIGGERS
                   WHERE TRIGGER_NAME IN ('payNotifyJob','tradeOrderAutoCancelJob',
                                          'brokerageRecordUnfreezeJob')) = 0
            THEN 'V147_NO_COMMERCIAL_CRON PASS'
            ELSE 'V147_NO_COMMERCIAL_CRON FAIL' END AS check_result;

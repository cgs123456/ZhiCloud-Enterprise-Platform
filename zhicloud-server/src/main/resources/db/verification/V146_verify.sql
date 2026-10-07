-- V146 验证：商业域调度任务清理的行为断言
-- 前置：全新库上 infra_job 原 18 条（含 12 条商业域孤儿），V146 应删 12 留 6。

-- 1. Flyway 通道记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '146' AND success = 1) = 1
            THEN 'V146_FLYWAY_APPLIED PASS'
            ELSE 'V146_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. 残留 infra_job 只剩现存的 6 个 handler（无 pay/trade/brokerage/combination/coupon/product）
SELECT CASE WHEN (SELECT COUNT(*) FROM infra_job WHERE deleted=b'0') = 6
            THEN 'V146_JOBS_SURVIVOR_COUNT PASS 6'
            ELSE CONCAT('V146_JOBS_SURVIVOR_COUNT FAIL 剩 ', (SELECT COUNT(*) FROM infra_job WHERE deleted=b'0')) END AS check_result;

-- 3. 现存 handler 名单精确匹配（防误删或漏删）
SELECT CASE WHEN (SELECT GROUP_CONCAT(handler_name ORDER BY handler_name) FROM infra_job WHERE deleted=b'0')
                 = 'accessLogCleanJob,demoJob,errorLogCleanJob,iotDeviceOfflineCheckJob,iotOtaUpgradeJob,jobLogCleanJob'
            THEN 'V146_SURVIVOR_SET_EXACT PASS'
            ELSE CONCAT('V146_SURVIVOR_SET_EXACT FAIL [',
                        (SELECT GROUP_CONCAT(handler_name ORDER BY handler_name) FROM infra_job WHERE deleted=b'0'), ']') END AS check_result;

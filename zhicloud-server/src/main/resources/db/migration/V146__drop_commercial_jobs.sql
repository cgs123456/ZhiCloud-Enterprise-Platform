-- ============================================================
-- V146：清理 V140 商业域移除的漏网残留——infra_job 里的 pay/mall 调度任务
-- ============================================================
-- V140 移除了 pay/member/mall 三个业务模块，但 infra_job（调度任务配置表）里的
-- 12 条对应任务没清：handler_name 指向的 JobHandler bean 已随模块从代码里删除。
-- 这些是"点了会 404/找不到 bean"的孤儿配置。
-- 清单来源（对账于 2026-10-07，全仓 grep implements JobHandler 的现存 16 个 handler 比对）：
--   pay* 5 条：payNotifyJob / payOrderSyncJob / payOrderExpireJob / payRefundSyncJob / payTransferSyncJob
--   mall* 7 条：tradeOrderAutoCancelJob / tradeOrderAutoReceiveJob / tradeOrderAutoCommentJob /
--               brokerageRecordUnfreezeJob / combinationRecordExpireJob / couponExpireJob / productStatisticsJob
-- 现存且保留：accessLogCleanJob / errorLogCleanJob / jobLogCleanJob / demoJob（infra/system 清理与示例）、
--             iotDeviceOfflineCheckJob / iotOtaUpgradeJob（IoT）、以及 CRM/IM/WMS 的新任务（未在种子中，按需另加）。
--
-- 与 §12.127「支付通知只有第一次」裁决项的关系：pay 模块已整体移除，重投机制随模块消失，
-- 不再需要为它实现重投——本迁移是把那条裁决项的残留面一并清掉，而非放开调度器。
-- 删除面（infra_job + 按 handler_name 关联的 infra_job_log 历史），不影响其它任务；
-- 表无租户列（infra_job 按 handler_name 唯一定位），全清即可。
-- ============================================================

DELETE FROM infra_job_log WHERE handler_name IN (
    'payNotifyJob', 'payOrderSyncJob', 'payOrderExpireJob', 'payRefundSyncJob',
    'payTransferSyncJob', 'tradeOrderAutoCancelJob', 'tradeOrderAutoReceiveJob',
    'tradeOrderAutoCommentJob', 'brokerageRecordUnfreezeJob', 'combinationRecordExpireJob',
    'couponExpireJob', 'productStatisticsJob');

DELETE FROM infra_job WHERE handler_name IN (
    'payNotifyJob', 'payOrderSyncJob', 'payOrderExpireJob', 'payRefundSyncJob',
    'payTransferSyncJob', 'tradeOrderAutoCancelJob', 'tradeOrderAutoReceiveJob',
    'tradeOrderAutoCommentJob', 'brokerageRecordUnfreezeJob', 'combinationRecordExpireJob',
    'couponExpireJob', 'productStatisticsJob');

-- 自检 1：商业域调度任务清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V146_NO_COMMERCIAL_JOBS PASS'
            ELSE CONCAT('V146_NO_COMMERCIAL_JOBS FAIL ', COUNT(*)) END AS check_result
  FROM infra_job
 WHERE deleted = b'0'
   AND ( handler_name LIKE 'pay%'
         OR handler_name LIKE 'tradeOrderAuto%'
         OR handler_name LIKE 'brokerage%'
         OR handler_name LIKE 'combination%'
         OR handler_name IN ('couponExpireJob', 'productStatisticsJob') );

-- 自检 2：现存 handler 的任务还在（IoT/infra/system 不应被误删）
SELECT CASE WHEN COUNT(*) >= 6 THEN CONCAT('V146_SURVIVOR_JOBS_PASS ', COUNT(*))
            ELSE CONCAT('V146_SURVIVOR_JOBS_FAIL ', COUNT(*)) END AS check_result
  FROM infra_job
 WHERE deleted = b'0'
   AND handler_name IN ('accessLogCleanJob', 'errorLogCleanJob', 'jobLogCleanJob',
                        'demoJob', 'iotDeviceOfflineCheckJob', 'iotOtaUpgradeJob');

-- 自检 3：job_log 里也没有这些 handler 的残留
SELECT CASE WHEN COUNT(*) = 0 THEN 'V146_NO_ORPHAN_JOB_LOGS PASS'
            ELSE CONCAT('V146_NO_ORPHAN_JOB_LOGS FAIL ', COUNT(*)) END AS check_result
  FROM infra_job_log
 WHERE deleted = b'0'
   AND handler_name IN ('payNotifyJob', 'payOrderSyncJob', 'payOrderExpireJob', 'payRefundSyncJob',
                        'payTransferSyncJob', 'tradeOrderAutoCancelJob', 'tradeOrderAutoReceiveJob',
                        'tradeOrderAutoCommentJob', 'brokerageRecordUnfreezeJob', 'combinationRecordExpireJob',
                        'couponExpireJob', 'productStatisticsJob');

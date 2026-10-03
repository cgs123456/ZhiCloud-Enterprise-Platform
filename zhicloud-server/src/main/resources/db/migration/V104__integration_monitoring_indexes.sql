-- ============================================================
-- V104: 可靠集成监控查询索引
--
-- 背景：
--   Outbox 的最老滞留指标查询 MIN(occurred_at) WHERE status IN ('NEW','PUBLISHING')，
--   Inbox 的最老待消费指标查询 MIN(received_at) WHERE status IN ('RECEIVED','FAILED')。
--   V100 的索引分别只覆盖 status + next_retry_at / status + processed_at，
--   当积压量上升时，Prometheus 每次抓取都可能扫描大量待处理行。
--
-- 处理：
--   1. event_outbox 增加 (status, occurred_at) 索引，支持按状态取最老业务发生时间。
--   2. event_inbox 增加 (status, received_at) 索引，支持按状态取最老接收时间。
--   3. 通过 information_schema.statistics 判断索引是否存在，避免部分执行后重跑失败。
--   4. 只做加法，不改写任何事件状态或业务数据。
--
-- 风险与边界：
--   两个 ALTER TABLE 显式要求 ALGORITHM=INPLACE、LOCK=NONE；若目标 MySQL 无法在线
--   添加索引则迁移失败，而不是静默退化为锁表 DDL。大表上线前仍应评估磁盘临时空间、
--   MDL 等待、复制延迟和业务低峰窗口，不能把迁移脚本本身当作性能验收。
-- ============================================================

SET @v104_outbox_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'event_outbox'
          AND index_name = 'idx_status_occurred'
    ),
    'SELECT 1',
    'ALTER TABLE `event_outbox` ADD INDEX `idx_status_occurred` (`status`, `occurred_at`), ALGORITHM=INPLACE, LOCK=NONE'
);
PREPARE v104_outbox_stmt FROM @v104_outbox_sql;
EXECUTE v104_outbox_stmt;
DEALLOCATE PREPARE v104_outbox_stmt;

SET @v104_inbox_sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'event_inbox'
          AND index_name = 'idx_status_received'
    ),
    'SELECT 1',
    'ALTER TABLE `event_inbox` ADD INDEX `idx_status_received` (`status`, `received_at`), ALGORITHM=INPLACE, LOCK=NONE'
);
PREPARE v104_inbox_stmt FROM @v104_inbox_sql;
EXECUTE v104_inbox_stmt;
DEALLOCATE PREPARE v104_inbox_stmt;

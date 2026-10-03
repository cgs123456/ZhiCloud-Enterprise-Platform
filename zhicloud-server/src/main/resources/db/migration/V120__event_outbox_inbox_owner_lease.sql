-- ============================================================
-- V120: event_outbox / event_inbox owner fencing 与租约列
--
-- 背景：
--   V100 建立了 Outbox/Inbox 数据面，但原表没有 processing_owner 和
--   lease_until。仅靠状态字段无法区分“当前 worker 仍在处理”和“旧 worker
--   在租约被接管后继续结算”的并发场景；Inbox 还缺少最近领取时间。
--
-- 处理：
--   1. event_outbox 增加 processing_owner、lease_until；
--   2. event_inbox 增加 processing_owner、lease_until、claimed_at；
--   3. 为两表增加 (status, lease_until, id) 索引，支持过期租约扫描；
--   4. 已存在列时只接受精确兼容定义，否则 SIGNAL fail-closed；
--   5. 迁移可重复执行，不删除、不改写任何业务行。
--
-- 边界：
--   * 这是 MySQL 迁移；其他数据库方言必须单独实现并做实库验证。
--   * ADD COLUMN / ADD INDEX 在存量大表上可能触发表重建或长时间 MDL；
--     生产执行前必须评估备份、磁盘空间、复制延迟和低峰窗口。
--   * 本迁移只提供数据库列与索引，不证明应用层 owner fencing、租约时长、
--     故障接管或生产并发语义已经通过验证。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v120_event_outbox_inbox_owner_lease;
DELIMITER $$
CREATE PROCEDURE p_v120_event_outbox_inbox_owner_lease()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'event_inbox';
    IF v_count = 0 THEN
        SET v_msg = 'V120 table missing: event_inbox';
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    SELECT COUNT(*) INTO v_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'event_outbox';
    IF v_count = 0 THEN
        SET v_msg = 'V120 table missing: event_outbox';
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    -- event_inbox.processing_owner
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'event_inbox'
           AND column_name = 'processing_owner'
    ) THEN
        ALTER TABLE `event_inbox`
            ADD COLUMN `processing_owner` varchar(128) NULL
                COMMENT '当前处理 owner；终态或重放时清空';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'event_inbox'
               AND column_name = 'processing_owner'
               AND LOWER(data_type) = 'varchar'
               AND character_maximum_length = 128
               AND is_nullable = 'YES'
               AND column_default IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_inbox.processing_owner';
        END IF;
    END IF;

    -- event_inbox.lease_until
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'event_inbox'
           AND column_name = 'lease_until'
    ) THEN
        ALTER TABLE `event_inbox`
            ADD COLUMN `lease_until` datetime NULL
                COMMENT '当前领取租约到期时间；过期后可被其他实例接管';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'event_inbox'
               AND column_name = 'lease_until'
               AND LOWER(data_type) = 'datetime'
               AND is_nullable = 'YES'
               AND column_default IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_inbox.lease_until';
        END IF;
    END IF;

    -- event_inbox.claimed_at
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'event_inbox'
           AND column_name = 'claimed_at'
    ) THEN
        ALTER TABLE `event_inbox`
            ADD COLUMN `claimed_at` datetime NULL
                COMMENT '最近一次领取时间';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'event_inbox'
               AND column_name = 'claimed_at'
               AND LOWER(data_type) = 'datetime'
               AND is_nullable = 'YES'
               AND column_default IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_inbox.claimed_at';
        END IF;
    END IF;

    -- event_outbox.processing_owner
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'event_outbox'
           AND column_name = 'processing_owner'
    ) THEN
        ALTER TABLE `event_outbox`
            ADD COLUMN `processing_owner` varchar(128) NULL
                COMMENT '当前投递 owner；终态或重放时清空';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'event_outbox'
               AND column_name = 'processing_owner'
               AND LOWER(data_type) = 'varchar'
               AND character_maximum_length = 128
               AND is_nullable = 'YES'
               AND column_default IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_outbox.processing_owner';
        END IF;
    END IF;

    -- event_outbox.lease_until
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'event_outbox'
           AND column_name = 'lease_until'
    ) THEN
        ALTER TABLE `event_outbox`
            ADD COLUMN `lease_until` datetime NULL
                COMMENT '当前投递租约到期时间；过期后可被其他实例接管';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'event_outbox'
               AND column_name = 'lease_until'
               AND LOWER(data_type) = 'datetime'
               AND is_nullable = 'YES'
               AND column_default IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_outbox.lease_until';
        END IF;
    END IF;

    -- event_inbox 租约扫描索引
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'event_inbox'
           AND index_name = 'idx_event_inbox_status_lease'
    ) THEN
        ALTER TABLE `event_inbox`
            ADD INDEX `idx_event_inbox_status_lease` (`status`, `lease_until`, `id`);
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.statistics
             WHERE table_schema = DATABASE()
               AND table_name = 'event_inbox'
               AND index_name = 'idx_event_inbox_status_lease'
             GROUP BY table_name, index_name
            HAVING COUNT(*) = 3
               AND MAX(non_unique) = 1
               AND SUM(CASE WHEN column_name = 'status' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN column_name = 'lease_until' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN column_name = 'id' AND seq_in_index = 3 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 3
               AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 3
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_inbox lease index';
        END IF;
    END IF;

    -- event_outbox 租约扫描索引
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'event_outbox'
           AND index_name = 'idx_event_outbox_status_lease'
    ) THEN
        ALTER TABLE `event_outbox`
            ADD INDEX `idx_event_outbox_status_lease` (`status`, `lease_until`, `id`);
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.statistics
             WHERE table_schema = DATABASE()
               AND table_name = 'event_outbox'
               AND index_name = 'idx_event_outbox_status_lease'
             GROUP BY table_name, index_name
            HAVING COUNT(*) = 3
               AND MAX(non_unique) = 1
               AND SUM(CASE WHEN column_name = 'status' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN column_name = 'lease_until' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN column_name = 'id' AND seq_in_index = 3 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 3
               AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 3
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V120 blocked: incompatible event_outbox lease index';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL p_v120_event_outbox_inbox_owner_lease();
DROP PROCEDURE IF EXISTS p_v120_event_outbox_inbox_owner_lease;

-- 回滚：删除 owner/lease 列或索引会撤销旧 worker fencing 和过期租约接管能力。
-- 回滚前必须确认应用已回退到兼容逻辑，并保留并发重复处理的补偿与审计方案。

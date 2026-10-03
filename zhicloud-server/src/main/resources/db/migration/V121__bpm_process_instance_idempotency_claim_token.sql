-- ============================================================
-- V121: BPM 流程实例幂等 claim token
--
-- 背景：
--   V118 只依赖 INSERT ... ON DUPLICATE KEY UPDATE 的 affected rows 判断
--   当前事务是否为 winner。Connector/J 默认 useAffectedRows=false 时，
--   重复命中也会返回 1，导致调用方在空白登记被其他事务提交后仍继续创建
--   Flowable 实例。
--
-- 处理：
--   1. bpm_process_instance_idempotency 增加 claim_token varchar(64) NULL；
--   2. 新调用把本次 claim token 写入新行，重复命中不得覆盖已有 token；
--   3. 已存在列时只接受精确兼容定义，否则 SIGNAL fail-closed；
--   4. 迁移可重复执行，不删除、不改写任何业务行。
--
-- 边界：
--   * 这是 MySQL 迁移；其他数据库方言必须单独实现并做实库验证。
--   * 存量 NULL token 的空白登记继续 fail-closed，需要人工对账/清理；
--     本迁移不自动回填 token，也不提供租约/过期回收。
--   * ADD COLUMN 在存量大表上可能触发表重建或长时间 MDL；
--     生产执行前必须评估备份、磁盘空间、复制延迟和低峰窗口。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v121_bpm_process_instance_idempotency_claim_token;
DELIMITER $$
CREATE PROCEDURE p_v121_bpm_process_instance_idempotency_claim_token()
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_count
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = 'bpm_process_instance_idempotency';
    IF v_count = 0 THEN
        SET v_msg = 'V121 table missing: bpm_process_instance_idempotency';
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'bpm_process_instance_idempotency'
           AND column_name = 'claim_token'
    ) THEN
        ALTER TABLE `bpm_process_instance_idempotency`
            ADD COLUMN `claim_token` varchar(64) NULL
                COMMENT '本次创建 claim 的 ownership token；legacy/blank 行为 NULL';
    ELSE
        IF NOT EXISTS (
            SELECT 1
              FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'bpm_process_instance_idempotency'
               AND column_name = 'claim_token'
               AND LOWER(data_type) = 'varchar'
               AND character_maximum_length = 64
               AND is_nullable = 'YES'
               AND column_default IS NULL
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'V121 blocked: incompatible bpm_process_instance_idempotency.claim_token';
        END IF;
    END IF;
END$$
DELIMITER ;

CALL p_v121_bpm_process_instance_idempotency_claim_token();
DROP PROCEDURE IF EXISTS p_v121_bpm_process_instance_idempotency_claim_token;

-- 回滚：删除 claim_token 会使应用无法区分 winner/loser，回滚前必须回退到
-- 不依赖 claim ownership 的应用版本，并完成空白登记对账。

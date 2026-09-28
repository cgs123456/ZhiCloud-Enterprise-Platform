-- ============================================================
-- V117: crm_receivable 租户级幂等键与载荷指纹
--
-- 背景：
--   CRM 回款创建接口已引入客户端幂等键，但生产库的 crm_receivable
--   还没有 idempotency_key / payload_hash 列，也没有数据库级唯一约束。
--   仅靠应用层“先查后插”无法覆盖并发重试，可能出现重复回款单据。
--
-- 目标：
--   1. 增加 idempotency_key、payload_hash 两个可空列，兼容历史数据；
--   2. 增加 (tenant_id, idempotency_key) 唯一索引，阻止同一租户并发重复创建；
--   3. 发现存量重复、键/指纹不成对或指纹格式异常时 fail-closed，不自动清理数据；
--   4. 迁移可重复执行，已有列/索引定义不兼容时明确失败。
--
-- 语义：
--   * idempotency_key 是永久幂等键，不因 deleted 变化而释放；
--     软删除记录仍占用该租户下的键，应用重复请求必须失败关闭。
--   * 历史行允许两个列都为 NULL；唯一索引允许多个 NULL。
--   * payload_hash 必须与 idempotency_key 同时存在或同时为 NULL。
--   * idempotency_key 使用 utf8mb4_bin，按字节精确比较，避免大小写或重音折叠；
--     payload_hash 使用 ascii_bin，固定为 64 位十六进制 SHA-256。
--
-- 边界：
--   * 这是 MySQL 迁移；其他数据库方言必须单独实现并做实库并发验证。
--   * ADD COLUMN / ADD UNIQUE KEY 在存量大表上可能触发表重建或长时间 MDL；
--     生产执行前必须评估备份、磁盘、复制延迟和低峰窗口。
--   * 本迁移不修改金额精度。V51.1 与 V82 对 CRM 金额列的精度口径不一致，
--     需要先冻结业务口径，再另立迁移评估 DECIMAL 变更成本。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v117_crm_receivable_idempotency;

DELIMITER $$
CREATE PROCEDURE p_v117_crm_receivable_idempotency()
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 table missing: crm_receivable';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND column_name = 'tenant_id'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 required column missing: crm_receivable.tenant_id';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND column_name = 'idempotency_key'
    ) THEN
        ALTER TABLE `crm_receivable`
            ADD COLUMN `idempotency_key` varchar(128)
                CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL
                COMMENT '客户端幂等键（同租户内永久唯一，软删除仍占用）';
    ELSEIF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND column_name = 'idempotency_key'
           AND LOWER(data_type) = 'varchar'
           AND character_maximum_length = 128
           AND is_nullable = 'YES'
           AND LOWER(character_set_name) = 'utf8mb4'
           AND LOWER(collation_name) = 'utf8mb4_bin'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 blocked: idempotency_key exists but has an incompatible definition';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND column_name = 'payload_hash'
    ) THEN
        ALTER TABLE `crm_receivable`
            ADD COLUMN `payload_hash` char(64)
                CHARACTER SET ascii COLLATE ascii_bin NULL
                COMMENT '创建请求载荷的 SHA-256（64 位十六进制）';
    ELSEIF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND column_name = 'payload_hash'
           AND LOWER(data_type) = 'char'
           AND character_maximum_length = 64
           AND is_nullable = 'YES'
           AND LOWER(character_set_name) = 'ascii'
           AND LOWER(collation_name) = 'ascii_bin'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 blocked: payload_hash exists but has an incompatible definition';
    END IF;

    -- 先做数据质量检查，再建立唯一索引；任何异常都阻断迁移，不自动删改业务数据。
    IF EXISTS (
        SELECT 1
          FROM crm_receivable
         WHERE idempotency_key IS NOT NULL
           AND (CHAR_LENGTH(idempotency_key) = 0 OR idempotency_key <> TRIM(idempotency_key))
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 blocked: empty or untrimmed idempotency_key';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM crm_receivable
         WHERE idempotency_key IS NULL
           AND payload_hash IS NOT NULL
    ) OR EXISTS (
        SELECT 1
          FROM crm_receivable
         WHERE idempotency_key IS NOT NULL
           AND (payload_hash IS NULL OR CHAR_LENGTH(payload_hash) <> 64
                OR payload_hash NOT REGEXP '^[0-9a-fA-F]{64}$')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 blocked: idempotency_key and payload_hash are not a valid pair';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM crm_receivable
         WHERE idempotency_key IS NOT NULL
         GROUP BY tenant_id, idempotency_key
        HAVING COUNT(*) > 1
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 blocked: duplicate idempotency_key within tenant';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND index_name = 'uk_crm_receivable_idempotency'
    ) THEN
        ALTER TABLE `crm_receivable`
            ADD UNIQUE KEY `uk_crm_receivable_idempotency` (`tenant_id`, `idempotency_key`);
    ELSEIF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = 'crm_receivable'
           AND index_name = 'uk_crm_receivable_idempotency'
         GROUP BY table_name, index_name
        HAVING COUNT(*) = 2
           AND MAX(non_unique) = 0
           AND SUM(CASE WHEN column_name = 'tenant_id' AND seq_in_index = 1 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
           AND SUM(CASE WHEN column_name = 'idempotency_key' AND seq_in_index = 2 AND sub_part IS NULL THEN 1 ELSE 0 END) = 1
           AND SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END) = 2
           AND SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END) = 2
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'V117 blocked: idempotency unique index has an incompatible definition';
    END IF;
END$$
DELIMITER ;

CALL p_v117_crm_receivable_idempotency();
DROP PROCEDURE IF EXISTS p_v117_crm_receivable_idempotency;

-- 回滚：删除唯一索引和两个列会撤销数据库级幂等保护。回滚前必须确认应用已
-- 恢复兼容逻辑，并保留重复回款风险登记与补偿方案；不能单独回滚本迁移。

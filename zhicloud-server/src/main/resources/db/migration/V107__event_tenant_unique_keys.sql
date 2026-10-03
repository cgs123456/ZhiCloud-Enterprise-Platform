-- ============================================================
-- V107: 事件 Outbox/Inbox 唯一键按租户隔离
--
-- 背景：
--   V100 将 event_outbox.event_id 建为全局唯一，将 event_inbox
--   (consumer_name, event_id) 建为全局唯一。业务事件编号通常只在一个租户内唯一，
--   不同租户使用相同业务单据编号时，全局唯一键会阻止第二个租户登记事件，或者让
--   应用层误把别的租户事件当成重复事件。V107 把幂等边界改为租户内唯一。
--
-- 处理：
--   1. event_outbox 新唯一键：(tenant_id, event_id)。
--   2. event_inbox 新唯一键：(tenant_id, consumer_name, event_id)。
--   3. 新唯一键成功建立后，才删除旧全局唯一键。
--   4. 迁移可重复执行：索引已存在且列顺序正确时跳过。
--
-- 失败策略：
--   如果存量数据存在同一租户内重复的 event_id（或同一租户内同一消费者重复事件），
--   迁移直接 SIGNAL 失败，不自动删除或合并任何事件。必须先由业务/运维确认数据
--   来源并完成去重，再重跑迁移。
--
-- 上线边界：
--   本迁移不证明数据已完成备份，也不替代大表在线 DDL 评估。生产执行前必须确认
--   备份可用、复制延迟可接受、MDL/磁盘空间充足，并在低峰窗口执行。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v107_assert_column;
DROP PROCEDURE IF EXISTS p_v107_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v107_drop_index_if_exists;

DELIMITER $$
CREATE PROCEDURE p_v107_assert_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = p_column;
    IF v_exists = 0 THEN
        SET v_msg = CONCAT('V107 column missing: ', p_table, '.', p_column);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
END$$

CREATE PROCEDURE p_v107_ensure_tenant_unique_index(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255),
    IN p_expected_columns VARCHAR(255)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_non_unique INT DEFAULT 0;
    DECLARE v_actual_columns VARCHAR(500) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND index_name = p_index;

    IF v_exists > 0 THEN
        SELECT COUNT(*) INTO v_non_unique
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
           AND non_unique <> 0;
        SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) INTO v_actual_columns
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index;

        IF v_non_unique > 0
           OR LOWER(IFNULL(v_actual_columns, '')) <> LOWER(p_expected_columns) THEN
            SET v_msg = CONCAT('V107 index mismatch: ', p_table, '.', p_index,
                               ' expected=', p_expected_columns,
                               ' actual=', IFNULL(v_actual_columns, ''));
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    ELSE
        SET @v107_dup_count = 0;
        SET @v107_dup_sql = CONCAT(
            'SELECT COUNT(*) INTO @v107_dup_count FROM (SELECT 1 FROM `', p_table,
            '` GROUP BY ', p_columns, ' HAVING COUNT(*) > 1 LIMIT 1) AS d'
        );
        PREPARE v107_dup_stmt FROM @v107_dup_sql;
        EXECUTE v107_dup_stmt;
        DEALLOCATE PREPARE v107_dup_stmt;

        IF @v107_dup_count > 0 THEN
            SET v_msg = CONCAT('V107 duplicate rows block unique index ', p_index, ' on ', p_table);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;

        SET @v107_index_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')'
        );
        PREPARE v107_index_stmt FROM @v107_index_sql;
        EXECUTE v107_index_stmt;
        DEALLOCATE PREPARE v107_index_stmt;
    END IF;
END$$

CREATE PROCEDURE p_v107_drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @v107_drop_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE v107_drop_stmt FROM @v107_drop_sql;
        EXECUTE v107_drop_stmt;
        DEALLOCATE PREPARE v107_drop_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v107_assert_column('event_outbox', 'tenant_id');
CALL p_v107_assert_column('event_outbox', 'event_id');
CALL p_v107_assert_column('event_inbox', 'tenant_id');
CALL p_v107_assert_column('event_inbox', 'consumer_name');
CALL p_v107_assert_column('event_inbox', 'event_id');

CALL p_v107_ensure_tenant_unique_index(
    'event_outbox',
    'uk_event_tenant_event',
    '`tenant_id`, `event_id`',
    'tenant_id,event_id'
);
CALL p_v107_ensure_tenant_unique_index(
    'event_inbox',
    'uk_consumer_tenant_event',
    '`tenant_id`, `consumer_name`, `event_id`',
    'tenant_id,consumer_name,event_id'
);

CALL p_v107_drop_index_if_exists('event_outbox', 'uk_event_id');
CALL p_v107_drop_index_if_exists('event_inbox', 'uk_consumer_event');

DROP PROCEDURE IF EXISTS p_v107_assert_column;
DROP PROCEDURE IF EXISTS p_v107_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v107_drop_index_if_exists;

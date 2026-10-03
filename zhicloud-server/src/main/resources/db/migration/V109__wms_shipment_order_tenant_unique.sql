-- ============================================================
-- V109: WMS 出库单号按租户唯一
--
-- 背景：
--   V11 将 wms_shipment_order.no 建成全局唯一键 uk_no。出库单号属于租户内
--   业务编号，不同租户可能使用相同编号；全局唯一键会让第二个租户建单失败，
--   也会让跨租户事件重放时产生不可预期冲突。
--
-- 处理：
--   1. 先建立新唯一键 uk_tenant_no (tenant_id, no)。
--   2. 新键建立成功并校验通过后，删除旧全局唯一键 uk_no。
--   3. 迁移可重复执行：新键已存在且列顺序/唯一性正确时跳过创建。
--
-- 失败策略：
--   同一租户内已存在重复 no 时直接 SIGNAL 失败，不自动删除或合并业务单据。
--   如果 uk_tenant_no 已存在但定义错误，同样失败，避免以错误索引继续上线。
--
-- 上线边界：
--   本迁移不证明数据已备份，也不替代大表在线 DDL 评估。生产执行前必须确认
--   备份可恢复、复制延迟可接受、MDL/磁盘空间充足，并在低峰窗口执行。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v109_assert_column;
DROP PROCEDURE IF EXISTS p_v109_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v109_drop_index_if_exists;

DELIMITER $$
CREATE PROCEDURE p_v109_assert_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = p_column;
    IF v_exists = 0 THEN
        SET v_msg = CONCAT('V109 column missing: ', p_table, '.', p_column);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
END$$

CREATE PROCEDURE p_v109_ensure_tenant_unique_index(
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
            SET v_msg = CONCAT('V109 index mismatch: ', p_table, '.', p_index,
                               ' expected=', p_expected_columns,
                               ' actual=', IFNULL(v_actual_columns, ''));
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    ELSE
        SET @v109_dup_count = 0;
        SET @v109_dup_sql = CONCAT(
            'SELECT COUNT(*) INTO @v109_dup_count FROM (SELECT 1 FROM `', p_table,
            '` GROUP BY ', p_columns, ' HAVING COUNT(*) > 1 LIMIT 1) AS d'
        );
        PREPARE v109_dup_stmt FROM @v109_dup_sql;
        EXECUTE v109_dup_stmt;
        DEALLOCATE PREPARE v109_dup_stmt;

        IF @v109_dup_count > 0 THEN
            SET v_msg = CONCAT('V109 duplicate rows block unique index ', p_index, ' on ', p_table);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;

        SET @v109_index_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')'
        );
        PREPARE v109_index_stmt FROM @v109_index_sql;
        EXECUTE v109_index_stmt;
        DEALLOCATE PREPARE v109_index_stmt;
    END IF;
END$$

CREATE PROCEDURE p_v109_drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @v109_drop_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE v109_drop_stmt FROM @v109_drop_sql;
        EXECUTE v109_drop_stmt;
        DEALLOCATE PREPARE v109_drop_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v109_assert_column('wms_shipment_order', 'tenant_id');
CALL p_v109_assert_column('wms_shipment_order', 'no');

CALL p_v109_ensure_tenant_unique_index(
    'wms_shipment_order',
    'uk_tenant_no',
    '`tenant_id`, `no`',
    'tenant_id,no'
);

CALL p_v109_drop_index_if_exists('wms_shipment_order', 'uk_no');

DROP PROCEDURE IF EXISTS p_v109_assert_column;
DROP PROCEDURE IF EXISTS p_v109_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v109_drop_index_if_exists;

-- ============================================================
-- V119: inventory_item 租户级复合唯一键
--
-- 背景：
--   V81 将 inventory_item 建为
--   (item_id, warehouse_id, location_id, area_id, batch_id) 全局唯一。
--   库存条目属于租户内业务实体；不同租户使用相同物品、仓库、库位、库区或批次
--   组合时，全局唯一键会让第二个租户写入失败，并破坏租户数据隔离。
--
-- 处理：
--   1. 先建立新唯一键
--      uk_inventory_item_tenant_composite
--      (tenant_id, item_id, warehouse_id, location_id, area_id, batch_id)。
--   2. 新键建立并校验通过后，删除旧全局唯一键
--      uk_item_warehouse_location_area_batch。
--   3. 迁移可重复执行：新键存在且定义正确时跳过创建，旧键不存在时跳过删除。
--
-- 失败策略：
--   存量数据存在同一租户内重复的复合键时，直接 SIGNAL 失败，不自动删除、
--   合并或改写业务数据；新键已存在但定义错误、旧键存在但定义不符时也失败。
--
-- 边界：
--   * 这是 MySQL 迁移；其他数据库方言必须单独实现并做实库验证。
--   * 添加/删除唯一索引在存量大表上可能触发表重建或长时间 MDL；
--     生产执行前必须评估备份、磁盘、复制延迟和低峰窗口。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v119_assert_table;
DROP PROCEDURE IF EXISTS p_v119_assert_column;
DROP PROCEDURE IF EXISTS p_v119_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v119_drop_legacy_index_if_exists;
DROP PROCEDURE IF EXISTS p_v119_assert_no_global_unique_index;

DELIMITER $$
CREATE PROCEDURE p_v119_assert_table(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = p_table;

    IF v_exists = 0 THEN
        SET v_msg = CONCAT('V119 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
END$$

CREATE PROCEDURE p_v119_assert_column(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_data_type VARCHAR(64),
    IN p_nullable VARCHAR(3)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = p_column
       AND LOWER(data_type) = LOWER(p_data_type)
       AND is_nullable = p_nullable;

    IF v_exists = 0 THEN
        SET v_msg = CONCAT('V119 column missing or incompatible: ',
                           p_table, '.', p_column,
                           ' expected ', LOWER(p_data_type), '/', p_nullable);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
END$$

CREATE PROCEDURE p_v119_ensure_tenant_unique_index(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(255),
    IN p_expected_columns VARCHAR(255)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_column_count INT DEFAULT 0;
    DECLARE v_non_unique INT DEFAULT 0;
    DECLARE v_full_columns INT DEFAULT 0;
    DECLARE v_btree_columns INT DEFAULT 0;
    DECLARE v_actual_columns VARCHAR(500) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    -- 无论新索引是否已存在，都先检查目标列组合的存量重复，避免部分升级后
    -- 以错误的非唯一索引绕过租户隔离语义。
    SET @v119_dup_count = 0;
    SET @v119_dup_sql = CONCAT(
        'SELECT COUNT(*) INTO @v119_dup_count FROM (SELECT 1 FROM `', p_table,
        '` GROUP BY ', p_columns, ' HAVING COUNT(*) > 1 LIMIT 1) AS d'
    );
    PREPARE v119_dup_stmt FROM @v119_dup_sql;
    EXECUTE v119_dup_stmt;
    DEALLOCATE PREPARE v119_dup_stmt;

    IF @v119_dup_count > 0 THEN
        SET v_msg = CONCAT('V119 duplicate rows block unique index ',
                           p_index, ' on ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND index_name = p_index;

    IF v_exists > 0 THEN
        SELECT COUNT(*),
               MAX(non_unique),
               SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END),
               SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END)
          INTO v_column_count, v_non_unique, v_full_columns, v_btree_columns
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index;

        SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) INTO v_actual_columns
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index;

        IF v_column_count <> 6
           OR v_non_unique <> 0
           OR v_full_columns <> 6
           OR v_btree_columns <> 6
           OR LOWER(IFNULL(v_actual_columns, '')) <> LOWER(p_expected_columns) THEN
            SET v_msg = LEFT(CONCAT('V119 index mismatch: ', p_table, '.', p_index,
                                    ' expected=', p_expected_columns,
                                    ' actual=', IFNULL(v_actual_columns, '')), 128);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    ELSE
        SET @v119_index_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')'
        );
        PREPARE v119_index_stmt FROM @v119_index_sql;
        EXECUTE v119_index_stmt;
        DEALLOCATE PREPARE v119_index_stmt;
    END IF;
END$$

CREATE PROCEDURE p_v119_drop_legacy_index_if_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_legacy_columns VARCHAR(255)
)
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_column_count INT DEFAULT 0;
    DECLARE v_non_unique INT DEFAULT 0;
    DECLARE v_full_columns INT DEFAULT 0;
    DECLARE v_btree_columns INT DEFAULT 0;
    DECLARE v_actual_columns VARCHAR(500) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND index_name = p_index;

    IF v_exists > 0 THEN
        SELECT COUNT(*),
               MAX(non_unique),
               SUM(CASE WHEN sub_part IS NULL THEN 1 ELSE 0 END),
               SUM(CASE WHEN UPPER(index_type) = 'BTREE' THEN 1 ELSE 0 END)
          INTO v_column_count, v_non_unique, v_full_columns, v_btree_columns
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index;

        SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) INTO v_actual_columns
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index;

        IF v_column_count <> 5
           OR v_non_unique <> 0
           OR v_full_columns <> 5
           OR v_btree_columns <> 5
           OR LOWER(IFNULL(v_actual_columns, '')) <> LOWER(p_legacy_columns) THEN
            SET v_msg = LEFT(CONCAT('V119 legacy index mismatch: ', p_table, '.', p_index,
                                    ' expected=', p_legacy_columns,
                                    ' actual=', IFNULL(v_actual_columns, '')), 128);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;

        SET @v119_drop_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE v119_drop_stmt FROM @v119_drop_sql;
        EXECUTE v119_drop_stmt;
        DEALLOCATE PREPARE v119_drop_stmt;
    END IF;
END$$

CREATE PROCEDURE p_v119_assert_no_global_unique_index(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_count
      FROM (
          SELECT index_name
            FROM information_schema.statistics
           WHERE table_schema = DATABASE()
             AND table_name = p_table
             AND non_unique = 0
           GROUP BY index_name
          HAVING COUNT(*) = 5
             AND LOWER(GROUP_CONCAT(column_name ORDER BY column_name)) =
                 'area_id,batch_id,item_id,location_id,warehouse_id'
      ) AS global_unique_indexes;

    IF v_count > 0 THEN
        SET v_msg = CONCAT('V119 blocked: global inventory unique index remains on ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
END$$
DELIMITER ;

CALL p_v119_assert_table('inventory_item');
CALL p_v119_assert_column('inventory_item', 'tenant_id', 'bigint', 'NO');
CALL p_v119_assert_column('inventory_item', 'item_id', 'bigint', 'NO');
CALL p_v119_assert_column('inventory_item', 'warehouse_id', 'bigint', 'NO');
CALL p_v119_assert_column('inventory_item', 'location_id', 'bigint', 'NO');
CALL p_v119_assert_column('inventory_item', 'area_id', 'bigint', 'NO');
CALL p_v119_assert_column('inventory_item', 'batch_id', 'bigint', 'NO');

CALL p_v119_ensure_tenant_unique_index(
    'inventory_item',
    'uk_inventory_item_tenant_composite',
    '`tenant_id`, `item_id`, `warehouse_id`, `location_id`, `area_id`, `batch_id`',
    'tenant_id,item_id,warehouse_id,location_id,area_id,batch_id'
);

CALL p_v119_drop_legacy_index_if_exists(
    'inventory_item',
    'uk_item_warehouse_location_area_batch',
    'item_id,warehouse_id,location_id,area_id,batch_id'
);

CALL p_v119_assert_no_global_unique_index('inventory_item');

DROP PROCEDURE IF EXISTS p_v119_assert_table;
DROP PROCEDURE IF EXISTS p_v119_assert_column;
DROP PROCEDURE IF EXISTS p_v119_ensure_tenant_unique_index;
DROP PROCEDURE IF EXISTS p_v119_drop_legacy_index_if_exists;
DROP PROCEDURE IF EXISTS p_v119_assert_no_global_unique_index;

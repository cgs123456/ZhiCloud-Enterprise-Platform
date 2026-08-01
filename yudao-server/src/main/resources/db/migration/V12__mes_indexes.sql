-- ============================================================
-- V12: MES 模块业务表二级索引补充（P0-5）
-- ============================================================
-- 背景：mes.sql 中 137 张业务表虽有 idx_tenant_id，但缺少：
--   1. (tenant_id, deleted) 复合索引 —— 多租户 + 软删除高效过滤
--   2. (create_time) 索引 —— 按创建时间查询/分页
--   3. (status) 索引 —— 业务单据状态过滤
--   4. (code) 索引 —— 主数据/单据编码查询
-- 策略：使用游标遍历 information_schema.tables 自动添加索引，脚本紧凑且覆盖全部 137 张表。
--      通过 information_schema.columns 判断字段是否存在，仅对存在的字段添加索引。
-- 复用幂等判断逻辑（仅在索引不存在时创建）
-- ============================================================

-- ------------------------------------------------------------ 
-- 存储过程 1：为所有 mes_* 表添加 (tenant_id, deleted) 复合索引
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_mes_add_tenant_deleted_index;
CREATE PROCEDURE p_mes_add_tenant_deleted_index()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_table VARCHAR(64);
    DECLARE v_index VARCHAR(100);
    DECLARE cur CURSOR FOR
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name LIKE 'mes\_%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    td_loop: LOOP
        FETCH cur INTO v_table;
        IF done THEN LEAVE td_loop; END IF;

        SET v_index = CONCAT('idx_', v_table, '_tenant_deleted');
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND index_name = v_index
        ) AND EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND column_name = 'tenant_id'
        ) AND EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND column_name = 'deleted'
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', v_table, '` ADD INDEX `', v_index, '` (tenant_id, deleted)');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END;

-- ------------------------------------------------------------ 
-- 存储过程 2：为所有 mes_* 表添加 (create_time) 索引
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_mes_add_create_time_index;
CREATE PROCEDURE p_mes_add_create_time_index()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_table VARCHAR(64);
    DECLARE v_index VARCHAR(100);
    DECLARE cur CURSOR FOR
        SELECT table_name FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name LIKE 'mes\_%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    ct_loop: LOOP
        FETCH cur INTO v_table;
        IF done THEN LEAVE ct_loop; END IF;

        SET v_index = CONCAT('idx_', v_table, '_create_time');
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND index_name = v_index
        ) AND EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND column_name = 'create_time'
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', v_table, '` ADD INDEX `', v_index, '` (create_time)');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END;

-- ------------------------------------------------------------ 
-- 存储过程 3：为带 status 字段的 mes_* 表添加 (status) 索引
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_mes_add_status_index;
CREATE PROCEDURE p_mes_add_status_index()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_table VARCHAR(64);
    DECLARE v_index VARCHAR(100);
    DECLARE cur CURSOR FOR
        SELECT DISTINCT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON c.table_schema = t.table_schema AND c.table_name = t.table_name
        WHERE c.table_schema = DATABASE()
          AND c.table_name LIKE 'mes\_%'
          AND c.column_name = 'status';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    st_loop: LOOP
        FETCH cur INTO v_table;
        IF done THEN LEAVE st_loop; END IF;

        SET v_index = CONCAT('idx_', v_table, '_status');
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND index_name = v_index
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', v_table, '` ADD INDEX `', v_index, '` (status)');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END;

-- ------------------------------------------------------------ 
-- 存储过程 4：为带 code 字段的 mes_* 表添加 (code) 索引
--    （非唯一索引，避免历史脏数据导致建索引失败）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_mes_add_code_index;
CREATE PROCEDURE p_mes_add_code_index()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_table VARCHAR(64);
    DECLARE v_index VARCHAR(100);
    DECLARE cur CURSOR FOR
        SELECT DISTINCT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON c.table_schema = t.table_schema AND c.table_name = t.table_name
        WHERE c.table_schema = DATABASE()
          AND c.table_name LIKE 'mes\_%'
          AND c.column_name = 'code'
          AND c.data_type IN ('varchar', 'char');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    cd_loop: LOOP
        FETCH cur INTO v_table;
        IF done THEN LEAVE cd_loop; END IF;

        SET v_index = CONCAT('idx_', v_table, '_code');
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND index_name = v_index
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', v_table, '` ADD INDEX `', v_index, '` (code)');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END;

-- ------------------------------------------------------------ 
-- 存储过程 5：为带 *_id 外键字段的 mes_* 表添加对应索引
--    仅处理核心外键字段：plan_id / machinery_id / team_id / workstation_id / item_id / route_id / 
--    work_order_id / product_id / bom_id / subject_id / order_id / vendor_id / client_id / task_id / 
--    warehouse_id / line_id / detail_id / parent_id / check_plan_id / sn_id / plan_detail_id
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_mes_add_fk_indexes;
CREATE PROCEDURE p_mes_add_fk_indexes()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_table VARCHAR(64);
    DECLARE v_col VARCHAR(64);
    DECLARE v_index VARCHAR(120);
    DECLARE cur CURSOR FOR
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON c.table_schema = t.table_schema AND c.table_name = t.table_name
        WHERE c.table_schema = DATABASE()
          AND c.table_name LIKE 'mes\_%'
          AND c.column_name IN (
            'plan_id', 'machinery_id', 'team_id', 'workstation_id', 'item_id',
            'route_id', 'work_order_id', 'product_id', 'bom_id', 'subject_id',
            'order_id', 'vendor_id', 'client_id', 'task_id', 'warehouse_id',
            'line_id', 'detail_id', 'parent_id', 'check_plan_id', 'sn_id',
            'plan_detail_id', 'workstation_machine_id', 'workstation_tool_id',
            'workstation_worker_id', 'machinery_type_id', 'item_type_id',
            'process_id', 'unit_measure_id', 'item_category_id', 'category_id',
            'brand_id', 'workshop_id', 'route_process_id', 'route_product_id',
            'route_product_bom_id', 'feedback_id', 'card_id', 'card_process_id',
            'task_issue_id', 'work_record_id', 'check_record_id', 'mainten_record_id',
            'repair_id', 'defect_id', 'indicator_id', 'template_id', 'iqc_id',
            'ipqc_id', 'oqc_id', 'rqc_id', 'tool_type_id', 'tool_id',
            'arrival_notice_id', 'notice_line_id', 'package_id', 'package_line_id',
            'batch_id', 'sn_id', 'material_stock_id', 'produce_id', 'receipt_id',
            'sales_id', 'return_id', 'stock_taking_plan_id', 'stock_taking_task_id',
            'transfer_id', 'warehouse_area_id', 'warehouse_location_id'
          )
          AND c.data_type = 'bigint';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    fk_loop: LOOP
        FETCH cur INTO v_table, v_col;
        IF done THEN LEAVE fk_loop; END IF;

        SET v_index = CONCAT('idx_', v_table, '_', v_col);
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = v_table
              AND index_name = v_index
        ) THEN
            SET @sql = CONCAT('ALTER TABLE `', v_table, '` ADD INDEX `', v_index, '` (', v_col, ')');
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END;

-- ============================================================
-- 执行索引补充
-- ============================================================
CALL p_mes_add_tenant_deleted_index();
CALL p_mes_add_create_time_index();
CALL p_mes_add_status_index();
CALL p_mes_add_code_index();
CALL p_mes_add_fk_indexes();

-- ============================================================
-- 清理存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_mes_add_tenant_deleted_index;
DROP PROCEDURE IF EXISTS p_mes_add_create_time_index;
DROP PROCEDURE IF EXISTS p_mes_add_status_index;
DROP PROCEDURE IF EXISTS p_mes_add_code_index;
DROP PROCEDURE IF EXISTS p_mes_add_fk_indexes;

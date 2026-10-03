-- ============================================================
-- V96: erp_accounting_event 期次列与来源唯一键扩展
--
-- 背景：
--   V94 的来源唯一键 uk_erp_accounting_event_source 覆盖
--   (tenant_id, event_type, source_doc_type, source_doc_id, source_doc_line_id,
--    source_revision, reversal_of_event_id)。
--   该约束使同一来源单据在同一事件类型下最多只能落地一条会计事件，
--   因此无法表达「分批开票 / 分期收款 / 分期付款」等部分结算场景：
--   第二次部分结算会被唯一键直接拒绝，而不是生成独立的期次事件。
--
-- 目标：
--   1. 为 erp_accounting_event 增加 sequence_no 期次列。
--   2. 将来源唯一键扩展为包含 sequence_no，允许同一单据按 1..N 期次落地多条事件。
--
-- 兼容性：
--   * 新增列与索引均通过 information_schema 判存在后执行，可重复执行。
--   * 不回填历史数据：历史事件的 sequence_no 取默认值 0，表示「主事件/未分期」。
--   * sequence_no = 0 保持原有语义，已落地的历史事件与冲销事件不受影响。
--
-- 限界：
--   * 本迁移只提供结构与约束，不证明部分结算金额累计不超额（需在单据服务层校验）。
--   * 对已有大表的 ALTER TABLE 可能触发表重建或锁表，生产窗口、在线 DDL
--     和回滚方案必须先在预生产同规模数据上演练与计时。
--   * 唯一键重建期间该表会短暂不可写入新来源事件，必须安排在低峰窗口。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 期次列幂等补齐
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v96_add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_v96_add_column_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(500)
)
BEGIN
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = p_table
    ) THEN
        SET v_msg = CONCAT('V96 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @v96_column_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition
        );
        PREPARE v96_column_stmt FROM @v96_column_sql;
        EXECUTE v96_column_stmt;
        DEALLOCATE PREPARE v96_column_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v96_add_column_if_not_exists(
    'erp_accounting_event',
    'sequence_no',
    'INT NOT NULL DEFAULT 0 COMMENT ''期次序号（部分开票/收款/付款从 1 递增，主事件为 0）''');

DROP PROCEDURE IF EXISTS p_v96_add_column_if_not_exists;

-- ------------------------------------------------------------
-- 2. 来源唯一键扩展为包含 sequence_no
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v96_replace_source_unique_key;
DELIMITER $$
CREATE PROCEDURE p_v96_replace_source_unique_key()
BEGIN
    DECLARE v_columns VARCHAR(500) DEFAULT NULL;

    SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) INTO v_columns
      FROM information_schema.statistics
     WHERE TABLE_SCHEMA = DATABASE()
       AND TABLE_NAME = 'erp_accounting_event'
       AND INDEX_NAME = 'uk_erp_accounting_event_source';

    IF v_columns IS NULL THEN
        SET @v96_key_sql = CONCAT('ALTER TABLE `erp_accounting_event` '
           , 'ADD UNIQUE KEY `uk_erp_accounting_event_source` '
           , '(`tenant_id`, `event_type`, `source_doc_type`, `source_doc_id`, '
           , '`source_doc_line_id`, `source_revision`, `sequence_no`, `reversal_of_event_id`)');
        PREPARE v96_key_stmt FROM @v96_key_sql;
        EXECUTE v96_key_stmt;
        DEALLOCATE PREPARE v96_key_stmt;
    ELSEIF LOCATE('sequence_no', v_columns) = 0 THEN
        SET @v96_key_sql = CONCAT('ALTER TABLE `erp_accounting_event` '
           , 'DROP INDEX `uk_erp_accounting_event_source`');
        PREPARE v96_key_stmt FROM @v96_key_sql;
        EXECUTE v96_key_stmt;
        DEALLOCATE PREPARE v96_key_stmt;

        SET @v96_key_sql = CONCAT('ALTER TABLE `erp_accounting_event` '
           , 'ADD UNIQUE KEY `uk_erp_accounting_event_source` '
           , '(`tenant_id`, `event_type`, `source_doc_type`, `source_doc_id`, '
           , '`source_doc_line_id`, `source_revision`, `sequence_no`, `reversal_of_event_id`)');
        PREPARE v96_key_stmt FROM @v96_key_sql;
        EXECUTE v96_key_stmt;
        DEALLOCATE PREPARE v96_key_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v96_replace_source_unique_key();
DROP PROCEDURE IF EXISTS p_v96_replace_source_unique_key;

-- 回滚：sequence_no 未被业务写入前可停写后删除列并恢复旧唯一键；
-- 一旦部分结算事件已落地，回滚需由数据负责人先导出期次事件再执行。

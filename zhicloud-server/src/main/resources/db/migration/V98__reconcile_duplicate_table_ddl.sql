-- ============================================================
-- V98: 重复建表收敛（11 组 CREATE TABLE 冲突的结构补齐）
--
-- 背景：
--   1. 迁移链上有 11 张表被两个迁移用 CREATE TABLE IF NOT EXISTS 重复创建：
--      qms_* 4 张（V2/V13）、aimultiagent_* 2 张（V3/V14）、wms_wave_* 2 张
--      （V4/V11）、erp_period / erp_period_close（V6/V10）、
--      ai_nl2sql_query_history（V38/V73）。
--   2. 其中 8 组的两个定义逐字节相同，不存在结构漂移；真正冲突的是 3 组：
--      erp_period、erp_period_close、ai_nl2sql_query_history。
--   3. Flyway 按版本号升序执行，版本小者先建表、版本大者因 IF NOT EXISTS 空转。
--      但对“先上线过大版本迁移、后补入小版本迁移”的历史库，生效的可能是后上线
--      的大版本结构。erp_period 若生效的是 V10 结构，则 ErpPeriodDO 需要的
--      year/month/start_date/end_date/closed_by/closed_time/remark 全部缺失，
--      会计期间守卫会读到错误状态或直接报 Unknown column。
--
-- 策略（只做加法与加宽，不做删减）：
--   * 以“与 DO 对齐”的定义为权威：erp_period / erp_period_close 取 V6，
--     ai_nl2sql_query_history 取 V38。
--   * 缺失列按权威定义补回；权威定义里 NOT NULL 且无默认值的列先补成可空列
--     并按 code 回填，NOT NULL 收敛需业务确认后单独迁移，不在本脚本强加。
--   * 另一份定义独有的列补成可空兼容列，使两种历史最终列集合一致，
--     消除“谁先执行谁生效”的歧义。
--   * 列类型只做加宽（VARCHAR(255) -> TEXT、VARCHAR(255) -> VARCHAR(2000)），
--     不做收窄；收窄会丢数据，必须先清洗再单独评审。
--   * id 缺少 AUTO_INCREMENT 时补齐（V10 结构下 id 只有 PRIMARY KEY）。
--   * 只补非唯一索引；唯一键涉及重复数据判定，延后到数据清洗后的单独迁移。
--
-- 限界：
--   * 本脚本未在任何真实 MySQL 上执行过（Docker 守护进程不可用），只有静态
--     lint 与契约门禁证据。执行前必须跑 V98_verify.sql 预检。
--   * ADD COLUMN / MODIFY 仍可能触发表重建；生产应在维护窗口执行，大表评估
--     gh-ost / pt-online-schema-change。
--   * 回填只处理 code 为 6 位数字的期间，其余行需要人工补录。
-- ============================================================

-- ------------------------------------------------------------
-- 辅助过程 1：表存在则按定义补列，已存在则跳过
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v98_add_column;
DELIMITER $$
CREATE PROCEDURE p_v98_add_column(
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
        SET v_msg = CONCAT('V98 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @v98_add_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition
        );
        PREPARE v98_add_stmt FROM @v98_add_sql;
        EXECUTE v98_add_stmt;
        DEALLOCATE PREPARE v98_add_stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- 辅助过程 2：列类型与期望不一致时按定义改写（只用于加宽）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v98_widen_column;
DELIMITER $$
CREATE PROCEDURE p_v98_widen_column(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_expected_type VARCHAR(64),
    IN p_definition VARCHAR(500)
)
BEGIN
    DECLARE v_current_type VARCHAR(64) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COLUMN_TYPE
      INTO v_current_type
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = p_column;

    IF v_current_type IS NULL THEN
        SET v_msg = CONCAT('V98 column missing: ', p_table, '.', p_column);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF LOWER(v_current_type) <> LOWER(p_expected_type) THEN
        SET @v98_widen_sql = CONCAT(
            'ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_column, '` ', p_definition
        );
        PREPARE v98_widen_stmt FROM @v98_widen_sql;
        EXECUTE v98_widen_stmt;
        DEALLOCATE PREPARE v98_widen_stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- 辅助过程 3：id 主键缺 AUTO_INCREMENT 时补齐
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v98_ensure_auto_increment;
DELIMITER $$
CREATE PROCEDURE p_v98_ensure_auto_increment(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_column_type VARCHAR(128) DEFAULT NULL;
    DECLARE v_column_key VARCHAR(8) DEFAULT NULL;
    DECLARE v_extra VARCHAR(128) DEFAULT NULL;
    DECLARE v_comment VARCHAR(1024) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COLUMN_TYPE, COLUMN_KEY, EXTRA, COLUMN_COMMENT
      INTO v_column_type, v_column_key, v_extra, v_comment
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = 'id';

    IF v_column_type IS NULL THEN
        SET v_msg = CONCAT('V98 id column missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF IFNULL(v_column_key, '') <> 'PRI' THEN
        SET v_msg = CONCAT('V98 id is not primary key: ', p_table, ' key=', IFNULL(v_column_key, ''));
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF IFNULL(v_extra, '') NOT LIKE '%auto_increment%' THEN
        SET @v98_ai_sql = CONCAT(
            'ALTER TABLE `', p_table, '` MODIFY COLUMN `id` ', v_column_type,
            ' NOT NULL AUTO_INCREMENT COMMENT ''', REPLACE(IFNULL(v_comment, ''), '''', ''''''), ''''
        );
        PREPARE v98_ai_stmt FROM @v98_ai_sql;
        EXECUTE v98_ai_stmt;
        DEALLOCATE PREPARE v98_ai_stmt;
    END IF;
END$$
DELIMITER ;

-- ------------------------------------------------------------
-- 辅助过程 4：索引不存在则创建（只用于非唯一索引）
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v98_ensure_index;
DELIMITER $$
CREATE PROCEDURE p_v98_ensure_index(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_definition VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @v98_idx_sql = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_definition);
        PREPARE v98_idx_stmt FROM @v98_idx_sql;
        EXECUTE v98_idx_stmt;
        DEALLOCATE PREPARE v98_idx_stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- erp_period：权威定义 V6（与 ErpPeriodDO 对齐）
-- ============================================================
CALL p_v98_add_column('erp_period', 'year', 'INT DEFAULT NULL COMMENT ''期间年度''');
CALL p_v98_add_column('erp_period', 'month', 'INT DEFAULT NULL COMMENT ''期间月份''');
CALL p_v98_add_column('erp_period', 'start_date', 'DATE DEFAULT NULL COMMENT ''起始日期''');
CALL p_v98_add_column('erp_period', 'end_date', 'DATE DEFAULT NULL COMMENT ''结束日期''');
CALL p_v98_add_column('erp_period', 'status', 'TINYINT NOT NULL DEFAULT 10 COMMENT ''状态''');
CALL p_v98_add_column('erp_period', 'closed_by', 'VARCHAR(64) DEFAULT NULL COMMENT ''关账人''');
CALL p_v98_add_column('erp_period', 'closed_time', 'DATETIME DEFAULT NULL COMMENT ''关账时间''');
CALL p_v98_add_column('erp_period', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');
-- V10 结构独有列：补为可空兼容列，使两种历史的列集合一致
CALL p_v98_add_column('erp_period', 'name', 'VARCHAR(128) DEFAULT NULL COMMENT ''期间名称（V10 兼容列）''');
CALL p_v98_add_column('erp_period', 'start_time', 'DATETIME DEFAULT NULL COMMENT ''起始时间（V10 兼容列）''');
CALL p_v98_add_column('erp_period', 'end_time', 'DATETIME DEFAULT NULL COMMENT ''结束时间（V10 兼容列）''');

-- ============================================================
-- erp_period_close：权威定义 V6（与 ErpPeriodCloseDO 对齐）
-- ============================================================
CALL p_v98_add_column('erp_period_close', 'period_code', 'VARCHAR(20) DEFAULT NULL COMMENT ''期间编码''');
CALL p_v98_add_column('erp_period_close', 'type', 'TINYINT DEFAULT NULL COMMENT ''处理类型''');
CALL p_v98_add_column('erp_period_close', 'executed_by', 'VARCHAR(64) DEFAULT NULL COMMENT ''执行人''');
CALL p_v98_add_column('erp_period_close', 'executed_time', 'DATETIME DEFAULT NULL COMMENT ''执行时间''');
CALL p_v98_add_column('erp_period_close', 'process_status', 'TINYINT NOT NULL DEFAULT 10 COMMENT ''处理状态''');
CALL p_v98_add_column('erp_period_close', 'summary', 'TEXT DEFAULT NULL COMMENT ''关键数据摘要''');
CALL p_v98_add_column('erp_period_close', 'adjustment_amount', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''调整金额''');
CALL p_v98_add_column('erp_period_close', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');
-- V10 结构独有列：补为可空兼容列
CALL p_v98_add_column('erp_period_close', 'close_time', 'DATETIME DEFAULT NULL COMMENT ''关闭时间（V10 兼容列）''');
CALL p_v98_add_column('erp_period_close', 'close_user', 'VARCHAR(64) DEFAULT NULL COMMENT ''关闭人（V10 兼容列）''');

-- ============================================================
-- ai_nl2sql_query_history：权威定义 V38，只加宽不收窄
--   * natural_language VARCHAR(255) -> TEXT（V73 结构会截断长问题）
--   * error_msg VARCHAR(255) -> VARCHAR(2000)（V73 结构会截断错误堆栈）
--   * data_source TEXT vs VARCHAR(64)、status INT vs TINYINT 属收窄方向，
--     与 DO（String / Integer）均可映射，留待数据清洗后单独迁移。
-- ============================================================
CALL p_v98_widen_column('ai_nl2sql_query_history', 'natural_language', 'text',
                        'TEXT DEFAULT NULL COMMENT ''自然语言问题''');
CALL p_v98_widen_column('ai_nl2sql_query_history', 'error_msg', 'varchar(2000)',
                        'VARCHAR(2000) DEFAULT NULL COMMENT ''错误信息''');

-- ============================================================
-- 回填：只对本次补列产生的空值生效，已有数据不覆盖
-- ============================================================
UPDATE erp_period
   SET year = CAST(LEFT(code, 4) AS UNSIGNED),
       month = CAST(RIGHT(code, 2) AS UNSIGNED)
 WHERE year IS NULL
   AND code REGEXP '^[0-9]{6}$';

UPDATE erp_period
   SET start_date = STR_TO_DATE(CONCAT(code, '01'), '%Y%m%d')
 WHERE start_date IS NULL
   AND code REGEXP '^[0-9]{6}$';

UPDATE erp_period
   SET end_date = LAST_DAY(start_date)
 WHERE end_date IS NULL
   AND start_date IS NOT NULL;

UPDATE erp_period_close c
  JOIN erp_period p ON p.id = c.period_id
   SET c.period_code = p.code
 WHERE c.period_code IS NULL
   AND p.code IS NOT NULL;

-- ============================================================
-- 主键自增与索引：V10 结构下 id 只有 PRIMARY KEY，缺 AUTO_INCREMENT
-- ============================================================
CALL p_v98_ensure_auto_increment('erp_period');
CALL p_v98_ensure_auto_increment('erp_period_close');

CALL p_v98_ensure_index('erp_period', 'idx_erp_period_tenant_code',
                        'KEY `idx_erp_period_tenant_code` (`tenant_id`, `code`, `deleted`)');
CALL p_v98_ensure_index('erp_period', 'idx_erp_period_year_month',
                        'KEY `idx_erp_period_year_month` (`year`, `month`)');
CALL p_v98_ensure_index('erp_period_close', 'idx_erp_period_close_period_code',
                        'KEY `idx_erp_period_close_period_code` (`period_code`)');

-- ============================================================
-- 收尾：删除临时过程，避免留在业务库里
-- ============================================================
DROP PROCEDURE IF EXISTS p_v98_add_column;
DROP PROCEDURE IF EXISTS p_v98_widen_column;
DROP PROCEDURE IF EXISTS p_v98_ensure_auto_increment;
DROP PROCEDURE IF EXISTS p_v98_ensure_index;

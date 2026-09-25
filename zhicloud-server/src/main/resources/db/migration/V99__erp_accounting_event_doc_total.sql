-- ============================================================
-- V99: erp_accounting_event 金额勾稽总额列
--
-- 背景：
--   V96 把来源唯一键扩展为包含 sequence_no 之后，同一来源单据在同一事件类型下
--   可以落地 1..N 条期次事件（分批开票 / 分期收款 / 分期付款）。
--   结构允许了，但金额没有上界：同一单据的期次事件金额可以无限累加，
--   既没有数据库约束，也没有应用层校验，存在「累计过账金额超过单据金额」的
--   超额过账风险（已登记在 07-风险与未验证假设.md）。
--
-- 目标：
--   1. 为 erp_accounting_event 增加 doc_total_amount（来源单据勾稽总额）。
--   2. 该列是应用层「同源单据累计金额勾稽守卫」的落库基线，使超额判定可审计、
--      可复算，而不是只依赖调用方临时传入的参数。
--
-- 语义：
--   * doc_total_amount 为 NULL 表示「该事件未参与勾稽守卫」（历史数据默认）。
--   * 非 NULL 时，同一 (tenant_id, event_type, source_doc_type, source_doc_id)
--     下全部事件（含冲销事件）的 amount_with_tax 带符号求和，其绝对值不得大于
--     doc_total_amount。带符号求和使「原事件 + 冲销事件」自动相互抵消，因此已
--     冲销的额度可以被重新使用。
--   * 求和范围刻意不过滤 reversal_of_event_id：冲销事件是负向事件，承担“释放
--     额度”的作用；若把它排除在求和之外，已冲销额度不会释放，反审核后重新过账
--     会被误判为超额。应用层守卫与本脚本必须使用同一求和范围，否则体检会假阳性。
--
-- 兼容性：
--   * 通过 information_schema 判存在后执行，可重复执行。
--   * 不回填历史数据：历史事件的 doc_total_amount 保持 NULL，不参与守卫。
--     这是刻意的——历史单据总额无法从事件表反推，凭猜测回填会把错误的
--     上界写进审计记录，比留空更危险。
--
-- 限界：
--   * 本迁移只提供列，不提供约束。金额上界依赖应用层守卫与调用方传入的
--     单据总额；数据库层面没有 CHECK 约束兜底（MySQL 8.0.16+ 虽支持 CHECK，
--     但该约束跨行聚合，无法用 CHECK 表达，只能用触发器或对账作业实现）。
--   * 不加索引：勾稽查询条件 (tenant_id, event_type, source_doc_type,
--     source_doc_id) 是 uk_erp_accounting_event_source 的最左前缀，已有索引覆盖，
--     额外建索引只会增加写放大与 ALTER 锁表时间。
--   * 对已有大表的 ALTER TABLE 可能触发表重建或锁表，生产窗口、在线 DDL
--     和回滚方案必须先在预生产同规模数据上演练与计时。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 金额勾稽总额列幂等补齐
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v99_add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_v99_add_column_if_not_exists(
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
        SET v_msg = CONCAT('V99 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @v99_column_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition
        );
        PREPARE v99_column_stmt FROM @v99_column_sql;
        EXECUTE v99_column_stmt;
        DEALLOCATE PREPARE v99_column_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v99_add_column_if_not_exists(
    'erp_accounting_event',
    'doc_total_amount',
    'DECIMAL(20,4) DEFAULT NULL COMMENT ''来源单据勾稽总额（NULL 表示未参与勾稽守卫）''');

DROP PROCEDURE IF EXISTS p_v99_add_column_if_not_exists;

-- 回滚：该列未被业务写入前可停写后 DROP COLUMN；一旦守卫已依赖该列作为基线，
-- 删除会让后续事件失去上界，必须先停写会计事件并导出基线后再执行。

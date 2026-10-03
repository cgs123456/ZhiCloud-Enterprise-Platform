-- ============================================================
-- V93: ERP 税务表纳入 Flyway 权威迁移路径
--
-- 背景：
--   1. erp_tax_rate / erp_tax_invoice / erp_tax_invoice_line 原先只在
--      sql/mysql/erp_tax.sql 中定义，自动建库不会创建三张表。
--   2. 旧手工脚本的 id 只有 PRIMARY KEY，没有 AUTO_INCREMENT；MyBatis-Plus
--      在 MySQL 下按 AUTO 生成 id，会直接触发 1364 或写 0 后主键冲突。
--   3. 旧发票唯一键 uk_invoice_no_code 没有 tenant_id，会错误禁止不同租户
--      使用相同发票号；本迁移改为租户维度唯一键。
--
-- 兼容性：
--   * 空库：CREATE TABLE IF NOT EXISTS 直接创建最终结构。
--   * 旧库：通过 information_schema 幂等补列、补 AUTO_INCREMENT、补租户唯一键。
--   * 可重复执行：所有新增结构均先判存在，重复执行不重复改表。
--
-- 限界：
--   * 本脚本只负责结构迁移，不伪造或回填业务发票数据。
--   * 对已有大量数据的表，ALTER TABLE 仍可能触发表重建/锁表；生产窗口和
--     gh-ost/pt-online-schema-change 评估见 V93_verify.sql。
-- ============================================================

CREATE TABLE IF NOT EXISTS erp_tax_rate (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code` VARCHAR(64) NOT NULL COMMENT '税率编码',
    `name` VARCHAR(128) NOT NULL COMMENT '税率名称',
    `rate_type` TINYINT NOT NULL COMMENT '税率类型（10 增值税 20 消费税 30 附加税）',
    `rate` DECIMAL(10,6) NOT NULL COMMENT '税率（小数，如 0.13 表示 13%）',
    `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认（0 否 1 是）',
    `effective_date` DATE DEFAULT NULL COMMENT '生效日期',
    `expiry_date` DATE DEFAULT NULL COMMENT '失效日期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_tax_rate_code_tenant` (`tenant_id`, `code`),
    KEY `idx_erp_tax_rate_status` (`tenant_id`, `status`),
    KEY `idx_erp_tax_rate_effective_date` (`tenant_id`, `effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 税率表';

CREATE TABLE IF NOT EXISTS erp_tax_invoice (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `invoice_no` VARCHAR(64) NOT NULL COMMENT '发票号',
    `invoice_code` VARCHAR(64) NOT NULL COMMENT '发票代码',
    `invoice_type` TINYINT NOT NULL COMMENT '发票类型（10 销项专票 20 销项普票 30 进项专票 40 进项普票）',
    `buyer_name` VARCHAR(128) DEFAULT NULL COMMENT '购方名称',
    `buyer_tax_no` VARCHAR(64) DEFAULT NULL COMMENT '购方税号',
    `seller_name` VARCHAR(128) DEFAULT NULL COMMENT '销方名称',
    `seller_tax_no` VARCHAR(64) DEFAULT NULL COMMENT '销方税号',
    `invoice_date` DATE DEFAULT NULL COMMENT '开票日期',
    `amount_without_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '不含税金额',
    `tax_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '税额',
    `amount_with_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '价税合计',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 已开具 30 已作废 40 已红冲）',
    `source_order_type` VARCHAR(64) DEFAULT NULL COMMENT '来源单据类型',
    `source_order_id` BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_tax_invoice_no_code_tenant` (`tenant_id`, `invoice_no`, `invoice_code`),
    KEY `idx_erp_tax_invoice_no_code` (`invoice_no`, `invoice_code`),
    KEY `idx_erp_tax_invoice_buyer_tax_no` (`tenant_id`, `buyer_tax_no`),
    KEY `idx_erp_tax_invoice_seller_tax_no` (`tenant_id`, `seller_tax_no`),
    KEY `idx_erp_tax_invoice_date` (`tenant_id`, `invoice_date`),
    KEY `idx_erp_tax_invoice_source` (`tenant_id`, `source_order_type`, `source_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 发票主表';

CREATE TABLE IF NOT EXISTS erp_tax_invoice_line (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `invoice_id` BIGINT NOT NULL COMMENT '发票 ID',
    `line_no` INT NOT NULL COMMENT '行号',
    `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称',
    `specification` VARCHAR(255) DEFAULT NULL COMMENT '规格型号',
    `unit` VARCHAR(32) DEFAULT NULL COMMENT '单位',
    `quantity` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '数量',
    `unit_price` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '单价（不含税）',
    `amount_without_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '不含税金额',
    `tax_rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '税率（小数）',
    `tax_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '税额',
    `amount_with_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '价税合计',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_tax_invoice_line_invoice_line_tenant` (`tenant_id`, `invoice_id`, `line_no`),
    KEY `idx_erp_tax_invoice_line_invoice_id` (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 发票明细表';

-- ============================================================
-- 旧表表选项收敛：统一 ENGINE 与字符集/排序规则。
-- 仅当检测到漂移时执行，避免重复迁移无条件重建表。
-- ============================================================
DROP PROCEDURE IF EXISTS p_v93_normalize_table_options;
DELIMITER $$
CREATE PROCEDURE p_v93_normalize_table_options(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_engine VARCHAR(64) DEFAULT NULL;
    DECLARE v_collation VARCHAR(64) DEFAULT NULL;
    DECLARE v_sql VARCHAR(1024) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT ENGINE, TABLE_COLLATION
      INTO v_engine, v_collation
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = p_table;

    IF v_engine IS NULL THEN
        SET v_msg = CONCAT('V93 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    SET v_sql = CONCAT('ALTER TABLE `', p_table, '`');
    IF LOWER(IFNULL(v_engine, '')) <> 'innodb' THEN
        SET v_sql = CONCAT(v_sql, ' ENGINE=InnoDB');
    END IF;
    IF LOWER(IFNULL(v_collation, '')) <> 'utf8mb4_unicode_ci' THEN
        IF LOWER(IFNULL(v_engine, '')) <> 'innodb' THEN
            SET v_sql = CONCAT(v_sql, ',');
        END IF;
        SET v_sql = CONCAT(v_sql, ' CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
    END IF;

    IF LOWER(IFNULL(v_engine, '')) <> 'innodb'
       OR LOWER(IFNULL(v_collation, '')) <> 'utf8mb4_unicode_ci' THEN
        SET @v93_table_options_sql = v_sql;
        PREPARE v93_table_options_stmt FROM @v93_table_options_sql;
        EXECUTE v93_table_options_stmt;
        DEALLOCATE PREPARE v93_table_options_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v93_normalize_table_options('erp_tax_rate');
CALL p_v93_normalize_table_options('erp_tax_invoice');
CALL p_v93_normalize_table_options('erp_tax_invoice_line');
-- ============================================================
-- 旧表补齐辅助过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_v93_add_column;
DELIMITER $$
CREATE PROCEDURE p_v93_add_column(
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
        SET v_msg = CONCAT('V93 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @v93_add_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition
        );
        PREPARE v93_add_stmt FROM @v93_add_sql;
        EXECUTE v93_add_stmt;
        DEALLOCATE PREPARE v93_add_stmt;
    END IF;
END$$
DELIMITER ;

-- erp_tax_rate：旧手工 DDL 的业务列与 BaseDO 列
CALL p_v93_add_column('erp_tax_rate', 'code', 'VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''税率编码''');
CALL p_v93_add_column('erp_tax_rate', 'name', 'VARCHAR(128) NOT NULL DEFAULT '''' COMMENT ''税率名称''');
CALL p_v93_add_column('erp_tax_rate', 'rate_type', 'TINYINT NOT NULL DEFAULT 10 COMMENT ''税率类型''');
CALL p_v93_add_column('erp_tax_rate', 'rate', 'DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT ''税率''');
CALL p_v93_add_column('erp_tax_rate', 'is_default', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否默认''');
CALL p_v93_add_column('erp_tax_rate', 'effective_date', 'DATE DEFAULT NULL COMMENT ''生效日期''');
CALL p_v93_add_column('erp_tax_rate', 'expiry_date', 'DATE DEFAULT NULL COMMENT ''失效日期''');
CALL p_v93_add_column('erp_tax_rate', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');
CALL p_v93_add_column('erp_tax_rate', 'status', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''状态''');
CALL p_v93_add_column('erp_tax_rate', 'creator', 'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL p_v93_add_column('erp_tax_rate', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL p_v93_add_column('erp_tax_rate', 'updater', 'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL p_v93_add_column('erp_tax_rate', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL p_v93_add_column('erp_tax_rate', 'deleted', 'BIT(1) NOT NULL DEFAULT 0 COMMENT ''是否删除''');
CALL p_v93_add_column('erp_tax_rate', 'tenant_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''租户 ID''');

-- erp_tax_invoice：旧手工 DDL 的业务列与 BaseDO 列
CALL p_v93_add_column('erp_tax_invoice', 'invoice_no', 'VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''发票号''');
CALL p_v93_add_column('erp_tax_invoice', 'invoice_code', 'VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''发票代码''');
CALL p_v93_add_column('erp_tax_invoice', 'invoice_type', 'TINYINT NOT NULL DEFAULT 10 COMMENT ''发票类型''');
CALL p_v93_add_column('erp_tax_invoice', 'buyer_name', 'VARCHAR(128) DEFAULT NULL COMMENT ''购方名称''');
CALL p_v93_add_column('erp_tax_invoice', 'buyer_tax_no', 'VARCHAR(64) DEFAULT NULL COMMENT ''购方税号''');
CALL p_v93_add_column('erp_tax_invoice', 'seller_name', 'VARCHAR(128) DEFAULT NULL COMMENT ''销方名称''');
CALL p_v93_add_column('erp_tax_invoice', 'seller_tax_no', 'VARCHAR(64) DEFAULT NULL COMMENT ''销方税号''');
CALL p_v93_add_column('erp_tax_invoice', 'invoice_date', 'DATE DEFAULT NULL COMMENT ''开票日期''');
CALL p_v93_add_column('erp_tax_invoice', 'amount_without_tax', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''不含税金额''');
CALL p_v93_add_column('erp_tax_invoice', 'tax_amount', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''税额''');
CALL p_v93_add_column('erp_tax_invoice', 'amount_with_tax', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''价税合计''');
CALL p_v93_add_column('erp_tax_invoice', 'status', 'TINYINT NOT NULL DEFAULT 10 COMMENT ''状态''');
CALL p_v93_add_column('erp_tax_invoice', 'source_order_type', 'VARCHAR(64) DEFAULT NULL COMMENT ''来源单据类型''');
CALL p_v93_add_column('erp_tax_invoice', 'source_order_id', 'BIGINT DEFAULT NULL COMMENT ''来源单据 ID''');
CALL p_v93_add_column('erp_tax_invoice', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');
CALL p_v93_add_column('erp_tax_invoice', 'creator', 'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL p_v93_add_column('erp_tax_invoice', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL p_v93_add_column('erp_tax_invoice', 'updater', 'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL p_v93_add_column('erp_tax_invoice', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL p_v93_add_column('erp_tax_invoice', 'deleted', 'BIT(1) NOT NULL DEFAULT 0 COMMENT ''是否删除''');
CALL p_v93_add_column('erp_tax_invoice', 'tenant_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''租户 ID''');

-- erp_tax_invoice_line：旧手工 DDL 的业务列与 BaseDO 列
CALL p_v93_add_column('erp_tax_invoice_line', 'invoice_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''发票 ID''');
CALL p_v93_add_column('erp_tax_invoice_line', 'line_no', 'INT NOT NULL DEFAULT 0 COMMENT ''行号''');
CALL p_v93_add_column('erp_tax_invoice_line', 'product_name', 'VARCHAR(128) NOT NULL DEFAULT '''' COMMENT ''商品名称''');
CALL p_v93_add_column('erp_tax_invoice_line', 'specification', 'VARCHAR(255) DEFAULT NULL COMMENT ''规格型号''');
CALL p_v93_add_column('erp_tax_invoice_line', 'unit', 'VARCHAR(32) DEFAULT NULL COMMENT ''单位''');
CALL p_v93_add_column('erp_tax_invoice_line', 'quantity', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''数量''');
CALL p_v93_add_column('erp_tax_invoice_line', 'unit_price', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''单价''');
CALL p_v93_add_column('erp_tax_invoice_line', 'amount_without_tax', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''不含税金额''');
CALL p_v93_add_column('erp_tax_invoice_line', 'tax_rate', 'DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT ''税率''');
CALL p_v93_add_column('erp_tax_invoice_line', 'tax_amount', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''税额''');
CALL p_v93_add_column('erp_tax_invoice_line', 'amount_with_tax', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''价税合计''');
CALL p_v93_add_column('erp_tax_invoice_line', 'remark', 'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');
CALL p_v93_add_column('erp_tax_invoice_line', 'creator', 'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL p_v93_add_column('erp_tax_invoice_line', 'create_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL p_v93_add_column('erp_tax_invoice_line', 'updater', 'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL p_v93_add_column('erp_tax_invoice_line', 'update_time', 'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL p_v93_add_column('erp_tax_invoice_line', 'deleted', 'BIT(1) NOT NULL DEFAULT 0 COMMENT ''是否删除''');
CALL p_v93_add_column('erp_tax_invoice_line', 'tenant_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''租户 ID''');

-- ============================================================
-- 旧库结构收敛：把旧手工 DDL 中“可空/无默认值”的列收敛为最终定义。
--
-- 说明：
--   * 只有检测到目标列可空、类型不符或缺少 ON UPDATE 时才执行 ALTER，重复执行
--     不会无条件重建表。
--   * 若待收敛列已存在 NULL，本迁移显式 SIGNAL，不静默把 NULL 改写成 0 或当前
--     时间；生产环境必须先由数据负责人确认并修复 NULL 后再重跑。
--   * 每张表的所有 MODIFY 合并为一条 ALTER，避免逐列触发多次表重建。
-- ============================================================
DROP PROCEDURE IF EXISTS p_v93_normalize_legacy_columns;
DELIMITER $$
CREATE PROCEDURE p_v93_normalize_legacy_columns(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_needs_normalization INT DEFAULT 0;
    DECLARE v_null_rows BIGINT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_needs_normalization
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND (
           (column_name = 'is_default'
            AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'tinyint'))
           OR (column_name = 'status'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'tinyint'))
           OR (column_name = 'amount_without_tax'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'decimal(20,4)'))
           OR (column_name = 'tax_amount'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'decimal(20,4)'))
           OR (column_name = 'amount_with_tax'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'decimal(20,4)'))
           OR (column_name = 'quantity'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'decimal(20,4)'))
           OR (column_name = 'unit_price'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'decimal(20,4)'))
           OR (column_name = 'tax_rate'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'decimal(10,6)'))
           OR (column_name = 'create_time'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'datetime'))
           OR (column_name = 'update_time'
               AND (IS_NULLABLE = 'YES'
                    OR LOWER(COLUMN_TYPE) <> 'datetime'
                    OR LOCATE('on update current_timestamp', LOWER(IFNULL(EXTRA, ''))) = 0))
           OR (column_name = 'deleted'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'bit(1)'))
           OR (column_name = 'tenant_id'
               AND (IS_NULLABLE = 'YES' OR LOWER(COLUMN_TYPE) <> 'bigint'))
       );

    IF v_needs_normalization > 0 THEN
        IF p_table = 'erp_tax_rate' THEN
            SELECT COUNT(*) INTO v_null_rows
              FROM erp_tax_rate
             WHERE is_default IS NULL
                OR status IS NULL
                OR create_time IS NULL
                OR update_time IS NULL
                OR deleted IS NULL
                OR tenant_id IS NULL;
            IF v_null_rows > 0 THEN
                SET v_msg = CONCAT('V93 cannot normalize erp_tax_rate: ', v_null_rows,
                                   ' rows contain NULL in NOT NULL target columns');
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;

            ALTER TABLE `erp_tax_rate`
                MODIFY COLUMN `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认（0 否 1 是）',
                MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
                MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                MODIFY COLUMN `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
                MODIFY COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID';
        ELSEIF p_table = 'erp_tax_invoice' THEN
            SELECT COUNT(*) INTO v_null_rows
              FROM erp_tax_invoice
             WHERE amount_without_tax IS NULL
                OR tax_amount IS NULL
                OR amount_with_tax IS NULL
                OR status IS NULL
                OR create_time IS NULL
                OR update_time IS NULL
                OR deleted IS NULL
                OR tenant_id IS NULL;
            IF v_null_rows > 0 THEN
                SET v_msg = CONCAT('V93 cannot normalize erp_tax_invoice: ', v_null_rows,
                                   ' rows contain NULL in NOT NULL target columns');
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;

            ALTER TABLE `erp_tax_invoice`
                MODIFY COLUMN `amount_without_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '不含税金额',
                MODIFY COLUMN `tax_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '税额',
                MODIFY COLUMN `amount_with_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '价税合计',
                MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 已开具 30 已作废 40 已红冲）',
                MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                MODIFY COLUMN `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
                MODIFY COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID';
        ELSEIF p_table = 'erp_tax_invoice_line' THEN
            SELECT COUNT(*) INTO v_null_rows
              FROM erp_tax_invoice_line
             WHERE quantity IS NULL
                OR unit_price IS NULL
                OR amount_without_tax IS NULL
                OR tax_rate IS NULL
                OR tax_amount IS NULL
                OR amount_with_tax IS NULL
                OR create_time IS NULL
                OR update_time IS NULL
                OR deleted IS NULL
                OR tenant_id IS NULL;
            IF v_null_rows > 0 THEN
                SET v_msg = CONCAT('V93 cannot normalize erp_tax_invoice_line: ', v_null_rows,
                                   ' rows contain NULL in NOT NULL target columns');
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;

            ALTER TABLE `erp_tax_invoice_line`
                MODIFY COLUMN `quantity` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '数量',
                MODIFY COLUMN `unit_price` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '单价（不含税）',
                MODIFY COLUMN `amount_without_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '不含税金额',
                MODIFY COLUMN `tax_rate` DECIMAL(10,6) NOT NULL DEFAULT 0 COMMENT '税率（小数）',
                MODIFY COLUMN `tax_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '税额',
                MODIFY COLUMN `amount_with_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '价税合计',
                MODIFY COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                MODIFY COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                MODIFY COLUMN `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
                MODIFY COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID';
        ELSE
            SET v_msg = CONCAT('V93 unknown table for normalization: ', p_table);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL p_v93_normalize_legacy_columns('erp_tax_rate');
CALL p_v93_normalize_legacy_columns('erp_tax_invoice');
CALL p_v93_normalize_legacy_columns('erp_tax_invoice_line');

-- ============================================================
-- 幂等补齐 AUTO_INCREMENT
-- ============================================================
DROP PROCEDURE IF EXISTS p_v93_ensure_auto_increment;
DELIMITER $$
CREATE PROCEDURE p_v93_ensure_auto_increment(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_exists INT DEFAULT 0;
    DECLARE v_column_type VARCHAR(128) DEFAULT NULL;
    DECLARE v_column_key VARCHAR(8) DEFAULT NULL;
    DECLARE v_extra VARCHAR(128) DEFAULT NULL;
    DECLARE v_comment VARCHAR(1024) DEFAULT NULL;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SELECT COUNT(*) INTO v_exists
      FROM information_schema.tables
     WHERE table_schema = DATABASE()
       AND table_name = p_table;
    IF v_exists = 0 THEN
        SET v_msg = CONCAT('V93 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    SELECT COLUMN_TYPE, COLUMN_KEY, EXTRA, COLUMN_COMMENT
      INTO v_column_type, v_column_key, v_extra, v_comment
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND column_name = 'id';
    IF v_column_type IS NULL THEN
        SET v_msg = CONCAT('V93 id column missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;
    IF IFNULL(v_column_key, '') <> 'PRI' THEN
        SET v_msg = CONCAT('V93 id is not primary key: ', p_table, ' key=', v_column_key);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF IFNULL(v_extra, '') NOT LIKE '%auto_increment%' THEN
        SET @v93_auto_sql = CONCAT(
            'ALTER TABLE `', p_table, '` MODIFY COLUMN `id` ', v_column_type,
            ' NOT NULL AUTO_INCREMENT',
            IF(v_comment IS NULL OR v_comment = '', '', CONCAT(' COMMENT ', QUOTE(v_comment)))
        );
        PREPARE v93_auto_stmt FROM @v93_auto_sql;
        EXECUTE v93_auto_stmt;
        DEALLOCATE PREPARE v93_auto_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v93_ensure_auto_increment('erp_tax_rate');
CALL p_v93_ensure_auto_increment('erp_tax_invoice');
CALL p_v93_ensure_auto_increment('erp_tax_invoice_line');

-- ============================================================
-- 幂等补齐租户维度唯一键
-- ============================================================
DROP PROCEDURE IF EXISTS p_v93_ensure_unique_index;
DELIMITER $$
CREATE PROCEDURE p_v93_ensure_unique_index(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(500)
)
BEGIN
    DECLARE v_index_exists INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    SET @v93_dup_count = 0;
    SELECT COUNT(*) INTO v_index_exists
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND table_name = p_table
       AND index_name = p_index;

    IF v_index_exists = 0 THEN
        SET @v93_dup_sql = CONCAT(
            'SELECT COUNT(*) INTO @v93_dup_count FROM `', p_table, '` ',
            'GROUP BY ', p_columns, ' HAVING COUNT(*) > 1 LIMIT 1'
        );
        PREPARE v93_dup_stmt FROM @v93_dup_sql;
        EXECUTE v93_dup_stmt;
        DEALLOCATE PREPARE v93_dup_stmt;

        IF @v93_dup_count > 0 THEN
            SET v_msg = CONCAT('V93 duplicate rows block unique index ', p_index, ' on ', p_table);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;

        SET @v93_index_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_columns, ')'
        );
        PREPARE v93_index_stmt FROM @v93_index_sql;
        EXECUTE v93_index_stmt;
        DEALLOCATE PREPARE v93_index_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v93_ensure_unique_index('erp_tax_rate', 'uk_erp_tax_rate_code_tenant', '`tenant_id`, `code`');
CALL p_v93_ensure_unique_index('erp_tax_invoice', 'uk_erp_tax_invoice_no_code_tenant', '`tenant_id`, `invoice_no`, `invoice_code`');
CALL p_v93_ensure_unique_index('erp_tax_invoice_line', 'uk_erp_tax_invoice_line_invoice_line_tenant', '`tenant_id`, `invoice_id`, `line_no`');

-- 旧全局发票唯一键会阻止不同租户使用相同发票号；新租户唯一键已建立后再删除。
DROP PROCEDURE IF EXISTS p_v93_drop_index_if_exists;
DELIMITER $$
CREATE PROCEDURE p_v93_drop_index_if_exists(IN p_table VARCHAR(64), IN p_index VARCHAR(64))
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @v93_drop_sql = CONCAT('ALTER TABLE `', p_table, '` DROP INDEX `', p_index, '`');
        PREPARE v93_drop_stmt FROM @v93_drop_sql;
        EXECUTE v93_drop_stmt;
        DEALLOCATE PREPARE v93_drop_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v93_drop_index_if_exists('erp_tax_invoice', 'uk_invoice_no_code');

-- 收尾：删除仅用于本次迁移的临时过程。
DROP PROCEDURE IF EXISTS p_v93_normalize_table_options;
DROP PROCEDURE IF EXISTS p_v93_add_column;
DROP PROCEDURE IF EXISTS p_v93_normalize_legacy_columns;
DROP PROCEDURE IF EXISTS p_v93_ensure_auto_increment;
DROP PROCEDURE IF EXISTS p_v93_ensure_unique_index;
DROP PROCEDURE IF EXISTS p_v93_drop_index_if_exists;

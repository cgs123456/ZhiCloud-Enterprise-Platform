-- ============================================================
-- V94: ERP 会计事件、过账结果、规则、结算分配与发票来源底座
--
-- 目标：
--   1. 建立业务单据到总账之间的可追溯会计事件表。
--   2. 建立事件到凭证的过账结果表和版本化过账规则表。
--   3. 建立付款/收款明细到发票及业务来源的结算分配表。
--   4. 建立发票行到入库/出库/退货行的来源关联表。
--   5. 为 erp_gl_voucher 补充来源字段和审计检索索引。
--
-- 兼容性：
--   * 六张业务表均为新增表，空库直接创建最终结构。
--   * erp_gl_voucher 来源字段通过 information_schema 判存在后补齐，可重复执行。
--   * 不修改或回填历史凭证，不伪造历史业务与会计数据。
--
-- 限界：
--   * 本迁移只提供结构底座，不证明 Java 过账链路、规则正确性或税务口径正确。
--   * 对已有大表的 ALTER TABLE 可能触发表重建或锁表，生产窗口、在线 DDL
--     和回滚方案必须先在预生产同规模数据上演练。
-- ============================================================

CREATE TABLE IF NOT EXISTS erp_accounting_event (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id` VARCHAR(64) NOT NULL COMMENT '全局业务事件 ID（ULID/UUID）',
    `event_type` VARCHAR(64) NOT NULL COMMENT '会计事件类型',
    `source_doc_type` VARCHAR(64) NOT NULL COMMENT '来源单据类型',
    `source_doc_id` BIGINT NOT NULL COMMENT '来源单据 ID',
    `source_doc_no` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源单据编号',
    `source_doc_line_id` BIGINT NOT NULL DEFAULT 0 COMMENT '来源单据行 ID',
    `source_revision` INT NOT NULL DEFAULT 0 COMMENT '来源单据版本号',
    `event_date` DATE NOT NULL COMMENT '业务发生日期',
    `accounting_date` DATE NOT NULL COMMENT '会计日期',
    `period_id` BIGINT NOT NULL DEFAULT 0 COMMENT '会计期间 ID',
    `period_code` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '会计期间编码',
    `amount_without_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '不含税金额',
    `tax_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '税额',
    `amount_with_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '价税合计',
    `cost_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '成本金额（销售成本结转等，非成本类事件为 0）',
    `currency` VARCHAR(8) NOT NULL DEFAULT 'cny' COMMENT '币种',
    `exchange_rate` DECIMAL(20,8) NOT NULL DEFAULT 1 COMMENT '汇率',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 新建 10 过账中 20 已过账 30 失败 40 已冲销）',
    `idempotency_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
    `rule_set_version` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '规则集版本',
    `payload_hash` CHAR(64) NOT NULL COMMENT '业务载荷 SHA-256',
    `reversal_of_event_id` BIGINT NOT NULL DEFAULT 0 COMMENT '冲销的原事件 ID，未冲销为 0',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    `next_retry_time` DATETIME DEFAULT NULL COMMENT '下次重试时间',
    `last_error` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最后错误',
    `occurred_at` DATETIME NOT NULL COMMENT '业务事件发生时间',
    `posted_at` DATETIME DEFAULT NULL COMMENT '过账完成时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_accounting_event_business_id` (`tenant_id`, `event_id`),
    UNIQUE KEY `uk_erp_accounting_event_idempotency` (`tenant_id`, `idempotency_key`),
    UNIQUE KEY `uk_erp_accounting_event_source` (`tenant_id`, `event_type`, `source_doc_type`, `source_doc_id`, `source_doc_line_id`, `source_revision`, `reversal_of_event_id`),
    KEY `idx_erp_accounting_event_status_retry` (`tenant_id`, `status`, `next_retry_time`),
    KEY `idx_erp_accounting_event_accounting_date` (`tenant_id`, `accounting_date`, `status`),
    KEY `idx_erp_accounting_event_source_no` (`tenant_id`, `source_doc_type`, `source_doc_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 会计事件表';

CREATE TABLE IF NOT EXISTS erp_accounting_posting (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `event_id` VARCHAR(64) NOT NULL COMMENT '会计事件 ID',
    `voucher_id` BIGINT NOT NULL DEFAULT 0 COMMENT '凭证 ID',
    `posting_rule_id` BIGINT NOT NULL DEFAULT 0 COMMENT '过账规则 ID',
    `rule_version` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '规则版本',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 新建 10 过账中 20 已过账 30 失败 40 已冲销）',
    `attempt_no` INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
    `error_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '错误编码',
    `error_message` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '错误信息',
    `posted_at` DATETIME DEFAULT NULL COMMENT '过账完成时间',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_accounting_posting_event_rule` (`tenant_id`, `event_id`, `posting_rule_id`, `rule_version`),
    KEY `idx_erp_accounting_posting_voucher` (`tenant_id`, `voucher_id`),
    KEY `idx_erp_accounting_posting_status` (`tenant_id`, `status`, `posted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 会计过账结果表';

CREATE TABLE IF NOT EXISTS erp_posting_rule (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
    `version` VARCHAR(32) NOT NULL COMMENT '规则版本',
    `event_type` VARCHAR(64) NOT NULL COMMENT '适用事件类型',
    `condition_json` TEXT COMMENT '规则头匹配条件 JSON',
    `effective_from` DATETIME NOT NULL COMMENT '生效时间',
    `effective_to` DATETIME DEFAULT NULL COMMENT '失效时间',
    `enabled` BIT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_posting_rule_code_version` (`tenant_id`, `rule_code`, `version`),
    KEY `idx_erp_posting_rule_event_effective` (`tenant_id`, `event_type`, `enabled`, `effective_from`, `effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 过账规则头表';

CREATE TABLE IF NOT EXISTS erp_posting_rule_line (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_id` BIGINT NOT NULL COMMENT '过账规则 ID',
    `line_no` INT NOT NULL COMMENT '规则内分录行号',
    `entry_side` TINYINT NOT NULL COMMENT '借贷方向（10 借 20 贷）',
    `account_code` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '固定科目编码',
    `account_resolver` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '动态科目解析器编码',
    `amount_expression` VARCHAR(255) NOT NULL COMMENT '金额表达式，只允许引用受限事件字段',
    `condition_json` TEXT COMMENT '行级匹配条件 JSON',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_posting_rule_line_no` (`tenant_id`, `rule_id`, `line_no`),
    KEY `idx_erp_posting_rule_line_rule` (`tenant_id`, `rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 过账规则分录行表';

CREATE TABLE IF NOT EXISTS erp_finance_settlement_allocation (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `finance_doc_type` VARCHAR(64) NOT NULL COMMENT '财务单据类型（payment/receipt）',
    `finance_doc_id` BIGINT NOT NULL COMMENT '财务单据 ID',
    `finance_doc_item_id` BIGINT NOT NULL COMMENT '财务单据明细 ID',
    `invoice_id` BIGINT NOT NULL DEFAULT 0 COMMENT '发票 ID，预付款场景可为 0',
    `invoice_line_id` BIGINT NOT NULL DEFAULT 0 COMMENT '发票行 ID，预付款场景可为 0',
    `source_doc_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '业务来源单据类型',
    `source_doc_id` BIGINT NOT NULL DEFAULT 0 COMMENT '业务来源单据 ID',
    `allocated_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '本次分配金额',
    `currency` VARCHAR(8) NOT NULL DEFAULT 'cny' COMMENT '币种',
    `exchange_rate` DECIMAL(20,8) NOT NULL DEFAULT 1 COMMENT '汇率',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 新建 10 已过账 20 已冲销）',
    `reversal_of_allocation_id` BIGINT NOT NULL DEFAULT 0 COMMENT '冲销的原分配 ID，未冲销为 0',
    `allocated_at` DATETIME DEFAULT NULL COMMENT '核销完成时间',
    `last_error` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最后错误',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_finance_settlement_allocation_identity` (`tenant_id`, `finance_doc_type`, `finance_doc_item_id`, `invoice_id`, `invoice_line_id`, `source_doc_type`, `source_doc_id`, `reversal_of_allocation_id`),
    KEY `idx_erp_finance_settlement_allocation_finance_doc` (`tenant_id`, `finance_doc_type`, `finance_doc_id`),
    KEY `idx_erp_finance_settlement_allocation_invoice` (`tenant_id`, `invoice_id`, `invoice_line_id`),
    KEY `idx_erp_finance_settlement_allocation_source` (`tenant_id`, `source_doc_type`, `source_doc_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 财务结算分配表';

CREATE TABLE IF NOT EXISTS erp_tax_invoice_source (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `invoice_id` BIGINT NOT NULL COMMENT '发票 ID',
    `invoice_line_id` BIGINT NOT NULL COMMENT '发票行 ID',
    `source_doc_type` VARCHAR(64) NOT NULL COMMENT '来源单据类型',
    `source_doc_id` BIGINT NOT NULL COMMENT '来源单据 ID',
    `source_doc_line_id` BIGINT NOT NULL DEFAULT 0 COMMENT '来源单据行 ID',
    `quantity` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '来源数量',
    `amount_without_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '不含税金额',
    `tax_amount` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '税额',
    `amount_with_tax` DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '价税合计',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 新建 10 已匹配 20 已冲销）',
    `last_error` VARCHAR(500) NOT NULL DEFAULT '' COMMENT '最后错误',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_erp_tax_invoice_source_identity` (`tenant_id`, `invoice_line_id`, `source_doc_type`, `source_doc_id`, `source_doc_line_id`),
    KEY `idx_erp_tax_invoice_source_invoice` (`tenant_id`, `invoice_id`, `invoice_line_id`),
    KEY `idx_erp_tax_invoice_source_source_doc` (`tenant_id`, `source_doc_type`, `source_doc_id`, `source_doc_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 发票来源关联表';

-- ============================================================
-- erp_gl_voucher 来源字段幂等补齐
-- ============================================================
DROP PROCEDURE IF EXISTS p_v94_add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_v94_add_column_if_not_exists(
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
        SET v_msg = CONCAT('V94 table missing: ', p_table);
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND column_name = p_column
    ) THEN
        SET @v94_column_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition
        );
        PREPARE v94_column_stmt FROM @v94_column_sql;
        EXECUTE v94_column_stmt;
        DEALLOCATE PREPARE v94_column_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_event_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''来源会计事件 ID''');
CALL p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_doc_type', 'VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''来源单据类型''');
CALL p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_doc_id', 'BIGINT NOT NULL DEFAULT 0 COMMENT ''来源单据 ID''');
CALL p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_doc_no', 'VARCHAR(64) NOT NULL DEFAULT '''' COMMENT ''来源单据编号''');
CALL p_v94_add_column_if_not_exists('erp_accounting_event', 'cost_amount', 'DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT ''成本金额（销售成本结转等，非成本类事件为 0）''');

-- ============================================================
-- erp_gl_voucher 来源索引幂等补齐
-- ============================================================
DROP PROCEDURE IF EXISTS p_v94_add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_v94_add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_columns VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = p_table
           AND index_name = p_index
    ) THEN
        SET @v94_index_sql = CONCAT(
            'ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_columns, ')'
        );
        PREPARE v94_index_stmt FROM @v94_index_sql;
        EXECUTE v94_index_stmt;
        DEALLOCATE PREPARE v94_index_stmt;
    END IF;
END$$
DELIMITER ;

CALL p_v94_add_index_if_not_exists('erp_gl_voucher', 'idx_erp_gl_voucher_source_event', '`tenant_id`, `source_event_id`, `deleted`');
CALL p_v94_add_index_if_not_exists('erp_gl_voucher', 'idx_erp_gl_voucher_source_doc', '`tenant_id`, `source_doc_type`, `source_doc_id`, `deleted`');

DROP PROCEDURE IF EXISTS p_v94_add_column_if_not_exists;
DROP PROCEDURE IF EXISTS p_v94_add_index_if_not_exists;

-- 回滚：六张新增表未被业务引用前可停写后删除；erp_gl_voucher 的来源列和索引
-- 只有在确认无应用依赖、无历史查询依赖后，才由数据负责人审批回滚。

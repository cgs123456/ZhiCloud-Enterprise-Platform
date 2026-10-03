-- ============================================================
-- V113: 补齐租户过滤所需的 tenant_id 前缀索引
--
-- 背景：
--   真库 schema 审计发现 75 张目标域/基础域表的 tenant_id 上没有
--   “以 tenant_id 为首列”的索引。多租户拦截器会给查询追加 tenant_id
--   条件；缺少该索引时，大表查询可能退化为扫描后再过滤。
--
-- 处理：
--   1. 对审计确认存在的 75 张表逐一检查 information_schema。
--   2. 仅当表存在、tenant_id 列存在、且没有 tenant_id 前缀索引时，
--      增加普通索引 idx_v113_tenant(tenant_id)。
--   3. 已有 tenant_id 前缀索引的表跳过；不删除、不修改任何业务数据。
--   4. 迁移可重复执行；若表/列缺失或索引名冲突则 SIGNAL 失败，
--      不静默跳过结构性错误。
--
-- 上线边界：
--   ADD INDEX 可能产生临时空间、复制延迟和 MDL 等待。脚本显式要求
--   ALGORITHM=INPLACE、LOCK=NONE；若目标实例不支持在线加索引，迁移会失败，
--   而不是静默退化为锁表 DDL。生产执行前必须完成备份校验、容量评估、
--   复制延迟检查，并选择业务低峰窗口。本迁移不替代真实生产压测。
--
-- 目标表清单来自：
--   docs/enterprise-platform/evidence/production/schema-findings-triage-2026-09-27.json
-- 禁止将本迁移用于替代业务数据迁移或模拟数据导入。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v113_add_tenant_indexes;

DELIMITER $$
CREATE PROCEDURE p_v113_add_tenant_indexes()
BEGIN
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_table VARCHAR(64);
    DECLARE v_table_exists INT DEFAULT 0;
    DECLARE v_tenant_column_exists INT DEFAULT 0;
    DECLARE v_tenant_index_exists INT DEFAULT 0;
    DECLARE v_named_index_exists INT DEFAULT 0;
    DECLARE v_msg VARCHAR(255) DEFAULT NULL;

    DECLARE cur CURSOR FOR
        SELECT table_name FROM v113_tenant_index_targets ORDER BY table_name;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_table;
        IF v_done = 1 THEN
            LEAVE read_loop;
        END IF;

        SELECT COUNT(*) INTO v_table_exists
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = v_table;
        IF v_table_exists = 0 THEN
            SET v_msg = CONCAT('V113 table missing: ', v_table);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;

        SELECT COUNT(*) INTO v_tenant_column_exists
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = v_table
           AND column_name = 'tenant_id';
        IF v_tenant_column_exists = 0 THEN
            SET v_msg = CONCAT('V113 tenant_id missing: ', v_table);
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
        END IF;

        SELECT COUNT(*) INTO v_tenant_index_exists
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = v_table
           AND seq_in_index = 1
           AND column_name = 'tenant_id';

        IF v_tenant_index_exists = 0 THEN
            SELECT COUNT(*) INTO v_named_index_exists
              FROM information_schema.statistics
             WHERE table_schema = DATABASE()
               AND table_name = v_table
               AND index_name = 'idx_v113_tenant';
            IF v_named_index_exists > 0 THEN
                SET v_msg = CONCAT('V113 index name collision: ', v_table, '.idx_v113_tenant');
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_msg;
            END IF;

            SET @v113_sql = CONCAT(
                'ALTER TABLE `', v_table, '` ADD INDEX `idx_v113_tenant` (`tenant_id`), ',
                'ALGORITHM=INPLACE, LOCK=NONE'
            );
            PREPARE v113_stmt FROM @v113_sql;
            EXECUTE v113_stmt;
            DEALLOCATE PREPARE v113_stmt;
        END IF;
    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS v113_tenant_index_targets;
CREATE TEMPORARY TABLE v113_tenant_index_targets (
    table_name VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO v113_tenant_index_targets (table_name) VALUES
('crm_clue_pool_config'),
('crm_invoice'),
('crm_invoice_line'),
('erp_account_book'),
('erp_accounting_event'),
('erp_accounting_posting'),
('erp_bank_account'),
('erp_cash_flow'),
('erp_consolidation_scope'),
('erp_consolidation_worksheet'),
('erp_cost_allocation'),
('erp_cost_center'),
('erp_currency'),
('erp_exchange_rate'),
('erp_finance_settlement_allocation'),
('erp_fund_plan'),
('erp_gl_account'),
('erp_gl_voucher'),
('erp_gl_voucher_entry'),
('erp_period'),
('erp_period_close'),
('erp_posting_rule'),
('erp_posting_rule_line'),
('erp_profit_center'),
('erp_profitability_analysis'),
('erp_sale_out'),
('erp_sale_out_items'),
('erp_tax_invoice'),
('erp_tax_invoice_line'),
('erp_tax_invoice_source'),
('erp_tax_rate'),
('hr_contract'),
('hr_interview'),
('hr_job_posting'),
('hr_leave_balance'),
('hr_leave_request'),
('hr_leave_type'),
('hr_resume'),
('hr_social_insurance_base'),
('mdm_cross_ref'),
('mes_energy_consumption'),
('oa_announcement'),
('oa_approval_template'),
('oa_document'),
('oa_document_attachment'),
('oa_knowledge_article'),
('oa_knowledge_category'),
('oa_knowledge_comment'),
('oa_knowledge_version'),
('oa_meeting_reservation'),
('oa_meeting_room'),
('oa_reimburse'),
('oa_reimburse_item'),
('oa_schedule'),
('qms_capa_document'),
('qms_electronic_signature_log'),
('qms_fmea_document'),
('qms_fmea_item'),
('qms_inspection_item'),
('qms_inspection_order'),
('qms_inspection_record'),
('qms_msa_measurement'),
('qms_msa_study'),
('qms_ncr_document'),
('qms_ncr_mrb_record'),
('system_dept'),
('system_oauth2_access_token'),
('system_oauth2_refresh_token'),
('system_role'),
('system_role_menu'),
('system_sms_code'),
('system_social_client'),
('system_users'),
('tms_fleet_operation'),
('tms_freight_reconciliation')
;

CALL p_v113_add_tenant_indexes();

DROP TEMPORARY TABLE IF EXISTS v113_tenant_index_targets;
DROP PROCEDURE IF EXISTS p_v113_add_tenant_indexes;

-- 回滚：删除本迁移新增的 75 个 idx_v113_tenant 索引不会改变业务语义；
-- 但必须先确认没有查询计划/容量回归，再在维护窗口逐表回滚。

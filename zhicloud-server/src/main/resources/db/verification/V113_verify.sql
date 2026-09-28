-- ============================================================
-- V113 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 审计列出的 75 张表全部存在；
--   2. 每张表存在以 tenant_id 为首列的索引；
--   3. 不要求索引名称必须为 idx_v113_tenant，已有等价索引同样通过。
-- ============================================================
WITH expected_table AS (
    SELECT 'crm_clue_pool_config' AS table_name UNION ALL
    SELECT 'crm_invoice' AS table_name UNION ALL
    SELECT 'crm_invoice_line' AS table_name UNION ALL
    SELECT 'erp_account_book' AS table_name UNION ALL
    SELECT 'erp_accounting_event' AS table_name UNION ALL
    SELECT 'erp_accounting_posting' AS table_name UNION ALL
    SELECT 'erp_bank_account' AS table_name UNION ALL
    SELECT 'erp_cash_flow' AS table_name UNION ALL
    SELECT 'erp_consolidation_scope' AS table_name UNION ALL
    SELECT 'erp_consolidation_worksheet' AS table_name UNION ALL
    SELECT 'erp_cost_allocation' AS table_name UNION ALL
    SELECT 'erp_cost_center' AS table_name UNION ALL
    SELECT 'erp_currency' AS table_name UNION ALL
    SELECT 'erp_exchange_rate' AS table_name UNION ALL
    SELECT 'erp_finance_settlement_allocation' AS table_name UNION ALL
    SELECT 'erp_fund_plan' AS table_name UNION ALL
    SELECT 'erp_gl_account' AS table_name UNION ALL
    SELECT 'erp_gl_voucher' AS table_name UNION ALL
    SELECT 'erp_gl_voucher_entry' AS table_name UNION ALL
    SELECT 'erp_period' AS table_name UNION ALL
    SELECT 'erp_period_close' AS table_name UNION ALL
    SELECT 'erp_posting_rule' AS table_name UNION ALL
    SELECT 'erp_posting_rule_line' AS table_name UNION ALL
    SELECT 'erp_profit_center' AS table_name UNION ALL
    SELECT 'erp_profitability_analysis' AS table_name UNION ALL
    SELECT 'erp_sale_out' AS table_name UNION ALL
    SELECT 'erp_sale_out_items' AS table_name UNION ALL
    SELECT 'erp_tax_invoice' AS table_name UNION ALL
    SELECT 'erp_tax_invoice_line' AS table_name UNION ALL
    SELECT 'erp_tax_invoice_source' AS table_name UNION ALL
    SELECT 'erp_tax_rate' AS table_name UNION ALL
    SELECT 'hr_contract' AS table_name UNION ALL
    SELECT 'hr_interview' AS table_name UNION ALL
    SELECT 'hr_job_posting' AS table_name UNION ALL
    SELECT 'hr_leave_balance' AS table_name UNION ALL
    SELECT 'hr_leave_request' AS table_name UNION ALL
    SELECT 'hr_leave_type' AS table_name UNION ALL
    SELECT 'hr_resume' AS table_name UNION ALL
    SELECT 'hr_social_insurance_base' AS table_name UNION ALL
    SELECT 'mdm_cross_ref' AS table_name UNION ALL
    SELECT 'mes_energy_consumption' AS table_name UNION ALL
    SELECT 'oa_announcement' AS table_name UNION ALL
    SELECT 'oa_approval_template' AS table_name UNION ALL
    SELECT 'oa_document' AS table_name UNION ALL
    SELECT 'oa_document_attachment' AS table_name UNION ALL
    SELECT 'oa_knowledge_article' AS table_name UNION ALL
    SELECT 'oa_knowledge_category' AS table_name UNION ALL
    SELECT 'oa_knowledge_comment' AS table_name UNION ALL
    SELECT 'oa_knowledge_version' AS table_name UNION ALL
    SELECT 'oa_meeting_reservation' AS table_name UNION ALL
    SELECT 'oa_meeting_room' AS table_name UNION ALL
    SELECT 'oa_reimburse' AS table_name UNION ALL
    SELECT 'oa_reimburse_item' AS table_name UNION ALL
    SELECT 'oa_schedule' AS table_name UNION ALL
    SELECT 'qms_capa_document' AS table_name UNION ALL
    SELECT 'qms_electronic_signature_log' AS table_name UNION ALL
    SELECT 'qms_fmea_document' AS table_name UNION ALL
    SELECT 'qms_fmea_item' AS table_name UNION ALL
    SELECT 'qms_inspection_item' AS table_name UNION ALL
    SELECT 'qms_inspection_order' AS table_name UNION ALL
    SELECT 'qms_inspection_record' AS table_name UNION ALL
    SELECT 'qms_msa_measurement' AS table_name UNION ALL
    SELECT 'qms_msa_study' AS table_name UNION ALL
    SELECT 'qms_ncr_document' AS table_name UNION ALL
    SELECT 'qms_ncr_mrb_record' AS table_name UNION ALL
    SELECT 'system_dept' AS table_name UNION ALL
    SELECT 'system_oauth2_access_token' AS table_name UNION ALL
    SELECT 'system_oauth2_refresh_token' AS table_name UNION ALL
    SELECT 'system_role' AS table_name UNION ALL
    SELECT 'system_role_menu' AS table_name UNION ALL
    SELECT 'system_sms_code' AS table_name UNION ALL
    SELECT 'system_social_client' AS table_name UNION ALL
    SELECT 'system_users' AS table_name UNION ALL
    SELECT 'tms_fleet_operation' AS table_name UNION ALL
    SELECT 'tms_freight_reconciliation' AS table_name
)
,
actual_table AS (
    SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()
),
tenant_index AS (
    SELECT DISTINCT table_name
      FROM information_schema.statistics
     WHERE table_schema = DATABASE()
       AND seq_in_index = 1
       AND column_name = 'tenant_id'
)
SELECT 'V113' AS check_name,
       CASE
           WHEN (SELECT COUNT(*) FROM expected_table e JOIN actual_table a ON a.table_name = e.table_name) <> 75 THEN 'FAIL'
           WHEN (SELECT COUNT(*) FROM expected_table e JOIN tenant_index i ON i.table_name = e.table_name) <> 75 THEN 'FAIL'
           ELSE 'PASS'
       END AS verdict,
       75 AS expected_tables;

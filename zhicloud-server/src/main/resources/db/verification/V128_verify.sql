-- ============================================================
-- V128 人工复核脚本（只读）
--
-- 门禁：本迁移列出的 95 张业务表，其 id 必须已经是 auto_increment，
--       否则该表所在业务流程在真机 MySQL 上 INSERT 会失败
--       （id-type 对 MySQL 解析为 AUTO，插入不带 id）。
-- ============================================================

SELECT 'V128_PK_AUTO_INCREMENT_COVERAGE' AS check_name,
       CASE WHEN COUNT(*) = 95 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(*), ' / 95') AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND column_name = 'id'
   AND EXTRA LIKE '%auto_increment%'
   AND table_name IN (
       'aimultiagent_checkpoint',
       'aimultiagent_execution_log',
       'aimultiagent_execution_span',
       'aimultiagent_skill',
       'aimultiagent_skill_group',
       'aimultiagent_topology',
       'airag_eval_dataset',
       'airag_eval_question',
       'airag_eval_report',
       'erp_account',
       'erp_cost_allocation',
       'erp_cost_center',
       'erp_currency',
       'erp_customer',
       'erp_exchange_rate',
       'erp_finance_payment',
       'erp_finance_payment_item',
       'erp_finance_receipt',
       'erp_finance_receipt_item',
       'erp_product',
       'erp_product_category',
       'erp_product_unit',
       'erp_profit_center',
       'erp_profitability_analysis',
       'erp_purchase_in',
       'erp_purchase_in_items',
       'erp_purchase_order',
       'erp_purchase_order_items',
       'erp_purchase_return',
       'erp_purchase_return_items',
       'erp_sale_order',
       'erp_sale_order_items',
       'erp_sale_out',
       'erp_sale_out_items',
       'erp_sale_return',
       'erp_sale_return_items',
       'erp_stock',
       'erp_stock_batch',
       'erp_stock_check',
       'erp_stock_check_item',
       'erp_stock_in',
       'erp_stock_in_item',
       'erp_stock_move',
       'erp_stock_move_item',
       'erp_stock_out',
       'erp_stock_out_item',
       'erp_stock_record',
       'erp_stock_serial',
       'erp_supplier',
       'erp_warehouse',
       'hr_attendance',
       'hr_department',
       'hr_employee',
       'hr_performance',
       'hr_position',
       'hr_salary',
       'qms_capa_document',
       'qms_customer_complaint',
       'qms_eight_d_report',
       'qms_electronic_signature_log',
       'qms_fmea_document',
       'qms_fmea_item',
       'qms_inspection_item',
       'qms_inspection_order',
       'qms_inspection_record',
       'qms_msa_measurement',
       'qms_msa_study',
       'qms_ncr_document',
       'qms_ncr_mrb_record',
       'qms_qualification',
       'qms_quality_cost',
       'qms_scar',
       'qms_supplier_audit',
       'qms_supplier_rating',
       'qms_training_plan',
       'qms_training_record',
       'wms_check_order',
       'wms_check_order_detail',
       'wms_inventory',
       'wms_inventory_batch',
       'wms_inventory_history',
       'wms_item',
       'wms_item_brand',
       'wms_item_category',
       'wms_item_sku',
       'wms_merchant',
       'wms_movement_order',
       'wms_movement_order_detail',
       'wms_receipt_order',
       'wms_receipt_order_detail',
       'wms_shipment_order',
       'wms_shipment_order_detail',
       'wms_warehouse',
       'wms_wave_order',
       'wms_wave_order_detail'
   );

SELECT 'V128_RESIDUAL_BIGINT_PK_WITHOUT_AUTO' AS check_name,
       CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'CHECK' END AS result,
       GROUP_CONCAT(t.table_name ORDER BY t.table_name) AS actual
  FROM information_schema.tables t
  JOIN information_schema.columns c
    ON c.table_schema = t.table_schema AND c.table_name = t.table_name
 WHERE t.table_schema = DATABASE()
   AND t.table_type = 'BASE TABLE'
   AND c.column_name = 'id'
   AND LOWER(c.data_type) = 'bigint'
   AND c.column_key = 'PRI'
   AND c.EXTRA NOT LIKE '%auto_increment%';

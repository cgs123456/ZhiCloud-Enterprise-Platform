-- ============================================================================
-- 智云企业数字化中台 —— DEMO 模拟数据清理脚本（MySQL）
-- 文件: demo-data/cleanup/cleanup_demo.sql
-- 配套: generate_master_data.py + generate_business_documents.py
-- 状态: STATIC_ONLY —— 本脚本从未在任何数据库执行过，未验证可执行性
-- ============================================================================
--
-- 【用途】
--   清除两个生成器写入的全部模拟数据（默认 tenant_id=9001、creator='demo-seed'）。
--   覆盖 118 张表 = 99 张业务/支撑表（2,458 行）+ 19 张主数据表（267 行），
--   small 档合计 2,725 行。实际行数以两个 manifest 的 row_total 为准。
--
-- 【重要 - 执行前必读】
--   1. 严禁在任何生产/预发/共享库执行。脚本不带 DROP/TRUNCATE，但仍会 DELETE。
--   2. 第 0 节必须先单独执行并人工确认连接目标；确认无误后再执行第 1 节。
--   3. 所有 DELETE 都带双谓词 tenant_id + creator='demo-seed'，缺一不可。
--   4. 若生成时使用了 --tenant 或 --scale 非默认值，需把 9001 全量替换为实际租户号；
--      第 0 节的 expected_small 仅对应 small 档，实际行数以 manifest 为准。
--   5. 业务表之间只有逻辑外键，没有数据库级 FOREIGN KEY；删除仍严格按
--      业务/支撑表逆序 -> 主数据表逆序执行，避免清理过程中的父缺失。
--   6. 第 2 节不带 creator 谓词检查租户 9001 是否已完全清空。若出现非 0，说明
--      该租户下存在非本生成器写入的数据，必须人工核对，禁止直接扩大删除范围。
--
-- 【已知边界】
--   1. 仅在 MySQL 语法层静态生成，未连接、未执行任何数据库。
--   2. 只清理 tenant_id=9001 且 creator=demo-seed 的记录，不触碰其他租户/创建人。
--   3. 若第 2 节存在非 0，本脚本不会、也不应自动处理非 demo-seed 数据。
-- ============================================================================


-- ---------------------------------------------------------------------------
-- 第 0 节：预检（只读，务必先跑这一节并人工确认）
-- expected_small 来自 demo-data/schema/*_summary.json；合计 2,725 行。
-- ---------------------------------------------------------------------------
SELECT @@hostname AS host, DATABASE() AS db, @@port AS port, CURRENT_USER() AS user;

SELECT 'erp_account' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM erp_account WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_item_sku' AS tbl, COUNT(*) AS demo_rows, 40 AS expected_small
  FROM wms_item_sku WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_unit_measure' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM mes_md_unit_measure WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_item_type' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM mes_md_item_type WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_item' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM mes_md_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_client' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM mes_md_client WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_vendor' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM mes_md_vendor WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_process' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM mes_pro_process WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_workshop' AS tbl, COUNT(*) AS demo_rows, 2 AS expected_small
  FROM mes_md_workshop WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_workstation' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM mes_md_workstation WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_route' AS tbl, COUNT(*) AS demo_rows, 2 AS expected_small
  FROM mes_pro_route WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_route_process' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM mes_pro_route_process WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_bom' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM mes_md_bom WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_md_bom_detail' AS tbl, COUNT(*) AS demo_rows, 18 AS expected_small
  FROM mes_md_bom_detail WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_product_category' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM crm_product_category WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_product' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM crm_product WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_business_status_type' AS tbl, COUNT(*) AS demo_rows, 2 AS expected_small
  FROM crm_business_status_type WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_business_status' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM crm_business_status WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_inspection_item' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM qms_inspection_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_instrument' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM qms_instrument WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_leave_type' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM hr_leave_type WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'oa_meeting_room' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM oa_meeting_room WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_sale_order' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM erp_sale_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_sale_order_items' AS tbl, COUNT(*) AS demo_rows, 30 AS expected_small
  FROM erp_sale_order_items WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_sale_out' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM erp_sale_out WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_sale_out_items' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM erp_sale_out_items WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_sale_return' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM erp_sale_return WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_sale_return_items' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM erp_sale_return_items WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_purchase_order' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM erp_purchase_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_purchase_order_items' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM erp_purchase_order_items WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_purchase_in' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM erp_purchase_in WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_purchase_in_items' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM erp_purchase_in_items WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_purchase_return' AS tbl, COUNT(*) AS demo_rows, 2 AS expected_small
  FROM erp_purchase_return WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_purchase_return_items' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM erp_purchase_return_items WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_in' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM erp_stock_in WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_in_item' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM erp_stock_in_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_out' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM erp_stock_out WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_out_item' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM erp_stock_out_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_move' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM erp_stock_move WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_move_item' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM erp_stock_move_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock' AS tbl, COUNT(*) AS demo_rows, 96 AS expected_small
  FROM erp_stock WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_record' AS tbl, COUNT(*) AS demo_rows, 36 AS expected_small
  FROM erp_stock_record WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_check' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM erp_stock_check WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_stock_check_item' AS tbl, COUNT(*) AS demo_rows, 9 AS expected_small
  FROM erp_stock_check_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_finance_receipt' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM erp_finance_receipt WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_finance_receipt_item' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM erp_finance_receipt_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_finance_payment' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM erp_finance_payment WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_finance_payment_item' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM erp_finance_payment_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_work_order' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM mes_pro_work_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_work_order_bom' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM mes_pro_work_order_bom WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_task' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM mes_pro_task WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_task_issue' AS tbl, COUNT(*) AS demo_rows, 48 AS expected_small
  FROM mes_pro_task_issue WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_pro_feedback' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM mes_pro_feedback WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'mes_wm_material_stock' AS tbl, COUNT(*) AS demo_rows, 48 AS expected_small
  FROM mes_wm_material_stock WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_asn_order' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM wms_asn_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_asn_order_detail' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM wms_asn_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_receipt_order' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM wms_receipt_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_receipt_order_detail' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM wms_receipt_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_inventory' AS tbl, COUNT(*) AS demo_rows, 160 AS expected_small
  FROM wms_inventory WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_shipment_order' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM wms_shipment_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_shipment_order_detail' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM wms_shipment_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_wave_order' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM wms_wave_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_wave_order_detail' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM wms_wave_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_pick_task' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM wms_pick_task WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_movement_order' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM wms_movement_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_movement_order_detail' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM wms_movement_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_inspection_order' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM qms_inspection_order WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_inspection_record' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM qms_inspection_record WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_ncr_document' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM qms_ncr_document WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_capa_document' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM qms_capa_document WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_customer_complaint' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM qms_customer_complaint WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'qms_instrument_calibration' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM qms_instrument_calibration WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_clue' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM crm_clue WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_business' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM crm_business WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_business_product' AS tbl, COUNT(*) AS demo_rows, 20 AS expected_small
  FROM crm_business_product WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_contract' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM crm_contract WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_contract_product' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM crm_contract_product WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_receivable_plan' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM crm_receivable_plan WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_receivable' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM crm_receivable WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_contact' AS tbl, COUNT(*) AS demo_rows, 15 AS expected_small
  FROM crm_contact WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_follow_up_record' AS tbl, COUNT(*) AS demo_rows, 20 AS expected_small
  FROM crm_follow_up_record WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_invoice' AS tbl, COUNT(*) AS demo_rows, 5 AS expected_small
  FROM crm_invoice WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_invoice_line' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM crm_invoice_line WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_leave_request' AS tbl, COUNT(*) AS demo_rows, 20 AS expected_small
  FROM hr_leave_request WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'oa_announcement' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM oa_announcement WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'oa_reimburse' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM oa_reimburse WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'oa_reimburse_item' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM oa_reimburse_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'oa_meeting_reservation' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM oa_meeting_reservation WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'oa_schedule' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM oa_schedule WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_shipment' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM tms_shipment WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_shipment_stop' AS tbl, COUNT(*) AS demo_rows, 16 AS expected_small
  FROM tms_shipment_stop WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_tracking_event' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM tms_tracking_event WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_freight' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM tms_freight WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_freight_reconciliation' AS tbl, COUNT(*) AS demo_rows, 3 AS expected_small
  FROM tms_freight_reconciliation WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_product_category' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM erp_product_category WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_product_unit' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM erp_product_unit WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_product' AS tbl, COUNT(*) AS demo_rows, 24 AS expected_small
  FROM erp_product WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_customer' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM erp_customer WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_supplier' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM erp_supplier WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'erp_warehouse' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM erp_warehouse WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_warehouse' AS tbl, COUNT(*) AS demo_rows, 4 AS expected_small
  FROM wms_warehouse WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_zone' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM wms_zone WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_location' AS tbl, COUNT(*) AS demo_rows, 48 AS expected_small
  FROM wms_location WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_item_category' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM wms_item_category WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_item' AS tbl, COUNT(*) AS demo_rows, 20 AS expected_small
  FROM wms_item WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'wms_merchant' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM wms_merchant WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'crm_customer' AS tbl, COUNT(*) AS demo_rows, 15 AS expected_small
  FROM crm_customer WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_department' AS tbl, COUNT(*) AS demo_rows, 8 AS expected_small
  FROM hr_department WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_position' AS tbl, COUNT(*) AS demo_rows, 10 AS expected_small
  FROM hr_position WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'hr_employee' AS tbl, COUNT(*) AS demo_rows, 40 AS expected_small
  FROM hr_employee WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_carrier' AS tbl, COUNT(*) AS demo_rows, 6 AS expected_small
  FROM tms_carrier WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_driver' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM tms_driver WHERE tenant_id = 9001 AND creator = 'demo-seed'
UNION ALL SELECT 'tms_vehicle' AS tbl, COUNT(*) AS demo_rows, 12 AS expected_small
  FROM tms_vehicle WHERE tenant_id = 9001 AND creator = 'demo-seed'
;


-- ---------------------------------------------------------------------------
-- 第 1 节：清理（确认第 0 节结果无误后再执行）
-- 顺序：99 张业务/支撑表逆序 -> 19 张主数据表逆序
-- ---------------------------------------------------------------------------
BEGIN;

DELETE FROM tms_freight_reconciliation WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_freight WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_tracking_event WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_shipment_stop WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_shipment WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM oa_schedule WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM oa_meeting_reservation WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM oa_reimburse_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM oa_reimburse WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM oa_announcement WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM hr_leave_request WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_invoice_line WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_invoice WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_follow_up_record WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_contact WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_receivable WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_receivable_plan WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_contract_product WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_contract WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_business_product WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_business WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_clue WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_instrument_calibration WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_customer_complaint WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_capa_document WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_ncr_document WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_inspection_record WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_inspection_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_movement_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_movement_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_pick_task WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_wave_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_wave_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_shipment_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_shipment_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_inventory WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_receipt_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_receipt_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_asn_order_detail WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_asn_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_wm_material_stock WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_feedback WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_task_issue WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_task WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_work_order_bom WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_work_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_finance_payment_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_finance_payment WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_finance_receipt_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_finance_receipt WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_check_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_check WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_record WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_move_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_move WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_out_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_out WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_in_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_stock_in WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_purchase_return_items WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_purchase_return WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_purchase_in_items WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_purchase_in WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_purchase_order_items WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_purchase_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_sale_return_items WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_sale_return WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_sale_out_items WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_sale_out WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_sale_order_items WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_sale_order WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM oa_meeting_room WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM hr_leave_type WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_instrument WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM qms_inspection_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_business_status WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_business_status_type WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_product WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_product_category WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_bom_detail WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_bom WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_route_process WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_route WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_workstation WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_workshop WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_pro_process WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_vendor WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_client WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_item_type WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM mes_md_unit_measure WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_item_sku WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_account WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_vehicle WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_driver WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM tms_carrier WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM hr_employee WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM hr_position WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM hr_department WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM crm_customer WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_merchant WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_item WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_item_category WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_location WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_zone WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM wms_warehouse WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_warehouse WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_supplier WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_customer WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_product WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_product_unit WHERE tenant_id = 9001 AND creator = 'demo-seed';
DELETE FROM erp_product_category WHERE tenant_id = 9001 AND creator = 'demo-seed';

COMMIT;


-- ---------------------------------------------------------------------------
-- 第 2 节：残留检查（租户 9001 下 118 张表应全部为 0）
-- 任何一行非 0 => 该租户下存在非 demo-seed 数据，需人工核对后再决定是否处理。
-- ---------------------------------------------------------------------------
SELECT 'erp_account' AS tbl, COUNT(*) AS remain FROM erp_account WHERE tenant_id = 9001
UNION ALL SELECT 'wms_item_sku' AS tbl, COUNT(*) AS remain FROM wms_item_sku WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_unit_measure' AS tbl, COUNT(*) AS remain FROM mes_md_unit_measure WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_item_type' AS tbl, COUNT(*) AS remain FROM mes_md_item_type WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_item' AS tbl, COUNT(*) AS remain FROM mes_md_item WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_client' AS tbl, COUNT(*) AS remain FROM mes_md_client WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_vendor' AS tbl, COUNT(*) AS remain FROM mes_md_vendor WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_process' AS tbl, COUNT(*) AS remain FROM mes_pro_process WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_workshop' AS tbl, COUNT(*) AS remain FROM mes_md_workshop WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_workstation' AS tbl, COUNT(*) AS remain FROM mes_md_workstation WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_route' AS tbl, COUNT(*) AS remain FROM mes_pro_route WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_route_process' AS tbl, COUNT(*) AS remain FROM mes_pro_route_process WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_bom' AS tbl, COUNT(*) AS remain FROM mes_md_bom WHERE tenant_id = 9001
UNION ALL SELECT 'mes_md_bom_detail' AS tbl, COUNT(*) AS remain FROM mes_md_bom_detail WHERE tenant_id = 9001
UNION ALL SELECT 'crm_product_category' AS tbl, COUNT(*) AS remain FROM crm_product_category WHERE tenant_id = 9001
UNION ALL SELECT 'crm_product' AS tbl, COUNT(*) AS remain FROM crm_product WHERE tenant_id = 9001
UNION ALL SELECT 'crm_business_status_type' AS tbl, COUNT(*) AS remain FROM crm_business_status_type WHERE tenant_id = 9001
UNION ALL SELECT 'crm_business_status' AS tbl, COUNT(*) AS remain FROM crm_business_status WHERE tenant_id = 9001
UNION ALL SELECT 'qms_inspection_item' AS tbl, COUNT(*) AS remain FROM qms_inspection_item WHERE tenant_id = 9001
UNION ALL SELECT 'qms_instrument' AS tbl, COUNT(*) AS remain FROM qms_instrument WHERE tenant_id = 9001
UNION ALL SELECT 'hr_leave_type' AS tbl, COUNT(*) AS remain FROM hr_leave_type WHERE tenant_id = 9001
UNION ALL SELECT 'oa_meeting_room' AS tbl, COUNT(*) AS remain FROM oa_meeting_room WHERE tenant_id = 9001
UNION ALL SELECT 'erp_sale_order' AS tbl, COUNT(*) AS remain FROM erp_sale_order WHERE tenant_id = 9001
UNION ALL SELECT 'erp_sale_order_items' AS tbl, COUNT(*) AS remain FROM erp_sale_order_items WHERE tenant_id = 9001
UNION ALL SELECT 'erp_sale_out' AS tbl, COUNT(*) AS remain FROM erp_sale_out WHERE tenant_id = 9001
UNION ALL SELECT 'erp_sale_out_items' AS tbl, COUNT(*) AS remain FROM erp_sale_out_items WHERE tenant_id = 9001
UNION ALL SELECT 'erp_sale_return' AS tbl, COUNT(*) AS remain FROM erp_sale_return WHERE tenant_id = 9001
UNION ALL SELECT 'erp_sale_return_items' AS tbl, COUNT(*) AS remain FROM erp_sale_return_items WHERE tenant_id = 9001
UNION ALL SELECT 'erp_purchase_order' AS tbl, COUNT(*) AS remain FROM erp_purchase_order WHERE tenant_id = 9001
UNION ALL SELECT 'erp_purchase_order_items' AS tbl, COUNT(*) AS remain FROM erp_purchase_order_items WHERE tenant_id = 9001
UNION ALL SELECT 'erp_purchase_in' AS tbl, COUNT(*) AS remain FROM erp_purchase_in WHERE tenant_id = 9001
UNION ALL SELECT 'erp_purchase_in_items' AS tbl, COUNT(*) AS remain FROM erp_purchase_in_items WHERE tenant_id = 9001
UNION ALL SELECT 'erp_purchase_return' AS tbl, COUNT(*) AS remain FROM erp_purchase_return WHERE tenant_id = 9001
UNION ALL SELECT 'erp_purchase_return_items' AS tbl, COUNT(*) AS remain FROM erp_purchase_return_items WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_in' AS tbl, COUNT(*) AS remain FROM erp_stock_in WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_in_item' AS tbl, COUNT(*) AS remain FROM erp_stock_in_item WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_out' AS tbl, COUNT(*) AS remain FROM erp_stock_out WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_out_item' AS tbl, COUNT(*) AS remain FROM erp_stock_out_item WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_move' AS tbl, COUNT(*) AS remain FROM erp_stock_move WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_move_item' AS tbl, COUNT(*) AS remain FROM erp_stock_move_item WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock' AS tbl, COUNT(*) AS remain FROM erp_stock WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_record' AS tbl, COUNT(*) AS remain FROM erp_stock_record WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_check' AS tbl, COUNT(*) AS remain FROM erp_stock_check WHERE tenant_id = 9001
UNION ALL SELECT 'erp_stock_check_item' AS tbl, COUNT(*) AS remain FROM erp_stock_check_item WHERE tenant_id = 9001
UNION ALL SELECT 'erp_finance_receipt' AS tbl, COUNT(*) AS remain FROM erp_finance_receipt WHERE tenant_id = 9001
UNION ALL SELECT 'erp_finance_receipt_item' AS tbl, COUNT(*) AS remain FROM erp_finance_receipt_item WHERE tenant_id = 9001
UNION ALL SELECT 'erp_finance_payment' AS tbl, COUNT(*) AS remain FROM erp_finance_payment WHERE tenant_id = 9001
UNION ALL SELECT 'erp_finance_payment_item' AS tbl, COUNT(*) AS remain FROM erp_finance_payment_item WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_work_order' AS tbl, COUNT(*) AS remain FROM mes_pro_work_order WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_work_order_bom' AS tbl, COUNT(*) AS remain FROM mes_pro_work_order_bom WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_task' AS tbl, COUNT(*) AS remain FROM mes_pro_task WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_task_issue' AS tbl, COUNT(*) AS remain FROM mes_pro_task_issue WHERE tenant_id = 9001
UNION ALL SELECT 'mes_pro_feedback' AS tbl, COUNT(*) AS remain FROM mes_pro_feedback WHERE tenant_id = 9001
UNION ALL SELECT 'mes_wm_material_stock' AS tbl, COUNT(*) AS remain FROM mes_wm_material_stock WHERE tenant_id = 9001
UNION ALL SELECT 'wms_asn_order' AS tbl, COUNT(*) AS remain FROM wms_asn_order WHERE tenant_id = 9001
UNION ALL SELECT 'wms_asn_order_detail' AS tbl, COUNT(*) AS remain FROM wms_asn_order_detail WHERE tenant_id = 9001
UNION ALL SELECT 'wms_receipt_order' AS tbl, COUNT(*) AS remain FROM wms_receipt_order WHERE tenant_id = 9001
UNION ALL SELECT 'wms_receipt_order_detail' AS tbl, COUNT(*) AS remain FROM wms_receipt_order_detail WHERE tenant_id = 9001
UNION ALL SELECT 'wms_inventory' AS tbl, COUNT(*) AS remain FROM wms_inventory WHERE tenant_id = 9001
UNION ALL SELECT 'wms_shipment_order' AS tbl, COUNT(*) AS remain FROM wms_shipment_order WHERE tenant_id = 9001
UNION ALL SELECT 'wms_shipment_order_detail' AS tbl, COUNT(*) AS remain FROM wms_shipment_order_detail WHERE tenant_id = 9001
UNION ALL SELECT 'wms_wave_order' AS tbl, COUNT(*) AS remain FROM wms_wave_order WHERE tenant_id = 9001
UNION ALL SELECT 'wms_wave_order_detail' AS tbl, COUNT(*) AS remain FROM wms_wave_order_detail WHERE tenant_id = 9001
UNION ALL SELECT 'wms_pick_task' AS tbl, COUNT(*) AS remain FROM wms_pick_task WHERE tenant_id = 9001
UNION ALL SELECT 'wms_movement_order' AS tbl, COUNT(*) AS remain FROM wms_movement_order WHERE tenant_id = 9001
UNION ALL SELECT 'wms_movement_order_detail' AS tbl, COUNT(*) AS remain FROM wms_movement_order_detail WHERE tenant_id = 9001
UNION ALL SELECT 'qms_inspection_order' AS tbl, COUNT(*) AS remain FROM qms_inspection_order WHERE tenant_id = 9001
UNION ALL SELECT 'qms_inspection_record' AS tbl, COUNT(*) AS remain FROM qms_inspection_record WHERE tenant_id = 9001
UNION ALL SELECT 'qms_ncr_document' AS tbl, COUNT(*) AS remain FROM qms_ncr_document WHERE tenant_id = 9001
UNION ALL SELECT 'qms_capa_document' AS tbl, COUNT(*) AS remain FROM qms_capa_document WHERE tenant_id = 9001
UNION ALL SELECT 'qms_customer_complaint' AS tbl, COUNT(*) AS remain FROM qms_customer_complaint WHERE tenant_id = 9001
UNION ALL SELECT 'qms_instrument_calibration' AS tbl, COUNT(*) AS remain FROM qms_instrument_calibration WHERE tenant_id = 9001
UNION ALL SELECT 'crm_clue' AS tbl, COUNT(*) AS remain FROM crm_clue WHERE tenant_id = 9001
UNION ALL SELECT 'crm_business' AS tbl, COUNT(*) AS remain FROM crm_business WHERE tenant_id = 9001
UNION ALL SELECT 'crm_business_product' AS tbl, COUNT(*) AS remain FROM crm_business_product WHERE tenant_id = 9001
UNION ALL SELECT 'crm_contract' AS tbl, COUNT(*) AS remain FROM crm_contract WHERE tenant_id = 9001
UNION ALL SELECT 'crm_contract_product' AS tbl, COUNT(*) AS remain FROM crm_contract_product WHERE tenant_id = 9001
UNION ALL SELECT 'crm_receivable_plan' AS tbl, COUNT(*) AS remain FROM crm_receivable_plan WHERE tenant_id = 9001
UNION ALL SELECT 'crm_receivable' AS tbl, COUNT(*) AS remain FROM crm_receivable WHERE tenant_id = 9001
UNION ALL SELECT 'crm_contact' AS tbl, COUNT(*) AS remain FROM crm_contact WHERE tenant_id = 9001
UNION ALL SELECT 'crm_follow_up_record' AS tbl, COUNT(*) AS remain FROM crm_follow_up_record WHERE tenant_id = 9001
UNION ALL SELECT 'crm_invoice' AS tbl, COUNT(*) AS remain FROM crm_invoice WHERE tenant_id = 9001
UNION ALL SELECT 'crm_invoice_line' AS tbl, COUNT(*) AS remain FROM crm_invoice_line WHERE tenant_id = 9001
UNION ALL SELECT 'hr_leave_request' AS tbl, COUNT(*) AS remain FROM hr_leave_request WHERE tenant_id = 9001
UNION ALL SELECT 'oa_announcement' AS tbl, COUNT(*) AS remain FROM oa_announcement WHERE tenant_id = 9001
UNION ALL SELECT 'oa_reimburse' AS tbl, COUNT(*) AS remain FROM oa_reimburse WHERE tenant_id = 9001
UNION ALL SELECT 'oa_reimburse_item' AS tbl, COUNT(*) AS remain FROM oa_reimburse_item WHERE tenant_id = 9001
UNION ALL SELECT 'oa_meeting_reservation' AS tbl, COUNT(*) AS remain FROM oa_meeting_reservation WHERE tenant_id = 9001
UNION ALL SELECT 'oa_schedule' AS tbl, COUNT(*) AS remain FROM oa_schedule WHERE tenant_id = 9001
UNION ALL SELECT 'tms_shipment' AS tbl, COUNT(*) AS remain FROM tms_shipment WHERE tenant_id = 9001
UNION ALL SELECT 'tms_shipment_stop' AS tbl, COUNT(*) AS remain FROM tms_shipment_stop WHERE tenant_id = 9001
UNION ALL SELECT 'tms_tracking_event' AS tbl, COUNT(*) AS remain FROM tms_tracking_event WHERE tenant_id = 9001
UNION ALL SELECT 'tms_freight' AS tbl, COUNT(*) AS remain FROM tms_freight WHERE tenant_id = 9001
UNION ALL SELECT 'tms_freight_reconciliation' AS tbl, COUNT(*) AS remain FROM tms_freight_reconciliation WHERE tenant_id = 9001
UNION ALL SELECT 'erp_product_category' AS tbl, COUNT(*) AS remain FROM erp_product_category WHERE tenant_id = 9001
UNION ALL SELECT 'erp_product_unit' AS tbl, COUNT(*) AS remain FROM erp_product_unit WHERE tenant_id = 9001
UNION ALL SELECT 'erp_product' AS tbl, COUNT(*) AS remain FROM erp_product WHERE tenant_id = 9001
UNION ALL SELECT 'erp_customer' AS tbl, COUNT(*) AS remain FROM erp_customer WHERE tenant_id = 9001
UNION ALL SELECT 'erp_supplier' AS tbl, COUNT(*) AS remain FROM erp_supplier WHERE tenant_id = 9001
UNION ALL SELECT 'erp_warehouse' AS tbl, COUNT(*) AS remain FROM erp_warehouse WHERE tenant_id = 9001
UNION ALL SELECT 'wms_warehouse' AS tbl, COUNT(*) AS remain FROM wms_warehouse WHERE tenant_id = 9001
UNION ALL SELECT 'wms_zone' AS tbl, COUNT(*) AS remain FROM wms_zone WHERE tenant_id = 9001
UNION ALL SELECT 'wms_location' AS tbl, COUNT(*) AS remain FROM wms_location WHERE tenant_id = 9001
UNION ALL SELECT 'wms_item_category' AS tbl, COUNT(*) AS remain FROM wms_item_category WHERE tenant_id = 9001
UNION ALL SELECT 'wms_item' AS tbl, COUNT(*) AS remain FROM wms_item WHERE tenant_id = 9001
UNION ALL SELECT 'wms_merchant' AS tbl, COUNT(*) AS remain FROM wms_merchant WHERE tenant_id = 9001
UNION ALL SELECT 'crm_customer' AS tbl, COUNT(*) AS remain FROM crm_customer WHERE tenant_id = 9001
UNION ALL SELECT 'hr_department' AS tbl, COUNT(*) AS remain FROM hr_department WHERE tenant_id = 9001
UNION ALL SELECT 'hr_position' AS tbl, COUNT(*) AS remain FROM hr_position WHERE tenant_id = 9001
UNION ALL SELECT 'hr_employee' AS tbl, COUNT(*) AS remain FROM hr_employee WHERE tenant_id = 9001
UNION ALL SELECT 'tms_carrier' AS tbl, COUNT(*) AS remain FROM tms_carrier WHERE tenant_id = 9001
UNION ALL SELECT 'tms_driver' AS tbl, COUNT(*) AS remain FROM tms_driver WHERE tenant_id = 9001
UNION ALL SELECT 'tms_vehicle' AS tbl, COUNT(*) AS remain FROM tms_vehicle WHERE tenant_id = 9001
;

-- ============================================================================
-- 结束
-- ============================================================================

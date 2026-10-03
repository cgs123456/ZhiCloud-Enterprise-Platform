-- ============================================================
-- V124 人工复核脚本（只读）
--
-- 门禁：TMS 运单必须能挂 WMS 出库单并记录签收时刻，
--       WMS 出库单必须能反查承运运单。
-- ============================================================

SELECT 'V124_TMS_SHIPMENT_LINK_COLUMNS' AS check_name,
       CASE WHEN COUNT(*) = 2 THEN 'PASS' ELSE 'FAIL' END AS result,
       GROUP_CONCAT(column_name ORDER BY column_name) AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'tms_shipment'
   AND column_name IN ('shipment_order_id', 'sign_time');

SELECT 'V124_WMS_SHIPMENT_WAYBILL_COLUMN' AS check_name,
       CASE WHEN COUNT(*) = 1 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(COUNT(*), ' / 1') AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'wms_shipment_order'
   AND column_name = 'waybill_id';

SELECT 'V124_LINK_INDEXES' AS check_name,
       CASE WHEN COUNT(*) = 2 THEN 'PASS' ELSE 'FAIL' END AS result,
       GROUP_CONCAT(DISTINCT index_name ORDER BY index_name) AS actual
  FROM information_schema.statistics
 WHERE table_schema = DATABASE()
   AND index_name IN ('idx_tms_shipment_order_id', 'idx_wms_shipment_order_waybill_id');

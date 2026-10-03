-- ============================================================
-- V126 人工复核脚本（只读）
--
-- 门禁：三张业务查询视图必须存在且可查询（GoView 裸 SQL 入口只应指向这些视图）。
-- ============================================================

SELECT 'V126_BUSINESS_VIEWS_PRESENT' AS check_name,
       CASE WHEN COUNT(*) = 3 THEN 'PASS' ELSE 'FAIL' END AS result,
       GROUP_CONCAT(table_name ORDER BY table_name) AS actual
  FROM information_schema.views
 WHERE table_schema = DATABASE()
   AND table_name IN ('v_wms_shipment_waybill_sign', 'v_qms_inspection_status_daily', 'v_bpm_approval_inflight');

SELECT 'V126_VIEW_QUERYABLE' AS check_name,
       CASE WHEN a.c + b.c + c.c >= 0 THEN 'PASS' ELSE 'FAIL' END AS result,
       CONCAT(a.c, '/', b.c, '/', c.c) AS actual
  FROM (SELECT COUNT(*) AS c FROM v_wms_shipment_waybill_sign) a,
       (SELECT COUNT(*) AS c FROM v_qms_inspection_status_daily) b,
       (SELECT COUNT(*) AS c FROM v_bpm_approval_inflight) c;

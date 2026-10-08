-- V148 验证：MES 物料×仓库双维度对账视图的行为断言
-- 前置：mdm_cross_ref 有 MES.ITEM 1→WMS.SKU 1 与 MES.WAREHOUSE 1→WMS.WAREHOUSE 1
--       两条 VALID 映射（R-014 维护 API 登记），MES 物料账 item_id=1 wh=1 qty=3，WMS sku=1 wh=1 qty=8。
-- 1. Flyway 通道
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '148' AND success = 1) = 1
            THEN 'V148_FLYWAY_APPLIED PASS'
            ELSE 'V148_FLYWAY_APPLIED FAIL' END AS check_result;
-- 2. 双桥视图出仓维度行：item 1 + mes_wh 1 → wms_sku 1 + wms_wh 1，diff=-5 QUANTITY_DIFF
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_mes_wms_warehouse`
                   WHERE mes_item_id = 1 AND mes_warehouse_id = 1
                     AND wms_sku_id = 1 AND wms_warehouse_id = 1
                     AND quantity_diff = -5.0000
                     AND reconcile_status = 'QUANTITY_DIFF') = 1
            THEN 'V148_WAREHOUSE_ROW_RECONCILED PASS'
            ELSE 'V148_WAREHOUSE_ROW_RECONCILED FAIL' END AS check_result;

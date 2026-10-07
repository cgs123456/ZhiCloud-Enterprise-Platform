-- V145 验证：MES↔WMS 对账视图的行为断言
-- 前置：mdm_cross_ref 有 (MES, ITEM, 1) -> (WMS, SKU, 1) VALID 映射（经 R-014 API 登记），
--       mes_wm_material_stock 有 item_id=1 quantity=3.0000（SQL 造数），
--       wms_inventory 有 sku_id=1 quantity=8.0000。

-- 1. Flyway 通道记录
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '145' AND success = 1) = 1
            THEN 'V145_FLYWAY_APPLIED PASS'
            ELSE 'V145_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. MES 行进入对账视图：mes_item_id=1 对到 wms_sku_id=1，
--    mes_quantity=3.0000 vs wms_quantity=8.0000 → diff=-5.0000 QUANTITY_DIFF
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_mes_wms`
                   WHERE mes_item_id = 1 AND wms_sku_id = 1
                     AND mes_quantity = 3.0000 AND wms_quantity = 8.0000
                     AND quantity_diff = -5.0000 AND reconcile_status = 'QUANTITY_DIFF') = 1
            THEN 'V145_MES_ROW_RECONCILED PASS'
            ELSE 'V145_MES_ROW_RECONCILED FAIL MES 对账行缺失或读数不符' END AS check_result;

-- 3. 状态枚举受控
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_reconcile_mes_wms`
                   WHERE reconcile_status NOT IN ('MATCHED','QUANTITY_DIFF','WMS_LEDGER_MISSING')
                      OR reconcile_status IS NULL) = 0
            THEN 'V145_STATUS_ENUM PASS'
            ELSE 'V145_STATUS_ENUM FAIL' END AS check_result;

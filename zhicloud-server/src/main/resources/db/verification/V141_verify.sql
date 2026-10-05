-- V141 验证：三本账总览视图的行为断言（不是只看定义文本）
-- 前置：④ 探测已在 wms_inventory 落了 SKU-PROBE-001（仓库 WH-PROBE-001）6.0 的库存行。

-- 1. Flyway 通道记录：V141 成功应用
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '141' AND success = 1) = 1
            THEN 'V141_FLYWAY_APPLIED PASS'
            ELSE 'V141_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. 视图可查询且来源枚举只有三本账
SELECT CASE WHEN (SELECT COUNT(DISTINCT source) FROM `v_inventory_ledgers_overview`
                   WHERE source IN ('ERP', 'WMS', 'MES')) =
                 (SELECT COUNT(DISTINCT source) FROM `v_inventory_ledgers_overview`)
            THEN 'V141_SOURCES_NORMALIZED PASS'
            ELSE 'V141_SOURCES_NORMALIZED FAIL 出现未知来源' END AS check_result;

-- 3. 行为断言：WMS 侧探测数据可经视图读出（quantity=6.0）
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_ledgers_overview` o
                   JOIN wms_inventory w ON w.id = o.item_key
                   WHERE o.source = 'WMS' AND o.item_key = w.sku_id
                     AND o.ledger_table = 'wms_inventory'
                     AND o.quantity = 6.0 AND o.tenant_id = 1) >= 1
            THEN 'V141_WMS_ROW_VISIBLE PASS'
            ELSE 'V141_WMS_ROW_VISIBLE FAIL 探测库存未出现在总览里' END AS check_result;

-- 4. 空账本如实可见：全新库上 ERP/MES 暂无库存行（当前应为 0；有数据后此断言改数）
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_ledgers_overview`
                   WHERE source IN ('ERP', 'MES')) = 0
            THEN 'V141_ERP_MES_EMPTY_AS_EXPECTED PASS'
            ELSE 'V141_ERP_MES_EMPTY_AS_EXPECTED ADVISE 出现 ERP/MES 库存行，核对口径' END AS check_result;

-- 5. 租户透传：视图行的 tenant_id 都非空
SELECT CASE WHEN (SELECT COUNT(*) FROM `v_inventory_ledgers_overview`
                   WHERE tenant_id IS NULL) = 0
            THEN 'V141_TENANT_PASSTHROUGH PASS'
            ELSE 'V141_TENANT_PASSTHROUGH FAIL' END AS check_result;

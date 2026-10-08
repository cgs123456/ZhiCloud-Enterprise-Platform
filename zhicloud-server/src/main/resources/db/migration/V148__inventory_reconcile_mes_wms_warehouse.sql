-- ============================================================
-- V148：R-007 仓库维度收口——MES↔WMS 按「同一物料 + 同一逻辑仓」行级对账
-- ============================================================
-- V145 只按 (tenant, item→sku) 对物料账，仓库维度未对齐（MES 的 warehouse_id 与
-- WMS 的 warehouse_id 分属两套 id 空间）。本视图加一条 MES.WAREHOUSE→WMS.WAREHOUSE
-- 映射，按「item + warehouse 双桥」出行——至此 R-007 的 ERP↔WMS（V143，含仓库）
-- 与 MES↔WMS（V145 物料 + V148 物料×仓库）都到行级，三本账差额可逐仓核对。
-- 口径：只对有「物料映射 + 仓库映射」的 (item, mes_wh) 出行；未建映射的仓
-- 由维护入口（R-014）录入，不在本视图伪装。两侧各自先按 (tenant,item,warehouse)
-- 聚合（MES 可能有批次行）。映射键显式 CAST SIGNED（同 V143/V145 教训，避免隐式转换）。
-- ============================================================

CREATE OR REPLACE VIEW `v_inventory_reconcile_mes_wms_warehouse` AS
SELECT
    agg.tenant_id                                        AS tenant_id,
    agg.item_id                                          AS mes_item_id,
    CAST(imap.target_id AS SIGNED)                       AS wms_sku_id,
    agg.warehouse_id                                     AS mes_warehouse_id,
    CAST(wmap.target_id AS SIGNED)                       AS wms_warehouse_id,
    CAST(agg.mes_qty AS DECIMAL(20, 4))                  AS mes_quantity,
    CAST(IFNULL(wms.quantity, 0) AS DECIMAL(20, 4))      AS wms_quantity,
    CAST(agg.mes_qty - IFNULL(wms.quantity, 0) AS DECIMAL(20, 4)) AS quantity_diff,
    CASE
        WHEN wmap.id IS NULL THEN 'WMS_WAREHOUSE_MAPPING_MISSING'
        WHEN wms.id IS NULL THEN 'WMS_LEDGER_MISSING'
        WHEN agg.mes_qty <> IFNULL(wms.quantity, 0) THEN 'QUANTITY_DIFF'
        ELSE 'MATCHED'
    END                                                  AS reconcile_status
  FROM (SELECT tenant_id, item_id, warehouse_id, SUM(quantity) AS mes_qty
          FROM `mes_wm_material_stock` WHERE deleted = b'0'
         GROUP BY tenant_id, item_id, warehouse_id) agg
  JOIN `mdm_cross_ref` imap
    ON  imap.tenant_id = agg.tenant_id
    AND imap.source_system = 'MES' AND imap.source_entity = 'ITEM'
    AND imap.target_system = 'WMS' AND imap.target_entity = 'SKU'
    AND imap.status = 'VALID' AND imap.deleted = b'0'
    AND CAST(imap.source_id AS SIGNED) = agg.item_id
  LEFT JOIN `mdm_cross_ref` wmap
    ON  wmap.tenant_id = agg.tenant_id
    AND wmap.source_system = 'MES' AND wmap.source_entity = 'WAREHOUSE'
    AND wmap.target_system = 'WMS' AND wmap.target_entity = 'WAREHOUSE'
    AND wmap.status = 'VALID' AND wmap.deleted = b'0'
    AND CAST(wmap.source_id AS SIGNED) = agg.warehouse_id
  LEFT JOIN `wms_inventory` wms
    ON  wms.tenant_id = agg.tenant_id
    AND wms.sku_id = CAST(imap.target_id AS SIGNED)
    AND wms.deleted = b'0'
    AND wmap.id IS NOT NULL
    AND wms.warehouse_id = CAST(wmap.target_id AS SIGNED);

-- 自检 1：视图存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V148_VIEW_EXISTS PASS'
            ELSE 'V148_VIEW_EXISTS FAIL' END AS check_result
  FROM information_schema.views
 WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_mes_wms_warehouse';

-- 自检 2：可查询
SELECT CASE WHEN COUNT(*) >= 0 THEN 'V148_SELECTABLE PASS'
            ELSE 'V148_SELECTABLE FAIL' END AS check_result
  FROM `v_inventory_reconcile_mes_wms_warehouse`;

-- 自检 3：状态枚举受控（四态之一，不出现空值）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V148_STATUS_ENUM PASS'
            ELSE CONCAT('V148_STATUS_ENUM FAIL ', COUNT(*)) END AS check_result
  FROM `v_inventory_reconcile_mes_wms_warehouse`
 WHERE reconcile_status NOT IN ('MATCHED','QUANTITY_DIFF','WMS_LEDGER_MISSING','WMS_WAREHOUSE_MAPPING_MISSING')
    OR reconcile_status IS NULL;

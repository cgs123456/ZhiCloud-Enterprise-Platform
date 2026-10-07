-- ============================================================
-- V145：R-007 第三步——经 MDM 映射的 MES↔WMS 物料账对账视图
-- ============================================================
-- V143 交付了 ERP↔WMS 行级对账。MES 物料账（mes_wm_material_stock，item_id 维度）
-- 此前未纳入：缺 MES→WMS 的映射消费。R-014 补上维护 API 后，本视图按
-- mdm_cross_ref(MES, ITEM -> WMS, SKU, VALID) 把 MES 物料账与 WMS SKU 账对到一行。
--
-- 口径与边界：
--   1. 两侧各自按 (tenant_id, item/sku) 聚合——MES 的 warehouse_id 与 WMS 的
--      warehouse_id 分属两套 id，仓库维度的对齐待 MES↔WMS 仓库映射建立后另做。
--   2. 只对已建立映射的物料出行；未映射的 MES 物料是 R-014 维护 API 的录入职责，
--      不在本视图内伪装。
--   3. MES 账的行是探测种子（SQL 造数，如实记录于 handoff §12.143）。
-- ============================================================

CREATE OR REPLACE VIEW `v_inventory_reconcile_mes_wms` AS
SELECT
    mes.tenant_id                                        AS tenant_id,
    CAST(mref.source_id AS SIGNED)                       AS mes_item_id,
    CAST(mref.target_id AS SIGNED)                       AS wms_sku_id,
    CAST(IFNULL(mes.mes_qty, 0) AS DECIMAL(20, 4))       AS mes_quantity,
    CAST(IFNULL(wms.wms_qty, 0) AS DECIMAL(20, 4))       AS wms_quantity,
    CAST(IFNULL(mes.mes_qty, 0) - IFNULL(wms.wms_qty, 0) AS DECIMAL(20, 4)) AS quantity_diff,
    CASE
        WHEN wms.wms_qty IS NULL THEN 'WMS_LEDGER_MISSING'
        WHEN IFNULL(mes.mes_qty, 0) <> IFNULL(wms.wms_qty, 0) THEN 'QUANTITY_DIFF'
        ELSE 'MATCHED'
    END                                                  AS reconcile_status
  FROM (SELECT tenant_id, item_id, SUM(quantity) AS mes_qty
          FROM `mes_wm_material_stock` WHERE deleted = b'0'
         GROUP BY tenant_id, item_id) mes
  JOIN `mdm_cross_ref` mref
    ON  mref.tenant_id = mes.tenant_id
    AND mref.source_system = 'MES' AND mref.source_entity = 'ITEM'
    AND mref.target_system = 'WMS' AND mref.target_entity = 'SKU'
    AND mref.status = 'VALID' AND mref.deleted = b'0'
    AND CAST(mref.source_id AS SIGNED) = mes.item_id
  LEFT JOIN (SELECT tenant_id, sku_id, SUM(quantity) AS wms_qty
               FROM `wms_inventory` WHERE deleted = b'0'
              GROUP BY tenant_id, sku_id) wms
    ON  wms.tenant_id = mref.tenant_id
    AND wms.sku_id = CAST(mref.target_id AS SIGNED);

-- 自检 1：视图存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V145_VIEW_EXISTS PASS'
            ELSE 'V145_VIEW_EXISTS FAIL' END AS check_result
  FROM information_schema.views
 WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_mes_wms';

-- 自检 2：三张关键表都在定义里
SELECT CASE WHEN (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_mes_wms')
                  LIKE '%mes_wm_material_stock%'
             AND (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_mes_wms')
                  LIKE '%mdm_cross_ref%'
             AND (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_mes_wms')
                  LIKE '%wms_inventory%'
            THEN 'V145_TABLES_WIRED PASS'
            ELSE 'V145_TABLES_WIRED FAIL' END AS check_result;

-- 自检 3：可查询
SELECT CASE WHEN COUNT(*) >= 0 THEN 'V145_SELECTABLE PASS'
            ELSE 'V145_SELECTABLE FAIL' END AS check_result
  FROM `v_inventory_reconcile_mes_wms`;

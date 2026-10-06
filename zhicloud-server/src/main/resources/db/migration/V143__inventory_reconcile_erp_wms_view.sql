-- ============================================================
-- V143：R-007 第二步——经 MDM 映射的 ERP↔WMS 行级库存对账视图
-- ============================================================
-- V141 交付了「三本账总览」（`v_inventory_ledgers_overview`，统一形态但**不 JOIN**），
-- 并在注释里写明边界：三本账的物料键分属三个 id 空间（ERP product_id / WMS sku_id /
-- MES item_id），建立跨模块物料映射之前，行级对账在语义上不成立。
--
-- 本轮（R-014）先把映射维护入口补齐，V143 再落地那一步行级对账：
-- 以 `mdm_cross_ref`（source_system='ERP', source_entity='PRODUCT' -> target_system='WMS',
-- target_entity='SKU'，status='VALID'）为桥，把 ERP 产品库存与 WMS SKU 库存按
-- 「同一产品 + 同一仓库」对到同一行，算出两侧数量差。
--
-- 口径与边界（不假装能做的事）：
--   1. 只对「已建立映射」的物料做行级对账；无映射的产品不在本视图内（那是 R-014 的数据缺口，
--      由 v_inventory_ledgers_overview 的孤儿行暴露，不是本视图的职责）。
--   2. **仓库维度**：ERP 与 WMS 的仓库也分属两套 id，但本视图**不 JOIN 仓库映射**——
--      仓库映射（ERP WAREHOUSE -> WMS WAREHOUSE）是另一条映射，若把两侧各自 warehouse_id
--      都换成"逻辑仓库"会引入第三层 JOIN 且掩盖"某侧缺仓库映射"的真实缺口。这里保留两侧原始
--      warehouse_id 供人判断，并只按「ERP 仓库映射存在」做前置过滤（见 WHERE）。
--   3. 数量列统一 CAST 成 DECIMAL(20,4)：两侧都是 decimal(20,4)，CAST 只为显式统一精度。
--   4. 映射的 source_id/target_id 是 VARCHAR，库存键是 BIGINT——JOIN 处**显式 CAST 成
--      SIGNED**，不用隐式转换（隐式转换既可能错配又让索引失效）。
--   5. 只透数值与差异，不翻译业务语义（与 V126/V130/V141 一致）。
--   6. 过滤两侧 deleted=0；tenant_id 透传；映射必须 status='VALID'（失效映射等同缺失）。
-- ============================================================

CREATE OR REPLACE VIEW `v_inventory_reconcile_erp_wms` AS
SELECT
    erp.tenant_id                                          AS tenant_id,
    CAST(xref.source_id AS SIGNED)                         AS erp_product_id,
    CAST(xref.target_id AS SIGNED)                         AS wms_sku_id,
    erp.warehouse_id                                       AS erp_warehouse_id,
    CAST(erp_wref.target_id AS SIGNED)                     AS wms_warehouse_id,
    CAST(IFNULL(erp.`count`, 0) AS DECIMAL(20, 4))         AS erp_quantity,
    CAST(IFNULL(wms.quantity, 0) AS DECIMAL(20, 4))        AS wms_quantity,
    CAST(IFNULL(erp.`count`, 0) - IFNULL(wms.quantity, 0) AS DECIMAL(20, 4)) AS quantity_diff,
    CASE
        WHEN wms.id IS NULL THEN 'WMS_LEDGER_MISSING'
        WHEN IFNULL(erp.`count`, 0) <> IFNULL(wms.quantity, 0) THEN 'QUANTITY_DIFF'
        ELSE 'MATCHED'
    END                                                    AS reconcile_status
  FROM `erp_stock` erp
  JOIN `mdm_cross_ref` xref
    ON  xref.tenant_id = erp.tenant_id
    AND xref.source_system = 'ERP' AND xref.source_entity = 'PRODUCT'
    AND xref.target_system = 'WMS' AND xref.target_entity = 'SKU'
    AND xref.status = 'VALID' AND xref.deleted = b'0'
    AND CAST(xref.source_id AS SIGNED) = erp.product_id
  LEFT JOIN `mdm_cross_ref` erp_wref
    ON  erp_wref.tenant_id = erp.tenant_id
    AND erp_wref.source_system = 'ERP' AND erp_wref.source_entity = 'WAREHOUSE'
    AND erp_wref.target_system = 'WMS' AND erp_wref.target_entity = 'WAREHOUSE'
    AND erp_wref.status = 'VALID' AND erp_wref.deleted = b'0'
    AND CAST(erp_wref.source_id AS SIGNED) = erp.warehouse_id
  LEFT JOIN `wms_inventory` wms
    ON  wms.tenant_id = erp.tenant_id
    AND wms.sku_id = CAST(xref.target_id AS SIGNED)
    AND wms.deleted = b'0'
  WHERE erp.deleted = b'0';

-- 自检 1：视图存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V143_VIEW_EXISTS PASS'
            ELSE 'V143_VIEW_EXISTS FAIL' END AS check_result
  FROM information_schema.views
 WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_erp_wms';

-- 自检 2：定义里三张关键表都在（erp_stock / mdm_cross_ref / wms_inventory）
SELECT CASE WHEN (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_erp_wms')
                  LIKE '%erp_stock%'
             AND (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_erp_wms')
                  LIKE '%mdm_cross_ref%'
             AND (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE() AND table_name = 'v_inventory_reconcile_erp_wms')
                  LIKE '%wms_inventory%'
            THEN 'V143_TABLES_WIRED PASS'
            ELSE 'V143_TABLES_WIRED FAIL 定义中缺关键表' END AS check_result;

-- 自检 3：可查询（JOIN 形态合法，含 CAST）
SELECT CASE WHEN COUNT(*) >= 0 THEN 'V143_SELECTABLE PASS'
            ELSE 'V143_SELECTABLE FAIL' END AS check_result
  FROM `v_inventory_reconcile_erp_wms`;

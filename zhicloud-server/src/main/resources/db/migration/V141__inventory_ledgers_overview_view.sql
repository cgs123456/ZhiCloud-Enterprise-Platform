-- ============================================================
-- V141：R-007「三套库存账无对账入口」第一步——统一形态的三本账总览视图
-- ============================================================
-- 背景（R-007，台账 §12.83/§12.86 与 business-optimization-plan 的 P0 第一条）：
-- ERP（erp_stock，产品维度 product_id）、WMS（wms_inventory，SKU 维度 sku_id）、
-- MES（mes_wm_material_stock，物料维度 item_id）三套库存台账互不同步。
-- 三者的物料主键分属三个 id 空间（连仓库表都各自独立：erp_warehouse / wms_warehouse / MES 自有仓库），
-- **在建立跨模块物料映射之前，行级对账在语义上不成立**——这是本视图不假装能做的事。
--
-- 本视图做的是把三本账归一成同一形态（来源、仓库键、物料键、数量、占用/冻结），
-- 让"一眼看完三本账"有唯一入口；后续建立物料映射后，对账 JOIN 在同一形态上进行即可。
-- 口径：过滤 deleted=0；tenant_id 透传（多租户下按租户各看各账）；只透数值不翻译语义。
-- ============================================================

CREATE OR REPLACE VIEW `v_inventory_ledgers_overview` AS
SELECT 'ERP' AS source, 'erp_stock' AS ledger_table, warehouse_id,
       product_id AS item_key, IFNULL(`count`, 0) AS quantity,
       IFNULL(locked_count, 0) AS held_quantity, CAST(NULL AS UNSIGNED) AS is_frozen, tenant_id
  FROM `erp_stock`
 WHERE deleted = 0
UNION ALL
SELECT 'WMS' AS source, 'wms_inventory' AS ledger_table, warehouse_id,
       sku_id AS item_key, IFNULL(quantity, 0) AS quantity,
       IFNULL(locked_quantity, 0) AS held_quantity, CAST(NULL AS UNSIGNED) AS is_frozen, tenant_id
  FROM `wms_inventory`
 WHERE deleted = 0
UNION ALL
SELECT 'MES' AS source, 'mes_wm_material_stock' AS ledger_table, warehouse_id,
       item_id AS item_key, IFNULL(quantity, 0) AS quantity,
       CAST(NULL AS DECIMAL(20, 6)) AS held_quantity, CAST(frozen AS UNSIGNED) AS is_frozen, tenant_id
  FROM `mes_wm_material_stock`
 WHERE deleted = 0;

-- 自检 1：视图存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V141_VIEW_EXISTS PASS'
            ELSE 'V141_VIEW_EXISTS FAIL' END AS check_result
  FROM information_schema.views
 WHERE table_schema = DATABASE() AND table_name = 'v_inventory_ledgers_overview';

-- 自检 2：三本账都在定义里（防"只 UNION 了两本"）。
-- 列名是 view_definition（V130 的教训：information_schema 的列名要 DESCRIBE 实测，
-- 不能想当然——第一版写成 PostgreSQL 的 definition，真机 1054 直接打脸）
SELECT CASE WHEN (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE()
                     AND table_name = 'v_inventory_ledgers_overview') LIKE '%erp_stock%'
             AND (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE()
                     AND table_name = 'v_inventory_ledgers_overview') LIKE '%wms_inventory%'
             AND (SELECT view_definition FROM information_schema.views
                   WHERE table_schema = DATABASE()
                     AND table_name = 'v_inventory_ledgers_overview') LIKE '%mes_wm_material_stock%'
            THEN 'V141_THREE_LEDGERS_PASS'
            ELSE 'V141_THREE_LEDGERS FAIL 定义中缺账本' END AS check_result;

-- 自检 3：可查询（UNION 形态合法）
SELECT CASE WHEN COUNT(*) >= 0 THEN 'V141_SELECTABLE PASS'
            ELSE 'V141_SELECTABLE FAIL' END AS check_result
  FROM `v_inventory_ledgers_overview`;

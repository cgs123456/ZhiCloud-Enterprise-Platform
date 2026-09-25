-- ============================================================
-- V91 人工复核脚本（只读，不改任何数据）
--
-- 场景：V91 执行后，在预发布库/生产库核对「25 个幽灵 NOT NULL 列」是否已放宽，
--       并产出 DROP COLUMN 收敛决策所需的证据。
-- 用法：mysql -h<host> -u<user> -p <db> < V91_verify.sql
-- 期望：A1 全部 nullable=YES；A2 返回 0 行；B 含 STRICT_TRANS_TABLES；
--       C 的 missed_backfill_rows = 0，mismatch_rows 需业务裁决（非 0 不等于迁移失败）；
--       C2 每列 null_rows = total_rows（DROP COLUMN 前提）；
--       D1/D2 的 orphan_rows = 0；D3b 返回 0 行。
-- 说明：本脚本只依赖 information_schema 与 SELECT COUNT(*)，不会锁写。
-- ============================================================

-- ------------------------------------------------------------
-- A1. 25 列现状：期望 25 行，nullable 全为 YES，column_default 全为 NULL
-- ------------------------------------------------------------
WITH expected (table_name, column_name) AS (
    SELECT 'erp_product', 'code'                  UNION ALL
    SELECT 'erp_supplier', 'code'                 UNION ALL
    SELECT 'erp_customer', 'code'                 UNION ALL
    SELECT 'erp_warehouse', 'code'                UNION ALL
    SELECT 'erp_account', 'code'                  UNION ALL
    SELECT 'erp_account', 'type'                  UNION ALL
    SELECT 'erp_stock_move', 'from_warehouse_id'  UNION ALL
    SELECT 'erp_stock_move', 'to_warehouse_id'    UNION ALL
    SELECT 'erp_stock_check', 'warehouse_id'      UNION ALL
    SELECT 'erp_purchase_order_items', 'master_id'  UNION ALL
    SELECT 'erp_purchase_in_items', 'master_id'     UNION ALL
    SELECT 'erp_purchase_return_items', 'master_id' UNION ALL
    SELECT 'erp_sale_order_items', 'master_id'      UNION ALL
    SELECT 'erp_sale_out_items', 'master_id'        UNION ALL
    SELECT 'erp_sale_return_items', 'master_id'     UNION ALL
    SELECT 'erp_stock_in_item', 'master_id'         UNION ALL
    SELECT 'erp_stock_out_item', 'master_id'        UNION ALL
    SELECT 'erp_stock_move_item', 'master_id'       UNION ALL
    SELECT 'erp_stock_check_item', 'master_id'      UNION ALL
    SELECT 'erp_finance_payment_item', 'master_id'   UNION ALL
    SELECT 'erp_finance_payment_item', 'bill_id'     UNION ALL
    SELECT 'erp_finance_payment_item', 'bill_type'   UNION ALL
    SELECT 'erp_finance_receipt_item', 'master_id'   UNION ALL
    SELECT 'erp_finance_receipt_item', 'bill_id'     UNION ALL
    SELECT 'erp_finance_receipt_item', 'bill_type'
)
SELECT e.table_name,
       e.column_name,
       ic.COLUMN_TYPE,
       ic.IS_NULLABLE,
       ic.COLUMN_DEFAULT,
       ic.COLUMN_COMMENT
  FROM expected e
  LEFT JOIN information_schema.columns ic
         ON ic.table_schema = DATABASE()
        AND ic.table_name = e.table_name
        AND ic.column_name = e.column_name
 ORDER BY e.table_name, e.column_name;

-- ------------------------------------------------------------
-- A2. 仍未放宽的列：期望 0 行（有输出即 V91 未生效或不完整）
-- ------------------------------------------------------------
WITH expected (table_name, column_name) AS (
    SELECT 'erp_product', 'code'                  UNION ALL
    SELECT 'erp_supplier', 'code'                 UNION ALL
    SELECT 'erp_customer', 'code'                 UNION ALL
    SELECT 'erp_warehouse', 'code'                UNION ALL
    SELECT 'erp_account', 'code'                  UNION ALL
    SELECT 'erp_account', 'type'                  UNION ALL
    SELECT 'erp_stock_move', 'from_warehouse_id'  UNION ALL
    SELECT 'erp_stock_move', 'to_warehouse_id'    UNION ALL
    SELECT 'erp_stock_check', 'warehouse_id'      UNION ALL
    SELECT 'erp_purchase_order_items', 'master_id'  UNION ALL
    SELECT 'erp_purchase_in_items', 'master_id'     UNION ALL
    SELECT 'erp_purchase_return_items', 'master_id' UNION ALL
    SELECT 'erp_sale_order_items', 'master_id'      UNION ALL
    SELECT 'erp_sale_out_items', 'master_id'        UNION ALL
    SELECT 'erp_sale_return_items', 'master_id'     UNION ALL
    SELECT 'erp_stock_in_item', 'master_id'         UNION ALL
    SELECT 'erp_stock_out_item', 'master_id'        UNION ALL
    SELECT 'erp_stock_move_item', 'master_id'       UNION ALL
    SELECT 'erp_stock_check_item', 'master_id'      UNION ALL
    SELECT 'erp_finance_payment_item', 'master_id'   UNION ALL
    SELECT 'erp_finance_payment_item', 'bill_id'     UNION ALL
    SELECT 'erp_finance_payment_item', 'bill_type'   UNION ALL
    SELECT 'erp_finance_receipt_item', 'master_id'   UNION ALL
    SELECT 'erp_finance_receipt_item', 'bill_id'     UNION ALL
    SELECT 'erp_finance_receipt_item', 'bill_type'
)
SELECT e.table_name, e.column_name,
       COALESCE(ic.IS_NULLABLE, 'COLUMN_MISSING') AS is_nullable
  FROM expected e
  LEFT JOIN information_schema.columns ic
         ON ic.table_schema = DATABASE()
        AND ic.table_name = e.table_name
        AND ic.column_name = e.column_name
 WHERE ic.column_name IS NULL OR ic.IS_NULLABLE <> 'YES';

-- ------------------------------------------------------------
-- B. sql_mode：生产必须含 STRICT_TRANS_TABLES
-- ------------------------------------------------------------
SELECT @@GLOBAL.sql_mode AS global_sql_mode, @@SESSION.sql_mode AS session_sql_mode;
-- ============================================================
-- C. 旧列 <- 新列 取值一致性（V91 §3 回填核对）
--
-- 机制（V91 源码）：p_v91_copy_legacy 只在 `旧列 IS NULL AND 新列 IS NOT NULL`
--   时才 `UPDATE ... SET 旧列 = 新列`，即「只补 NULL、不覆盖存量值」。
-- 因此三类差异要分开看：
--   missed_backfill_rows：旧列为 NULL 但新列有值 —— 回填遗漏，需修；
--   legacy_only_rows    ：新列为 NULL 但旧列有值 —— 历史数据（V82 之前写入），
--                         属预期存在；V91 不会把它反推回新列（新列才是真相源，
--                         但这批行需要一次性数据迁移，见 03 号方案）；
--   mismatch_rows       ：两列都有值但不等 —— V91 不会覆盖旧值，需人工裁决。
-- 期望：missed_backfill_rows = 0；mismatch_rows 需逐行确认（非 0 不等于迁移失败）。
-- ============================================================
SELECT 'erp_purchase_order_items.master_id <- order_id' AS mapping,
       SUM(master_id IS NULL AND order_id IS NOT NULL) AS missed_backfill_rows,
       SUM(order_id IS NULL AND master_id IS NOT NULL) AS legacy_only_rows,
       SUM(NOT (master_id <=> order_id)) AS mismatch_rows
  FROM erp_purchase_order_items WHERE deleted = 0
UNION ALL
SELECT 'erp_purchase_in_items.master_id <- in_id',
       SUM(master_id IS NULL AND in_id IS NOT NULL),
       SUM(in_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> in_id))
  FROM erp_purchase_in_items WHERE deleted = 0
UNION ALL
SELECT 'erp_purchase_return_items.master_id <- return_id',
       SUM(master_id IS NULL AND return_id IS NOT NULL),
       SUM(return_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> return_id))
  FROM erp_purchase_return_items WHERE deleted = 0
UNION ALL
SELECT 'erp_sale_order_items.master_id <- order_id',
       SUM(master_id IS NULL AND order_id IS NOT NULL),
       SUM(order_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> order_id))
  FROM erp_sale_order_items WHERE deleted = 0
UNION ALL
SELECT 'erp_sale_out_items.master_id <- out_id',
       SUM(master_id IS NULL AND out_id IS NOT NULL),
       SUM(out_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> out_id))
  FROM erp_sale_out_items WHERE deleted = 0
UNION ALL
SELECT 'erp_sale_return_items.master_id <- return_id',
       SUM(master_id IS NULL AND return_id IS NOT NULL),
       SUM(return_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> return_id))
  FROM erp_sale_return_items WHERE deleted = 0
UNION ALL
SELECT 'erp_stock_in_item.master_id <- in_id',
       SUM(master_id IS NULL AND in_id IS NOT NULL),
       SUM(in_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> in_id))
  FROM erp_stock_in_item WHERE deleted = 0
UNION ALL
SELECT 'erp_stock_out_item.master_id <- out_id',
       SUM(master_id IS NULL AND out_id IS NOT NULL),
       SUM(out_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> out_id))
  FROM erp_stock_out_item WHERE deleted = 0
UNION ALL
SELECT 'erp_stock_move_item.master_id <- move_id',
       SUM(master_id IS NULL AND move_id IS NOT NULL),
       SUM(move_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> move_id))
  FROM erp_stock_move_item WHERE deleted = 0
UNION ALL
SELECT 'erp_stock_check_item.master_id <- check_id',
       SUM(master_id IS NULL AND check_id IS NOT NULL),
       SUM(check_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> check_id))
  FROM erp_stock_check_item WHERE deleted = 0
UNION ALL
SELECT 'erp_finance_payment_item.master_id <- payment_id',
       SUM(master_id IS NULL AND payment_id IS NOT NULL),
       SUM(payment_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> payment_id))
  FROM erp_finance_payment_item WHERE deleted = 0
UNION ALL
SELECT 'erp_finance_payment_item.bill_id <- biz_id',
       SUM(bill_id IS NULL AND biz_id IS NOT NULL),
       SUM(biz_id IS NULL AND bill_id IS NOT NULL),
       SUM(NOT (bill_id <=> biz_id))
  FROM erp_finance_payment_item WHERE deleted = 0
UNION ALL
SELECT 'erp_finance_payment_item.bill_type <- biz_type',
       SUM(bill_type IS NULL AND biz_type IS NOT NULL),
       SUM(biz_type IS NULL AND bill_type IS NOT NULL),
       SUM(NOT (bill_type <=> biz_type))
  FROM erp_finance_payment_item WHERE deleted = 0
UNION ALL
SELECT 'erp_finance_receipt_item.master_id <- receipt_id',
       SUM(master_id IS NULL AND receipt_id IS NOT NULL),
       SUM(receipt_id IS NULL AND master_id IS NOT NULL),
       SUM(NOT (master_id <=> receipt_id))
  FROM erp_finance_receipt_item WHERE deleted = 0
UNION ALL
SELECT 'erp_finance_receipt_item.bill_id <- biz_id',
       SUM(bill_id IS NULL AND biz_id IS NOT NULL),
       SUM(biz_id IS NULL AND bill_id IS NOT NULL),
       SUM(NOT (bill_id <=> biz_id))
  FROM erp_finance_receipt_item WHERE deleted = 0
UNION ALL
SELECT 'erp_finance_receipt_item.bill_type <- biz_type',
       SUM(bill_type IS NULL AND biz_type IS NOT NULL),
       SUM(biz_type IS NULL AND bill_type IS NOT NULL),
       SUM(NOT (bill_type <=> biz_type))
  FROM erp_finance_receipt_item WHERE deleted = 0;

-- ------------------------------------------------------------
-- C2. 25 个幽灵列的 NULL 比例 —— DROP COLUMN 决策依据（期望 null_rows = total_rows）
--     若某列存在非 NULL 值，先查清来源（历史导入/旧 SQL/手工运维），不可直接 DROP。
-- ------------------------------------------------------------
SELECT 'erp_product' AS table_name, 'code' AS column_name, COUNT(*) AS total_rows, SUM(code IS NULL) AS null_rows FROM erp_product
UNION ALL SELECT 'erp_supplier', 'code', COUNT(*), SUM(code IS NULL) FROM erp_supplier
UNION ALL SELECT 'erp_customer', 'code', COUNT(*), SUM(code IS NULL) FROM erp_customer
UNION ALL SELECT 'erp_warehouse', 'code', COUNT(*), SUM(code IS NULL) FROM erp_warehouse
UNION ALL SELECT 'erp_account', 'code', COUNT(*), SUM(code IS NULL) FROM erp_account
UNION ALL SELECT 'erp_account', 'type', COUNT(*), SUM(type IS NULL) FROM erp_account
UNION ALL SELECT 'erp_stock_move', 'from_warehouse_id', COUNT(*), SUM(from_warehouse_id IS NULL) FROM erp_stock_move
UNION ALL SELECT 'erp_stock_move', 'to_warehouse_id', COUNT(*), SUM(to_warehouse_id IS NULL) FROM erp_stock_move
UNION ALL SELECT 'erp_stock_check', 'warehouse_id', COUNT(*), SUM(warehouse_id IS NULL) FROM erp_stock_check
UNION ALL SELECT 'erp_purchase_order_items', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_purchase_order_items
UNION ALL SELECT 'erp_purchase_in_items', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_purchase_in_items
UNION ALL SELECT 'erp_purchase_return_items', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_purchase_return_items
UNION ALL SELECT 'erp_sale_order_items', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_sale_order_items
UNION ALL SELECT 'erp_sale_out_items', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_sale_out_items
UNION ALL SELECT 'erp_sale_return_items', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_sale_return_items
UNION ALL SELECT 'erp_stock_in_item', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_stock_in_item
UNION ALL SELECT 'erp_stock_out_item', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_stock_out_item
UNION ALL SELECT 'erp_stock_move_item', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_stock_move_item
UNION ALL SELECT 'erp_stock_check_item', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_stock_check_item
UNION ALL SELECT 'erp_finance_payment_item', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_finance_payment_item
UNION ALL SELECT 'erp_finance_payment_item', 'bill_id', COUNT(*), SUM(bill_id IS NULL) FROM erp_finance_payment_item
UNION ALL SELECT 'erp_finance_payment_item', 'bill_type', COUNT(*), SUM(bill_type IS NULL) FROM erp_finance_payment_item
UNION ALL SELECT 'erp_finance_receipt_item', 'master_id', COUNT(*), SUM(master_id IS NULL) FROM erp_finance_receipt_item
UNION ALL SELECT 'erp_finance_receipt_item', 'bill_id', COUNT(*), SUM(bill_id IS NULL) FROM erp_finance_receipt_item
UNION ALL SELECT 'erp_finance_receipt_item', 'bill_type', COUNT(*), SUM(bill_type IS NULL) FROM erp_finance_receipt_item;

-- ------------------------------------------------------------
-- C3. code 系列唯一索引现状（V91 放宽可空性后仍存在）：
--     期望 non_unique = 0，columns_in_order 以 code, tenant_id 顺序出现。
--     这些索引是 DROP COLUMN 前必须先决策的对象：code 全 NULL 时索引仍占空间，
--     但 MySQL 唯一索引允许多个 NULL，不会阻塞写入。
-- ------------------------------------------------------------
SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE,
       GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns_in_order
  FROM information_schema.statistics
 WHERE TABLE_SCHEMA = DATABASE()
   AND INDEX_NAME IN ('uk_erp_product_code_tenant', 'uk_erp_supplier_code_tenant',
                      'uk_erp_customer_code_tenant', 'uk_erp_warehouse_code_tenant',
                      'uk_erp_account_code_tenant')
 GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
 ORDER BY TABLE_NAME;
-- ============================================================
-- D. 旧列孤儿指向 + 唯一索引冲突预演
--
-- 说明：V82 之后「新列是真相源、旧列是兼容副本」，V91 又把旧列放宽为 NULL。
--       因此旧列的孤儿指向不会在写入时立刻报错（可能长期潜伏），必须显式核对。
--       期望：D1/D2 每项 orphan_rows = 0；D3 的重复明细 0 行。
-- ============================================================
-- D1. 明细 master_id -> 主表（12 张明细，期望 0）
SELECT 'erp_purchase_order_items.master_id -> erp_purchase_order' AS check_name, COUNT(*) AS orphan_rows
  FROM erp_purchase_order_items i LEFT JOIN erp_purchase_order r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_in_items.master_id -> erp_purchase_in', COUNT(*)
  FROM erp_purchase_in_items i LEFT JOIN erp_purchase_in r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_return_items.master_id -> erp_purchase_return', COUNT(*)
  FROM erp_purchase_return_items i LEFT JOIN erp_purchase_return r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_order_items.master_id -> erp_sale_order', COUNT(*)
  FROM erp_sale_order_items i LEFT JOIN erp_sale_order r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_out_items.master_id -> erp_sale_out', COUNT(*)
  FROM erp_sale_out_items i LEFT JOIN erp_sale_out r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_return_items.master_id -> erp_sale_return', COUNT(*)
  FROM erp_sale_return_items i LEFT JOIN erp_sale_return r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_stock_in_item.master_id -> erp_stock_in', COUNT(*)
  FROM erp_stock_in_item i LEFT JOIN erp_stock_in r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_stock_out_item.master_id -> erp_stock_out', COUNT(*)
  FROM erp_stock_out_item i LEFT JOIN erp_stock_out r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_stock_move_item.master_id -> erp_stock_move', COUNT(*)
  FROM erp_stock_move_item i LEFT JOIN erp_stock_move r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_stock_check_item.master_id -> erp_stock_check', COUNT(*)
  FROM erp_stock_check_item i LEFT JOIN erp_stock_check r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_finance_payment_item.master_id -> erp_finance_payment', COUNT(*)
  FROM erp_finance_payment_item i LEFT JOIN erp_finance_payment r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_finance_receipt_item.master_id -> erp_finance_receipt', COUNT(*)
  FROM erp_finance_receipt_item i LEFT JOIN erp_finance_receipt r ON r.id = i.master_id
 WHERE i.deleted = 0 AND i.master_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0);

-- D2. finance item.bill_id -> 业务单据（按 biz_type，期望 0）
-- 映射依据：V90 §7.4（11 采购入库 / 12 采购退货 / 21 销售出库 / 22 销售退货）。
SELECT 'erp_finance_payment_item.bill_id (biz_type=11) -> erp_purchase_in' AS check_name, COUNT(*) AS orphan_rows
  FROM erp_finance_payment_item i LEFT JOIN erp_purchase_in r ON r.id = i.bill_id
 WHERE i.deleted = 0 AND i.biz_type = 11 AND i.bill_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_finance_payment_item.bill_id (biz_type=12) -> erp_purchase_return', COUNT(*)
  FROM erp_finance_payment_item i LEFT JOIN erp_purchase_return r ON r.id = i.bill_id
 WHERE i.deleted = 0 AND i.biz_type = 12 AND i.bill_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_finance_receipt_item.bill_id (biz_type=21) -> erp_sale_out', COUNT(*)
  FROM erp_finance_receipt_item i LEFT JOIN erp_sale_out r ON r.id = i.bill_id
 WHERE i.deleted = 0 AND i.biz_type = 21 AND i.bill_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_finance_receipt_item.bill_id (biz_type=22) -> erp_sale_return', COUNT(*)
  FROM erp_finance_receipt_item i LEFT JOIN erp_sale_return r ON r.id = i.bill_id
 WHERE i.deleted = 0 AND i.biz_type = 22 AND i.bill_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_finance_payment_item.biz_type 不在 11/12 的行', COUNT(*)
  FROM erp_finance_payment_item WHERE deleted = 0 AND (biz_type IS NULL OR biz_type NOT IN (11,12))
UNION ALL
SELECT 'erp_finance_receipt_item.biz_type 不在 21/22 的行', COUNT(*)
  FROM erp_finance_receipt_item WHERE deleted = 0 AND (biz_type IS NULL OR biz_type NOT IN (21,22));

-- D3a. 唯一索引冲突预演：若将来把 code 回填，是否会撞 uk_erp_*_code_tenant
--      期望：potential_duplicate_rows = 0（当前 code 全 NULL 时必然为 0；
--      一旦有人手工回填 code，本查询立即能发现）。
SELECT 'erp_product' AS table_name, COUNT(*) AS total_rows, SUM(code IS NOT NULL) AS non_null_code_rows,
       SUM(code IS NOT NULL) - COUNT(DISTINCT CASE WHEN code IS NOT NULL THEN CONCAT(COALESCE(tenant_id, 0), ':', code) END) AS potential_duplicate_rows
  FROM erp_product
UNION ALL SELECT 'erp_supplier', COUNT(*), SUM(code IS NOT NULL),
       SUM(code IS NOT NULL) - COUNT(DISTINCT CASE WHEN code IS NOT NULL THEN CONCAT(COALESCE(tenant_id, 0), ':', code) END) FROM erp_supplier
UNION ALL SELECT 'erp_customer', COUNT(*), SUM(code IS NOT NULL),
       SUM(code IS NOT NULL) - COUNT(DISTINCT CASE WHEN code IS NOT NULL THEN CONCAT(COALESCE(tenant_id, 0), ':', code) END) FROM erp_customer
UNION ALL SELECT 'erp_warehouse', COUNT(*), SUM(code IS NOT NULL),
       SUM(code IS NOT NULL) - COUNT(DISTINCT CASE WHEN code IS NOT NULL THEN CONCAT(COALESCE(tenant_id, 0), ':', code) END) FROM erp_warehouse
UNION ALL SELECT 'erp_account', COUNT(*), SUM(code IS NOT NULL),
       SUM(code IS NOT NULL) - COUNT(DISTINCT CASE WHEN code IS NOT NULL THEN CONCAT(COALESCE(tenant_id, 0), ':', code) END) FROM erp_account;

-- D3b. 重复键明细：期望 0 行（有输出即为 1062 预演失败，不可回填该 code）
SELECT 'erp_product' AS table_name, code, tenant_id, COUNT(*) AS dup_rows
  FROM erp_product WHERE code IS NOT NULL GROUP BY code, tenant_id HAVING COUNT(*) > 1
UNION ALL SELECT 'erp_supplier', code, tenant_id, COUNT(*) FROM erp_supplier WHERE code IS NOT NULL GROUP BY code, tenant_id HAVING COUNT(*) > 1
UNION ALL SELECT 'erp_customer', code, tenant_id, COUNT(*) FROM erp_customer WHERE code IS NOT NULL GROUP BY code, tenant_id HAVING COUNT(*) > 1
UNION ALL SELECT 'erp_warehouse', code, tenant_id, COUNT(*) FROM erp_warehouse WHERE code IS NOT NULL GROUP BY code, tenant_id HAVING COUNT(*) > 1
UNION ALL SELECT 'erp_account', code, tenant_id, COUNT(*) FROM erp_account WHERE code IS NOT NULL GROUP BY code, tenant_id HAVING COUNT(*) > 1
 ORDER BY table_name, tenant_id, code;

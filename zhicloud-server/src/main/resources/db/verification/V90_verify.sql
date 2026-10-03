-- ============================================================
-- V90 人工复核脚本（只读，不改任何业务数据）
--
-- 目的：V90 修复了 6 张 ERP 单据主表缺失的 71 个 DO 列（读取路径 Unknown column）。
--       本脚本在预发布库/生产库核对修复结果，并把「存量数据口径差异」从
--       「迁移失败」中区分出来，避免误判。
--
-- 用法：mysql -h<host> -u<user> -p --default-character-set=utf8mb4 <db> < V90_verify.sql
-- 期望：J1 = 71/71/0；J1b = 0 行；J2/J3 的差异行需业务确认（V90 只补 NULL、
--       不覆盖存量值，非 0 不等于迁移失败）；J4/J5/J6/J7 期望 0 行；
--       J8 期望 11 行且 index_exists = 1、actual_column 与 expected_column 一致。
--
-- 限界（重要）：
--   * 71 列清单抄自 V90__fix_erp_bill_master_columns.sql §9 自检段，属静态推导；
--     本脚本需要连真库运行，是把静态结论升级为动态证据的正确方式。
--   * 只创建 TEMPORARY TABLE（会话级，断开即释放），不修改任何业务表；
--     只读账号需额外具备 CREATE TEMPORARY TABLES 权限。
--   * 金额/数量的「期望 0」只在 V90 兜底覆盖的行上成立；存量行由业务口径决定。
-- ============================================================
SET @v90_verify_tolerance = 0.0001;   -- DECIMAL(20,4) 金额/数量容差

DROP TEMPORARY TABLE IF EXISTS tmp_v90_verify_expected;
CREATE TEMPORARY TABLE tmp_v90_verify_expected (
    table_name  VARCHAR(64) NOT NULL,
    column_name VARCHAR(64) NOT NULL,
    PRIMARY KEY (table_name, column_name)
) ENGINE = MEMORY;

INSERT INTO tmp_v90_verify_expected (table_name, column_name) VALUES
    ('erp_sale_order', 'account_id'),
    ('erp_sale_order', 'sale_user_id'),
    ('erp_sale_order', 'order_time'),
    ('erp_sale_order', 'total_product_price'),
    ('erp_sale_order', 'total_tax_price'),
    ('erp_sale_order', 'total_price'),
    ('erp_sale_order', 'discount_percent'),
    ('erp_sale_order', 'discount_price'),
    ('erp_sale_order', 'deposit_price'),
    ('erp_sale_order', 'out_count'),
    ('erp_sale_order', 'return_count'),
    ('erp_sale_order', 'file_url'),
    ('erp_purchase_order', 'account_id'),
    ('erp_purchase_order', 'order_time'),
    ('erp_purchase_order', 'total_product_price'),
    ('erp_purchase_order', 'total_tax_price'),
    ('erp_purchase_order', 'total_price'),
    ('erp_purchase_order', 'discount_percent'),
    ('erp_purchase_order', 'discount_price'),
    ('erp_purchase_order', 'deposit_price'),
    ('erp_purchase_order', 'in_count'),
    ('erp_purchase_order', 'return_count'),
    ('erp_purchase_order', 'file_url'),
    ('erp_purchase_in', 'account_id'),
    ('erp_purchase_in', 'order_id'),
    ('erp_purchase_in', 'order_no'),
    ('erp_purchase_in', 'total_product_price'),
    ('erp_purchase_in', 'total_tax_price'),
    ('erp_purchase_in', 'total_price'),
    ('erp_purchase_in', 'discount_percent'),
    ('erp_purchase_in', 'discount_price'),
    ('erp_purchase_in', 'other_price'),
    ('erp_purchase_in', 'payment_price'),
    ('erp_purchase_in', 'file_url'),
    ('erp_sale_out', 'account_id'),
    ('erp_sale_out', 'sale_user_id'),
    ('erp_sale_out', 'order_id'),
    ('erp_sale_out', 'order_no'),
    ('erp_sale_out', 'total_product_price'),
    ('erp_sale_out', 'total_tax_price'),
    ('erp_sale_out', 'total_price'),
    ('erp_sale_out', 'discount_percent'),
    ('erp_sale_out', 'discount_price'),
    ('erp_sale_out', 'other_price'),
    ('erp_sale_out', 'receipt_price'),
    ('erp_sale_out', 'file_url'),
    ('erp_purchase_return', 'account_id'),
    ('erp_purchase_return', 'order_id'),
    ('erp_purchase_return', 'order_no'),
    ('erp_purchase_return', 'return_time'),
    ('erp_purchase_return', 'total_product_price'),
    ('erp_purchase_return', 'total_tax_price'),
    ('erp_purchase_return', 'total_price'),
    ('erp_purchase_return', 'discount_percent'),
    ('erp_purchase_return', 'discount_price'),
    ('erp_purchase_return', 'other_price'),
    ('erp_purchase_return', 'refund_price'),
    ('erp_purchase_return', 'file_url'),
    ('erp_sale_return', 'account_id'),
    ('erp_sale_return', 'sale_user_id'),
    ('erp_sale_return', 'order_id'),
    ('erp_sale_return', 'order_no'),
    ('erp_sale_return', 'return_time'),
    ('erp_sale_return', 'total_product_price'),
    ('erp_sale_return', 'total_tax_price'),
    ('erp_sale_return', 'total_price'),
    ('erp_sale_return', 'discount_percent'),
    ('erp_sale_return', 'discount_price'),
    ('erp_sale_return', 'other_price'),
    ('erp_sale_return', 'refund_price'),
    ('erp_sale_return', 'file_url');

-- ------------------------------------------------------------
-- J1. 期望列数 / 已存在列数 / 缺失列数：期望 71 / 71 / 0
-- ------------------------------------------------------------
SELECT COUNT(*) AS expected_columns,
       SUM(ic.column_name IS NOT NULL) AS present_columns,
       SUM(ic.column_name IS NULL)     AS missing_columns
  FROM tmp_v90_verify_expected e
  LEFT JOIN information_schema.columns ic
         ON ic.table_schema = DATABASE()
        AND ic.table_name = e.table_name
        AND ic.column_name = e.column_name;

-- ------------------------------------------------------------
-- J1b. 缺失明细：期望 0 行（有输出即 V90 未生效或执行不完整）
-- ------------------------------------------------------------
SELECT e.table_name, e.column_name, 'MISSING' AS verdict
  FROM tmp_v90_verify_expected e
  LEFT JOIN information_schema.columns ic
         ON ic.table_schema = DATABASE()
        AND ic.table_name = e.table_name
        AND ic.column_name = e.column_name
 WHERE ic.column_name IS NULL
 ORDER BY e.table_name, e.column_name;
-- ------------------------------------------------------------
-- J2. 主表金额 vs 明细汇总（观察项，不是迁移失败判据）
--
-- 口径依据（V90 §7.4.1，detail-first 兜底）：
--   总商品额 = SUM(明细.total_price)，总税额 = SUM(明细.tax_price)；
--   V90 仅当主表新列为 NULL 时才用明细汇总补，不覆盖存量值。
-- DO 注释口径：total_price = 不含税合计 + 税费 - 优惠 + 其它费用。
--   * 订单主表（purchase_order / sale_order）没有 other_price；
--   * 出入库/退货主表有 other_price；
--   * discount_price / deposit_price 是否参与合计、以及 deposit_price 是否属于
--     「优惠」，代码与 DDL 注释不一致，属未决口径，需业务确认后再收紧门禁。
-- 因此本查询的作用是「把差异行列出来」，差异行数非 0 时逐行确认口径。
-- ------------------------------------------------------------
SELECT 'erp_purchase_order' AS master_table,
       SUM(ABS(COALESCE(m.total_product_price,0) - COALESCE(x.product_sum,0)) > @v90_verify_tolerance) AS product_price_diff_rows,
       SUM(ABS(COALESCE(m.total_tax_price,0)     - COALESCE(x.tax_sum,0))     > @v90_verify_tolerance) AS tax_price_diff_rows,
       SUM(ABS(COALESCE(m.total_price,0) - COALESCE(m.total_product_price,0) - COALESCE(m.total_tax_price,0)) > @v90_verify_tolerance) AS total_equation_diff_rows
  FROM erp_purchase_order m
  LEFT JOIN (
      SELECT i.order_id AS master_id,
             SUM(COALESCE(i.total_price,0)) AS product_sum,
             SUM(COALESCE(i.tax_price,0))   AS tax_sum
        FROM erp_purchase_order_items i
       WHERE i.deleted = 0 AND i.order_id IS NOT NULL
       GROUP BY i.order_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_purchase_in',
       SUM(ABS(COALESCE(m.total_product_price,0) - COALESCE(x.product_sum,0)) > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_tax_price,0)     - COALESCE(x.tax_sum,0))     > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_price,0) - COALESCE(m.total_product_price,0) - COALESCE(m.total_tax_price,0)) > @v90_verify_tolerance)
  FROM erp_purchase_in m
  LEFT JOIN (
      SELECT i.in_id AS master_id,
             SUM(COALESCE(i.total_price,0)) AS product_sum,
             SUM(COALESCE(i.tax_price,0))   AS tax_sum
        FROM erp_purchase_in_items i
       WHERE i.deleted = 0 AND i.in_id IS NOT NULL
       GROUP BY i.in_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_purchase_return',
       SUM(ABS(COALESCE(m.total_product_price,0) - COALESCE(x.product_sum,0)) > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_tax_price,0)     - COALESCE(x.tax_sum,0))     > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_price,0) - COALESCE(m.total_product_price,0) - COALESCE(m.total_tax_price,0)) > @v90_verify_tolerance)
  FROM erp_purchase_return m
  LEFT JOIN (
      SELECT i.return_id AS master_id,
             SUM(COALESCE(i.total_price,0)) AS product_sum,
             SUM(COALESCE(i.tax_price,0))   AS tax_sum
        FROM erp_purchase_return_items i
       WHERE i.deleted = 0 AND i.return_id IS NOT NULL
       GROUP BY i.return_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_order',
       SUM(ABS(COALESCE(m.total_product_price,0) - COALESCE(x.product_sum,0)) > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_tax_price,0)     - COALESCE(x.tax_sum,0))     > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_price,0) - COALESCE(m.total_product_price,0) - COALESCE(m.total_tax_price,0)) > @v90_verify_tolerance)
  FROM erp_sale_order m
  LEFT JOIN (
      SELECT i.order_id AS master_id,
             SUM(COALESCE(i.total_price,0)) AS product_sum,
             SUM(COALESCE(i.tax_price,0))   AS tax_sum
        FROM erp_sale_order_items i
       WHERE i.deleted = 0 AND i.order_id IS NOT NULL
       GROUP BY i.order_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_out',
       SUM(ABS(COALESCE(m.total_product_price,0) - COALESCE(x.product_sum,0)) > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_tax_price,0)     - COALESCE(x.tax_sum,0))     > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_price,0) - COALESCE(m.total_product_price,0) - COALESCE(m.total_tax_price,0)) > @v90_verify_tolerance)
  FROM erp_sale_out m
  LEFT JOIN (
      SELECT i.out_id AS master_id,
             SUM(COALESCE(i.total_price,0)) AS product_sum,
             SUM(COALESCE(i.tax_price,0))   AS tax_sum
        FROM erp_sale_out_items i
       WHERE i.deleted = 0 AND i.out_id IS NOT NULL
       GROUP BY i.out_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_return',
       SUM(ABS(COALESCE(m.total_product_price,0) - COALESCE(x.product_sum,0)) > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_tax_price,0)     - COALESCE(x.tax_sum,0))     > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.total_price,0) - COALESCE(m.total_product_price,0) - COALESCE(m.total_tax_price,0)) > @v90_verify_tolerance)
  FROM erp_sale_return m
  LEFT JOIN (
      SELECT i.return_id AS master_id,
             SUM(COALESCE(i.total_price,0)) AS product_sum,
             SUM(COALESCE(i.tax_price,0))   AS tax_sum
        FROM erp_sale_return_items i
       WHERE i.deleted = 0 AND i.return_id IS NOT NULL
       GROUP BY i.return_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0;
-- ------------------------------------------------------------
-- J3. 订单主表已入库/已出库/已退货数量 vs 明细汇总（观察项）
-- 口径依据：V90 §7.2 明确采用 SUM(明细.in_count/out_count/return_count)，
--           且 V90 用 COALESCE(主表列, 汇总) 兜底、不覆盖存量值。
-- ------------------------------------------------------------
SELECT 'erp_purchase_order' AS master_table,
       SUM(ABS(COALESCE(m.in_count,0)     - COALESCE(x.in_count_sum,0))     > @v90_verify_tolerance) AS in_count_diff_rows,
       SUM(ABS(COALESCE(m.return_count,0) - COALESCE(x.return_count_sum,0)) > @v90_verify_tolerance) AS return_count_diff_rows
  FROM erp_purchase_order m
  LEFT JOIN (
      SELECT i.order_id AS master_id,
             SUM(COALESCE(i.in_count,0))     AS in_count_sum,
             SUM(COALESCE(i.return_count,0)) AS return_count_sum
        FROM erp_purchase_order_items i
       WHERE i.deleted = 0 AND i.order_id IS NOT NULL
       GROUP BY i.order_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_order',
       SUM(ABS(COALESCE(m.out_count,0)    - COALESCE(x.out_count_sum,0))    > @v90_verify_tolerance),
       SUM(ABS(COALESCE(m.return_count,0) - COALESCE(x.return_count_sum,0)) > @v90_verify_tolerance)
  FROM erp_sale_order m
  LEFT JOIN (
      SELECT i.order_id AS master_id,
             SUM(COALESCE(i.out_count,0))    AS out_count_sum,
             SUM(COALESCE(i.return_count,0)) AS return_count_sum
        FROM erp_sale_order_items i
       WHERE i.deleted = 0 AND i.order_id IS NOT NULL
       GROUP BY i.order_id
  ) x ON x.master_id = m.id
 WHERE m.deleted = 0;

-- ------------------------------------------------------------
-- J3b. 主表 total_count vs 明细基础数量汇总（口径观察项）
-- 说明：V82 把 purchase_count/sale_count 复制为 count（注释「count（原列 xxx）」
--       [已核验]）。主表 total_count 的口径没有对应的 V90 回填语句，因此这里
--       只输出差异行数供业务确认；「期望 0」不是本脚本的门禁条件。
-- ------------------------------------------------------------
SELECT 'erp_purchase_order' AS master_table,
       SUM(ABS(COALESCE(m.total_count,0) - COALESCE(x.count_sum,0)) > @v90_verify_tolerance) AS total_count_diff_rows
  FROM erp_purchase_order m
  LEFT JOIN (SELECT i.order_id AS master_id, SUM(COALESCE(i.count,0)) AS count_sum
               FROM erp_purchase_order_items i WHERE i.deleted = 0 AND i.order_id IS NOT NULL GROUP BY i.order_id) x
    ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_purchase_in',
       SUM(ABS(COALESCE(m.total_count,0) - COALESCE(x.count_sum,0)) > @v90_verify_tolerance)
  FROM erp_purchase_in m
  LEFT JOIN (SELECT i.in_id AS master_id, SUM(COALESCE(i.count,0)) AS count_sum
               FROM erp_purchase_in_items i WHERE i.deleted = 0 AND i.in_id IS NOT NULL GROUP BY i.in_id) x
    ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_purchase_return',
       SUM(ABS(COALESCE(m.total_count,0) - COALESCE(x.count_sum,0)) > @v90_verify_tolerance)
  FROM erp_purchase_return m
  LEFT JOIN (SELECT i.return_id AS master_id, SUM(COALESCE(i.count,0)) AS count_sum
               FROM erp_purchase_return_items i WHERE i.deleted = 0 AND i.return_id IS NOT NULL GROUP BY i.return_id) x
    ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_order',
       SUM(ABS(COALESCE(m.total_count,0) - COALESCE(x.count_sum,0)) > @v90_verify_tolerance)
  FROM erp_sale_order m
  LEFT JOIN (SELECT i.order_id AS master_id, SUM(COALESCE(i.count,0)) AS count_sum
               FROM erp_sale_order_items i WHERE i.deleted = 0 AND i.order_id IS NOT NULL GROUP BY i.order_id) x
    ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_out',
       SUM(ABS(COALESCE(m.total_count,0) - COALESCE(x.count_sum,0)) > @v90_verify_tolerance)
  FROM erp_sale_out m
  LEFT JOIN (SELECT i.out_id AS master_id, SUM(COALESCE(i.count,0)) AS count_sum
               FROM erp_sale_out_items i WHERE i.deleted = 0 AND i.out_id IS NOT NULL GROUP BY i.out_id) x
    ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_return',
       SUM(ABS(COALESCE(m.total_count,0) - COALESCE(x.count_sum,0)) > @v90_verify_tolerance)
  FROM erp_sale_return m
  LEFT JOIN (SELECT i.return_id AS master_id, SUM(COALESCE(i.count,0)) AS count_sum
               FROM erp_sale_return_items i WHERE i.deleted = 0 AND i.return_id IS NOT NULL GROUP BY i.return_id) x
    ON x.master_id = m.id
 WHERE m.deleted = 0;

-- ------------------------------------------------------------
-- J4. 出入库/退货单 order_id/order_no 反推核对
-- 口径依据：V90 §7.3（order_item_id → 订单明细 → 订单）。
-- 已知限界：一张出入库单若跨多张订单，V90 取 MIN(order_id) 属有损推断；
--           cross_order_rows > 0 时必须人工处理，不能当成 0 差异放过。
-- 期望：mismatch_rows = 0；cross_order_rows = 0；unresolvable_rows = 0。
-- ------------------------------------------------------------
SELECT 'erp_purchase_in' AS master_table,
       SUM(m.order_id IS NOT NULL AND x.master_id IS NOT NULL
           AND (m.order_id <> x.order_id OR m.order_no IS NULL OR m.order_no <> x.order_no)) AS mismatch_rows,
       (SELECT COUNT(*) FROM (
            SELECT i.in_id FROM erp_purchase_in_items i
              JOIN erp_purchase_order_items o ON o.id = i.order_item_id AND o.deleted = 0
             WHERE i.deleted = 0 AND i.in_id IS NOT NULL
             GROUP BY i.in_id HAVING COUNT(DISTINCT o.order_id) > 1) t) AS cross_order_rows,
       SUM(x.master_id IS NULL
           AND EXISTS (SELECT 1 FROM erp_purchase_in_items i WHERE i.in_id = m.id AND i.deleted = 0)) AS unresolvable_rows
  FROM erp_purchase_in m
  LEFT JOIN (
      SELECT i.in_id AS master_id, MIN(o.order_id) AS order_id, MIN(po.no) AS order_no
        FROM erp_purchase_in_items i
        JOIN erp_purchase_order_items o ON o.id = i.order_item_id
        JOIN erp_purchase_order po      ON po.id = o.order_id
       WHERE i.deleted = 0 AND o.deleted = 0 AND i.in_id IS NOT NULL
       GROUP BY i.in_id) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_out',
       SUM(m.order_id IS NOT NULL AND x.master_id IS NOT NULL
           AND (m.order_id <> x.order_id OR m.order_no IS NULL OR m.order_no <> x.order_no)),
       (SELECT COUNT(*) FROM (
            SELECT i.out_id FROM erp_sale_out_items i
              JOIN erp_sale_order_items o ON o.id = i.order_item_id AND o.deleted = 0
             WHERE i.deleted = 0 AND i.out_id IS NOT NULL
             GROUP BY i.out_id HAVING COUNT(DISTINCT o.order_id) > 1) t),
       SUM(x.master_id IS NULL
           AND EXISTS (SELECT 1 FROM erp_sale_out_items i WHERE i.out_id = m.id AND i.deleted = 0))
  FROM erp_sale_out m
  LEFT JOIN (
      SELECT i.out_id AS master_id, MIN(o.order_id) AS order_id, MIN(so.no) AS order_no
        FROM erp_sale_out_items i
        JOIN erp_sale_order_items o ON o.id = i.order_item_id
        JOIN erp_sale_order so      ON so.id = o.order_id
       WHERE i.deleted = 0 AND o.deleted = 0 AND i.out_id IS NOT NULL
       GROUP BY i.out_id) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_purchase_return',
       SUM(m.order_id IS NOT NULL AND x.master_id IS NOT NULL
           AND (m.order_id <> x.order_id OR m.order_no IS NULL OR m.order_no <> x.order_no)),
       (SELECT COUNT(*) FROM (
            SELECT i.return_id FROM erp_purchase_return_items i
              JOIN erp_purchase_order_items o ON o.id = i.order_item_id AND o.deleted = 0
             WHERE i.deleted = 0 AND i.return_id IS NOT NULL
             GROUP BY i.return_id HAVING COUNT(DISTINCT o.order_id) > 1) t),
       SUM(x.master_id IS NULL
           AND EXISTS (SELECT 1 FROM erp_purchase_return_items i WHERE i.return_id = m.id AND i.deleted = 0))
  FROM erp_purchase_return m
  LEFT JOIN (
      SELECT i.return_id AS master_id, MIN(o.order_id) AS order_id, MIN(po.no) AS order_no
        FROM erp_purchase_return_items i
        JOIN erp_purchase_order_items o ON o.id = i.order_item_id
        JOIN erp_purchase_order po      ON po.id = o.order_id
       WHERE i.deleted = 0 AND o.deleted = 0 AND i.return_id IS NOT NULL
       GROUP BY i.return_id) x ON x.master_id = m.id
 WHERE m.deleted = 0
UNION ALL
SELECT 'erp_sale_return',
       SUM(m.order_id IS NOT NULL AND x.master_id IS NOT NULL
           AND (m.order_id <> x.order_id OR m.order_no IS NULL OR m.order_no <> x.order_no)),
       (SELECT COUNT(*) FROM (
            SELECT i.return_id FROM erp_sale_return_items i
              JOIN erp_sale_order_items o ON o.id = i.order_item_id AND o.deleted = 0
             WHERE i.deleted = 0 AND i.return_id IS NOT NULL
             GROUP BY i.return_id HAVING COUNT(DISTINCT o.order_id) > 1) t),
       SUM(x.master_id IS NULL
           AND EXISTS (SELECT 1 FROM erp_sale_return_items i WHERE i.return_id = m.id AND i.deleted = 0))
  FROM erp_sale_return m
  LEFT JOIN (
      SELECT i.return_id AS master_id, MIN(o.order_id) AS order_id, MIN(so.no) AS order_no
        FROM erp_sale_return_items i
        JOIN erp_sale_order_items o ON o.id = i.order_item_id
        JOIN erp_sale_order so      ON so.id = o.order_id
       WHERE i.deleted = 0 AND o.deleted = 0 AND i.return_id IS NOT NULL
       GROUP BY i.return_id) x ON x.master_id = m.id
 WHERE m.deleted = 0;
-- ------------------------------------------------------------
-- J5. 单号唯一性：期望 0 行
-- 依据：V15 建有 uk_erp_{purchase,sale}_{order,in,out,return}_no_tenant(no, tenant_id)。
-- 注意：MySQL 唯一索引不约束 NULL，故另查 no IS NULL 或 no = '' 的行。
-- ------------------------------------------------------------
SELECT 'erp_purchase_order' AS master_table, tenant_id, no, COUNT(*) AS duplicate_rows
  FROM erp_purchase_order WHERE deleted = 0 GROUP BY tenant_id, no HAVING COUNT(*) > 1
UNION ALL
SELECT 'erp_purchase_in', tenant_id, no, COUNT(*) FROM erp_purchase_in WHERE deleted = 0 GROUP BY tenant_id, no HAVING COUNT(*) > 1
UNION ALL
SELECT 'erp_purchase_return', tenant_id, no, COUNT(*) FROM erp_purchase_return WHERE deleted = 0 GROUP BY tenant_id, no HAVING COUNT(*) > 1
UNION ALL
SELECT 'erp_sale_order', tenant_id, no, COUNT(*) FROM erp_sale_order WHERE deleted = 0 GROUP BY tenant_id, no HAVING COUNT(*) > 1
UNION ALL
SELECT 'erp_sale_out', tenant_id, no, COUNT(*) FROM erp_sale_out WHERE deleted = 0 GROUP BY tenant_id, no HAVING COUNT(*) > 1
UNION ALL
SELECT 'erp_sale_return', tenant_id, no, COUNT(*) FROM erp_sale_return WHERE deleted = 0 GROUP BY tenant_id, no HAVING COUNT(*) > 1
 ORDER BY master_table, tenant_id, no;

-- J5b. 空/ NULL 单号：期望 0 行（唯一索引对 NULL 不去重，空单号是隐藏风险）
SELECT 'erp_purchase_order' AS master_table, SUM(no IS NULL OR no = '') AS null_or_empty_no_rows FROM erp_purchase_order WHERE deleted = 0
UNION ALL SELECT 'erp_purchase_in',        SUM(no IS NULL OR no = '') FROM erp_purchase_in        WHERE deleted = 0
UNION ALL SELECT 'erp_purchase_return',    SUM(no IS NULL OR no = '') FROM erp_purchase_return    WHERE deleted = 0
UNION ALL SELECT 'erp_sale_order',         SUM(no IS NULL OR no = '') FROM erp_sale_order         WHERE deleted = 0
UNION ALL SELECT 'erp_sale_out',           SUM(no IS NULL OR no = '') FROM erp_sale_out           WHERE deleted = 0
UNION ALL SELECT 'erp_sale_return',        SUM(no IS NULL OR no = '') FROM erp_sale_return        WHERE deleted = 0;

-- ------------------------------------------------------------
-- J6. 孤儿指向：期望每项 0 行
-- 判定口径：引用目标不存在，或目标 deleted <> 0（软删除后仍被引用同样要修）。
-- ------------------------------------------------------------
SELECT 'erp_purchase_in.order_id -> erp_purchase_order' AS check_name, COUNT(*) AS orphan_rows
  FROM erp_purchase_in m LEFT JOIN erp_purchase_order r ON r.id = m.order_id
 WHERE m.deleted = 0 AND m.order_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_out.order_id -> erp_sale_order', COUNT(*)
  FROM erp_sale_out m LEFT JOIN erp_sale_order r ON r.id = m.order_id
 WHERE m.deleted = 0 AND m.order_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_return.order_id -> erp_purchase_order', COUNT(*)
  FROM erp_purchase_return m LEFT JOIN erp_purchase_order r ON r.id = m.order_id
 WHERE m.deleted = 0 AND m.order_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_return.order_id -> erp_sale_order', COUNT(*)
  FROM erp_sale_return m LEFT JOIN erp_sale_order r ON r.id = m.order_id
 WHERE m.deleted = 0 AND m.order_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_order_items.order_id -> erp_purchase_order', COUNT(*)
  FROM erp_purchase_order_items i LEFT JOIN erp_purchase_order r ON r.id = i.order_id
 WHERE i.deleted = 0 AND i.order_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_in_items.in_id -> erp_purchase_in', COUNT(*)
  FROM erp_purchase_in_items i LEFT JOIN erp_purchase_in r ON r.id = i.in_id
 WHERE i.deleted = 0 AND i.in_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_return_items.return_id -> erp_purchase_return', COUNT(*)
  FROM erp_purchase_return_items i LEFT JOIN erp_purchase_return r ON r.id = i.return_id
 WHERE i.deleted = 0 AND i.return_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_order_items.order_id -> erp_sale_order', COUNT(*)
  FROM erp_sale_order_items i LEFT JOIN erp_sale_order r ON r.id = i.order_id
 WHERE i.deleted = 0 AND i.order_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_out_items.out_id -> erp_sale_out', COUNT(*)
  FROM erp_sale_out_items i LEFT JOIN erp_sale_out r ON r.id = i.out_id
 WHERE i.deleted = 0 AND i.out_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_return_items.return_id -> erp_sale_return', COUNT(*)
  FROM erp_sale_return_items i LEFT JOIN erp_sale_return r ON r.id = i.return_id
 WHERE i.deleted = 0 AND i.return_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_in_items.order_item_id -> erp_purchase_order_items', COUNT(*)
  FROM erp_purchase_in_items i LEFT JOIN erp_purchase_order_items r ON r.id = i.order_item_id
 WHERE i.deleted = 0 AND i.order_item_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_purchase_return_items.order_item_id -> erp_purchase_order_items', COUNT(*)
  FROM erp_purchase_return_items i LEFT JOIN erp_purchase_order_items r ON r.id = i.order_item_id
 WHERE i.deleted = 0 AND i.order_item_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_out_items.order_item_id -> erp_sale_order_items', COUNT(*)
  FROM erp_sale_out_items i LEFT JOIN erp_sale_order_items r ON r.id = i.order_item_id
 WHERE i.deleted = 0 AND i.order_item_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0)
UNION ALL
SELECT 'erp_sale_return_items.order_item_id -> erp_sale_order_items', COUNT(*)
  FROM erp_sale_return_items i LEFT JOIN erp_sale_order_items r ON r.id = i.order_item_id
 WHERE i.deleted = 0 AND i.order_item_id IS NOT NULL AND (r.id IS NULL OR r.deleted <> 0);

-- ------------------------------------------------------------
-- J7. V90 §8 新增的 11 个索引：期望 11 行且 index_exists = 1、
--     actual_columns 与 expected_column 一致。
-- ------------------------------------------------------------
WITH expected (table_name, index_name, expected_column) AS (
    SELECT 'erp_sale_order',     'idx_erp_sale_order_order_time',       'order_time'     UNION ALL
    SELECT 'erp_sale_order',     'idx_erp_sale_order_account_id',       'account_id'     UNION ALL
    SELECT 'erp_sale_order',     'idx_erp_sale_order_sale_user_id',     'sale_user_id'   UNION ALL
    SELECT 'erp_purchase_order', 'idx_erp_purchase_order_order_time',   'order_time'     UNION ALL
    SELECT 'erp_purchase_order', 'idx_erp_purchase_order_account_id',   'account_id'     UNION ALL
    SELECT 'erp_purchase_in',    'idx_erp_purchase_in_order_id',        'order_id'       UNION ALL
    SELECT 'erp_purchase_in',    'idx_erp_purchase_in_account_id',      'account_id'     UNION ALL
    SELECT 'erp_sale_out',       'idx_erp_sale_out_order_id',           'order_id'       UNION ALL
    SELECT 'erp_sale_out',       'idx_erp_sale_out_account_id',         'account_id'     UNION ALL
    SELECT 'erp_purchase_return','idx_erp_purchase_return_order_id',    'order_id'       UNION ALL
    SELECT 'erp_sale_return',    'idx_erp_sale_return_order_id',        'order_id'
)
SELECT e.table_name, e.index_name, e.expected_column,
       SUM(s.INDEX_NAME IS NOT NULL) AS index_exists,
       GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) AS actual_columns
  FROM expected e
  LEFT JOIN information_schema.statistics s
         ON s.TABLE_SCHEMA = DATABASE()
        AND s.TABLE_NAME = e.table_name
        AND s.INDEX_NAME = e.index_name
 GROUP BY e.table_name, e.index_name, e.expected_column
 ORDER BY e.table_name, e.index_name;

-- ------------------------------------------------------------
-- J8. V15 索引名与列名不一致的 12 处历史债务（只读证据，期望 actual_columns = master_id）
--     收敛动作见 03-目标架构与升级方案.md：确认无依赖后 ALTER TABLE ... RENAME INDEX。
-- ------------------------------------------------------------
SELECT s.TABLE_NAME, s.INDEX_NAME,
       GROUP_CONCAT(s.COLUMN_NAME ORDER BY s.SEQ_IN_INDEX) AS actual_columns
  FROM information_schema.statistics s
 WHERE s.TABLE_SCHEMA = DATABASE()
   AND s.INDEX_NAME IN ('idx_erp_stock_in_item_order_id', 'idx_erp_stock_out_item_order_id',
                        'idx_erp_stock_move_item_order_id', 'idx_erp_stock_check_item_order_id',
                        'idx_erp_purchase_order_items_order_id', 'idx_erp_purchase_in_items_order_id',
                        'idx_erp_purchase_return_items_order_id', 'idx_erp_sale_order_items_order_id',
                        'idx_erp_sale_out_items_order_id', 'idx_erp_sale_return_items_order_id',
                        'idx_erp_finance_payment_item_order_id', 'idx_erp_finance_receipt_item_order_id')
 GROUP BY s.TABLE_NAME, s.INDEX_NAME
 ORDER BY s.TABLE_NAME;

-- ------------------------------------------------------------
-- J9. sql_mode：确认 STRICT_TRANS_TABLES 生效（V91 放宽列之前的阻断前提）
-- ------------------------------------------------------------
SELECT @@GLOBAL.sql_mode AS global_sql_mode, @@SESSION.sql_mode AS session_sql_mode;

-- ============================================================
-- V90: 补齐 ERP 采购/销售 6 张单据主表缺失的 71 个 DO 列
--
-- 背景（静态核验，2026-09-24）：
--   zhicloud-module-erp 的 DO / Service / Mapper / Controller 与上游 yudao
--   (YunaiV/ruoyi-vue-pro, MIT) 的 erp 模块字段契约一致，但本项目自带 DDL
--   （zhicloud-server/.../V10__erp_ddl.sql 与 sql/mysql/erp.sql）是另行手写的，
--   与 DO 字段集合不一致：
--     * DDL 独有（孤儿列）：out_time / in_time / total_amount /
--       payment_status / receipt_status / refund_status
--     * DO 独有（缺列）：见下方 71 个 CALL
--   影响：MyBatis-Plus 的 selectById/selectList/selectPage 的 SELECT 列表由 DO
--   字段推导，命中缺列即抛 SQLException: Unknown column，**与查询条件无关**，
--   因此单据页的按 ID 读取、列表分页、新建/编辑提交都会失败。
--   注意：V82__fix_do_column_mappings.sql 的 275 个 p_v80_add_column 只覆盖了
--   `*_items` 明细表，未覆盖本脚本的 6 张主表。
--
-- 策略（与 V82 保持一致）：
--   1) 以 Java DO 为真相源，幂等补齐缺失列（不改名、不删旧列）；
--   2) 旧列 total_amount → 新列 total_product_price/total_price 做保守回填；
--   3) 明细语义列汇总回填主表的 in_count/out_count/return_count；
--   4) erp_purchase_in/erp_sale_out 的 order_id/order_no 由明细 order_item_id 反推；
--   5) payment_price/receipt_price 由 erp_finance_*_item 按 biz_type+biz_id 汇总。
--   所有回填都是 `WHERE <col> IS NULL` 守卫，可重复执行。
--
-- 类型约定沿用 V82：金额/数量/百分比 DECIMAL(20,4)，时间 DATETIME，
-- 外键 BIGINT，单号 VARCHAR(64)。
-- file_url 用 VARCHAR(512)（V82 里 erp_stock_* 用的是 255；这里考虑单个 OSS
-- 预签名 URL 常超 255，故放宽，属 [判断] 项，可按需改回 255）。
--
-- 限界：本脚本无法在本地真库执行验证（本机无 JDK/Maven/MySQL）。
--   上线前请务必在预发布库执行 `information_schema` 复核（见 README）。
-- ============================================================

DROP PROCEDURE IF EXISTS p_v90_add_column;
DELIMITER $$
CREATE PROCEDURE p_v90_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
        SET @v90_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE v90_stmt FROM @v90_sql;
        EXECUTE v90_stmt;
        DEALLOCATE PREPARE v90_stmt;
    END IF;
END$$
DELIMITER ;
-- ============================================================
-- 1. erp_sale_order（12 列）—— 对应 ErpSaleOrderDO
-- ============================================================
CALL p_v90_add_column('erp_sale_order', 'account_id',        'BIGINT DEFAULT NULL COMMENT ''账户 ID（关联 erp_account.id）''');
CALL p_v90_add_column('erp_sale_order', 'sale_user_id',      'BIGINT DEFAULT NULL COMMENT ''销售员 ID（关联 system_users.id）''');
CALL p_v90_add_column('erp_sale_order', 'order_time',        'DATETIME DEFAULT NULL COMMENT ''下单时间''');
CALL p_v90_add_column('erp_sale_order', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''商品原价合计（不含税）''');
CALL p_v90_add_column('erp_sale_order', 'total_tax_price',   'DECIMAL(20,4) DEFAULT NULL COMMENT ''税费合计''');
CALL p_v90_add_column('erp_sale_order', 'total_price',       'DECIMAL(20,4) DEFAULT NULL COMMENT ''合计价格（不含税合计+税费-优惠）''');
CALL p_v90_add_column('erp_sale_order', 'discount_percent',  'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠百分比''');
CALL p_v90_add_column('erp_sale_order', 'discount_price',    'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠金额''');
CALL p_v90_add_column('erp_sale_order', 'deposit_price',     'DECIMAL(20,4) DEFAULT NULL COMMENT ''订金''');
CALL p_v90_add_column('erp_sale_order', 'out_count',         'DECIMAL(20,4) DEFAULT 0 COMMENT ''已出库数量''');
CALL p_v90_add_column('erp_sale_order', 'return_count',      'DECIMAL(20,4) DEFAULT 0 COMMENT ''已退货数量''');
CALL p_v90_add_column('erp_sale_order', 'file_url',          'VARCHAR(512) DEFAULT NULL COMMENT ''附件 URL（多个逗号分隔）''');

-- ============================================================
-- 2. erp_purchase_order（11 列）—— 对应 ErpPurchaseOrderDO
-- ============================================================
CALL p_v90_add_column('erp_purchase_order', 'account_id',        'BIGINT DEFAULT NULL COMMENT ''账户 ID（关联 erp_account.id）''');
CALL p_v90_add_column('erp_purchase_order', 'order_time',        'DATETIME DEFAULT NULL COMMENT ''下单时间''');
CALL p_v90_add_column('erp_purchase_order', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''商品原价合计（不含税）''');
CALL p_v90_add_column('erp_purchase_order', 'total_tax_price',   'DECIMAL(20,4) DEFAULT NULL COMMENT ''税费合计''');
CALL p_v90_add_column('erp_purchase_order', 'total_price',       'DECIMAL(20,4) DEFAULT NULL COMMENT ''合计价格（不含税合计+税费-优惠）''');
CALL p_v90_add_column('erp_purchase_order', 'discount_percent',  'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠百分比''');
CALL p_v90_add_column('erp_purchase_order', 'discount_price',    'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠金额''');
CALL p_v90_add_column('erp_purchase_order', 'deposit_price',     'DECIMAL(20,4) DEFAULT NULL COMMENT ''订金''');
CALL p_v90_add_column('erp_purchase_order', 'in_count',          'DECIMAL(20,4) DEFAULT 0 COMMENT ''已入库数量''');
CALL p_v90_add_column('erp_purchase_order', 'return_count',      'DECIMAL(20,4) DEFAULT 0 COMMENT ''已退货数量''');
CALL p_v90_add_column('erp_purchase_order', 'file_url',          'VARCHAR(512) DEFAULT NULL COMMENT ''附件 URL（多个逗号分隔）''');

-- ============================================================
-- 3. erp_purchase_in（11 列）—— 对应 ErpPurchaseInDO
-- ============================================================
CALL p_v90_add_column('erp_purchase_in', 'account_id',         'BIGINT DEFAULT NULL COMMENT ''账户 ID（关联 erp_account.id）''');
CALL p_v90_add_column('erp_purchase_in', 'order_id',           'BIGINT DEFAULT NULL COMMENT ''采购订单 ID（关联 erp_purchase_order.id）''');
CALL p_v90_add_column('erp_purchase_in', 'order_no',           'VARCHAR(64) DEFAULT NULL COMMENT ''采购订单号''');
CALL p_v90_add_column('erp_purchase_in', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''商品原价合计（不含税）''');
CALL p_v90_add_column('erp_purchase_in', 'total_tax_price',    'DECIMAL(20,4) DEFAULT NULL COMMENT ''税费合计''');
CALL p_v90_add_column('erp_purchase_in', 'total_price',        'DECIMAL(20,4) DEFAULT NULL COMMENT ''合计价格（不含税合计+税费-优惠+其它费用）''');
CALL p_v90_add_column('erp_purchase_in', 'discount_percent',   'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠百分比''');
CALL p_v90_add_column('erp_purchase_in', 'discount_price',     'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠金额''');
CALL p_v90_add_column('erp_purchase_in', 'other_price',        'DECIMAL(20,4) DEFAULT NULL COMMENT ''其它费用''');
CALL p_v90_add_column('erp_purchase_in', 'payment_price',      'DECIMAL(20,4) DEFAULT NULL COMMENT ''已付款金额''');
CALL p_v90_add_column('erp_purchase_in', 'file_url',           'VARCHAR(512) DEFAULT NULL COMMENT ''附件 URL（多个逗号分隔）''');

-- ============================================================
-- 4. erp_sale_out（12 列）—— 对应 ErpSaleOutDO
-- ============================================================
CALL p_v90_add_column('erp_sale_out', 'account_id',          'BIGINT DEFAULT NULL COMMENT ''账户 ID（关联 erp_account.id）''');
CALL p_v90_add_column('erp_sale_out', 'sale_user_id',        'BIGINT DEFAULT NULL COMMENT ''销售员 ID（关联 system_users.id）''');
CALL p_v90_add_column('erp_sale_out', 'order_id',            'BIGINT DEFAULT NULL COMMENT ''销售订单 ID（关联 erp_sale_order.id）''');
CALL p_v90_add_column('erp_sale_out', 'order_no',            'VARCHAR(64) DEFAULT NULL COMMENT ''销售订单号''');
CALL p_v90_add_column('erp_sale_out', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''商品原价合计（不含税）''');
CALL p_v90_add_column('erp_sale_out', 'total_tax_price',     'DECIMAL(20,4) DEFAULT NULL COMMENT ''税费合计''');
CALL p_v90_add_column('erp_sale_out', 'total_price',         'DECIMAL(20,4) DEFAULT NULL COMMENT ''合计价格（不含税合计+税费-优惠+其它费用）''');
CALL p_v90_add_column('erp_sale_out', 'discount_percent',    'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠百分比''');
CALL p_v90_add_column('erp_sale_out', 'discount_price',      'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠金额''');
CALL p_v90_add_column('erp_sale_out', 'other_price',         'DECIMAL(20,4) DEFAULT NULL COMMENT ''其它费用''');
CALL p_v90_add_column('erp_sale_out', 'receipt_price',       'DECIMAL(20,4) DEFAULT NULL COMMENT ''已收款金额''');
CALL p_v90_add_column('erp_sale_out', 'file_url',            'VARCHAR(512) DEFAULT NULL COMMENT ''附件 URL（多个逗号分隔）''');

-- ============================================================
-- 5. erp_purchase_return（12 列）—— 对应 ErpPurchaseReturnDO
-- ============================================================
CALL p_v90_add_column('erp_purchase_return', 'account_id',          'BIGINT DEFAULT NULL COMMENT ''账户 ID（关联 erp_account.id）''');
CALL p_v90_add_column('erp_purchase_return', 'order_id',            'BIGINT DEFAULT NULL COMMENT ''采购订单 ID（关联 erp_purchase_order.id）''');
CALL p_v90_add_column('erp_purchase_return', 'order_no',            'VARCHAR(64) DEFAULT NULL COMMENT ''采购订单号''');
CALL p_v90_add_column('erp_purchase_return', 'return_time',         'DATETIME DEFAULT NULL COMMENT ''退货时间''');
CALL p_v90_add_column('erp_purchase_return', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''商品原价合计（不含税）''');
CALL p_v90_add_column('erp_purchase_return', 'total_tax_price',     'DECIMAL(20,4) DEFAULT NULL COMMENT ''税费合计''');
CALL p_v90_add_column('erp_purchase_return', 'total_price',         'DECIMAL(20,4) DEFAULT NULL COMMENT ''合计价格（不含税合计+税费-优惠+其它费用）''');
CALL p_v90_add_column('erp_purchase_return', 'discount_percent',    'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠百分比''');
CALL p_v90_add_column('erp_purchase_return', 'discount_price',      'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠金额''');
CALL p_v90_add_column('erp_purchase_return', 'other_price',         'DECIMAL(20,4) DEFAULT NULL COMMENT ''其它费用''');
CALL p_v90_add_column('erp_purchase_return', 'refund_price',        'DECIMAL(20,4) DEFAULT NULL COMMENT ''已退款金额''');
CALL p_v90_add_column('erp_purchase_return', 'file_url',            'VARCHAR(512) DEFAULT NULL COMMENT ''附件 URL（多个逗号分隔）''');

-- ============================================================
-- 6. erp_sale_return（13 列）—— 对应 ErpSaleReturnDO
-- ============================================================
CALL p_v90_add_column('erp_sale_return', 'account_id',          'BIGINT DEFAULT NULL COMMENT ''账户 ID（关联 erp_account.id）''');
CALL p_v90_add_column('erp_sale_return', 'sale_user_id',        'BIGINT DEFAULT NULL COMMENT ''销售员 ID（关联 system_users.id）''');
CALL p_v90_add_column('erp_sale_return', 'order_id',            'BIGINT DEFAULT NULL COMMENT ''销售订单 ID（关联 erp_sale_order.id）''');
CALL p_v90_add_column('erp_sale_return', 'order_no',            'VARCHAR(64) DEFAULT NULL COMMENT ''销售订单号''');
CALL p_v90_add_column('erp_sale_return', 'return_time',         'DATETIME DEFAULT NULL COMMENT ''退货时间''');
CALL p_v90_add_column('erp_sale_return', 'total_product_price', 'DECIMAL(20,4) DEFAULT NULL COMMENT ''商品原价合计（不含税）''');
CALL p_v90_add_column('erp_sale_return', 'total_tax_price',     'DECIMAL(20,4) DEFAULT NULL COMMENT ''税费合计''');
CALL p_v90_add_column('erp_sale_return', 'total_price',         'DECIMAL(20,4) DEFAULT NULL COMMENT ''合计价格（不含税合计+税费-优惠+其它费用）''');
CALL p_v90_add_column('erp_sale_return', 'discount_percent',    'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠百分比''');
CALL p_v90_add_column('erp_sale_return', 'discount_price',      'DECIMAL(20,4) DEFAULT NULL COMMENT ''优惠金额''');
CALL p_v90_add_column('erp_sale_return', 'other_price',         'DECIMAL(20,4) DEFAULT NULL COMMENT ''其它费用''');
CALL p_v90_add_column('erp_sale_return', 'refund_price',        'DECIMAL(20,4) DEFAULT NULL COMMENT ''已退款金额''');
CALL p_v90_add_column('erp_sale_return', 'file_url',            'VARCHAR(512) DEFAULT NULL COMMENT ''附件 URL（多个逗号分隔）''');

-- 合计 12 + 11 + 11 + 12 + 12 + 13 = 71 列
-- ============================================================
-- 7. 数据回填（全部幂等，可重复执行）
--
-- 原则：只在目标列为 NULL 时写入，绝不覆盖已有业务数据；旧列一律保留
--      （不 DROP、不改名），以支持回滚与"双读"兼容期。
--      列名双写债务的收敛路线见
--      docs/enterprise-platform/03-目标架构与升级方案.md。
--
-- 依赖：本节依赖 V82 已执行（明细表的 order_id / order_item_id /
--      in_count / return_count / tax_price 等列由 V82 补齐），
--      Flyway 版本序 V82 < V90 保证这一点。
-- ============================================================

-- ------------------------------------------------------------
-- 7.1 旧列 → DO 列（拷贝型，缺列自动跳过）
--
-- 为什么用存储过程：旧列（total_amount / in_time / out_time）在"手工建库"
-- 路径（sql/mysql/erp.sql）与"Flyway"路径下都存在，但不排除后续有人先把旧列
-- 删了再跑本脚本；静态 UPDATE 一旦命中不存在的列会让整个迁移 Job 失败并阻断
-- 发布，故这里统一走 information_schema 判断 + 动态 SQL。
-- ------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_v90_copy_legacy;
DELIMITER $$
CREATE PROCEDURE p_v90_copy_legacy(IN p_table VARCHAR(64), IN p_new_col VARCHAR(64), IN p_legacy_col VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_new_col)
       AND EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_legacy_col) THEN
        SET @v90_sql = CONCAT('UPDATE `', p_table, '` SET `', p_new_col, '` = `', p_legacy_col,
                              '` WHERE `', p_new_col, '` IS NULL AND `', p_legacy_col, '` IS NOT NULL');
        PREPARE v90_stmt FROM @v90_sql;
        EXECUTE v90_stmt;
        DEALLOCATE PREPARE v90_stmt;
    END IF;
END$$
DELIMITER ;
-- 7.1.1 总金额：total_amount（旧，单号级总金额）→ total_product_price / total_price
--   [判断] 旧列 total_amount 未区分"含税/不含税"，这里把同一个值同时写入
--   "不含税商品合计"与"合计价格"两列作为初始值。数量级正确，但税额拆分不准，
--   上线后应由业务按真实税额重新校准（见 07-风险与未验证假设.md）。
CALL p_v90_copy_legacy('erp_purchase_order',  'total_product_price', 'total_amount');
CALL p_v90_copy_legacy('erp_purchase_order',  'total_price',         'total_amount');
CALL p_v90_copy_legacy('erp_purchase_in',     'total_product_price', 'total_amount');
CALL p_v90_copy_legacy('erp_purchase_in',     'total_price',         'total_amount');
CALL p_v90_copy_legacy('erp_purchase_return', 'total_product_price', 'total_amount');
CALL p_v90_copy_legacy('erp_purchase_return', 'total_price',         'total_amount');
CALL p_v90_copy_legacy('erp_sale_order',      'total_product_price', 'total_amount');
CALL p_v90_copy_legacy('erp_sale_order',      'total_price',         'total_amount');
CALL p_v90_copy_legacy('erp_sale_out',        'total_product_price', 'total_amount');
CALL p_v90_copy_legacy('erp_sale_out',        'total_price',         'total_amount');
CALL p_v90_copy_legacy('erp_sale_return',     'total_product_price', 'total_amount');
CALL p_v90_copy_legacy('erp_sale_return',     'total_price',         'total_amount');

-- 7.1.2 时间列语义映射
--   （a）等价映射：DDL 注释本身写的就是"退货时间"，与 DO 的 return_time 同义。
CALL p_v90_copy_legacy('erp_purchase_return', 'return_time', 'in_time');
CALL p_v90_copy_legacy('erp_sale_return',     'return_time', 'out_time');
--   （b）[判断] 非等价映射（务必人工确认后再执行）：
--   V10 DDL 里 erp_purchase_order.in_time 的注释是"预计入库时间"、
--   erp_sale_order.out_time 的注释是"预计出库时间"，而 DO 的 order_time 是
--   "下单时间"，两者语义不同。此处仅因为存量数据里 order_time 之外没有更接近
--   下单时间的字段才做兜底拷贝；若业务上"预计到货日 ≠ 下单日"，
--   请删除下面两行，改为由业务导入真实的 order_time。
CALL p_v90_copy_legacy('erp_purchase_order', 'order_time', 'in_time');
CALL p_v90_copy_legacy('erp_sale_order',     'order_time', 'out_time');
-- ------------------------------------------------------------
-- 7.2 明细汇总 → 主表已入库/已出库/已退货数量
--
-- 语义来源（[已核验]）：
--   zhicloud-ui-admin-vue3/.../erp/sale/return/SaleReturnForm.vue:294
--       item.count = (item.outCount ?? 0) - (item.returnCount ?? 0)
--   zhicloud-ui-admin-vue3/.../erp/purchase/return/PurchaseReturnForm.vue:282
--       item.count = (item.inCount ?? 0) - (item.returnCount ?? 0)
--   即明细行的 *_count 直接决定前端"可退货数量"，主表的 *_count 决定
--   "可出库/可入库订单列表"是否出现（见各 Mapper 的 apply(...) 条件）。
-- ------------------------------------------------------------
UPDATE erp_purchase_order m
JOIN (
    SELECT i.order_id AS order_id,
           SUM(COALESCE(i.in_count, 0))     AS in_count_sum,
           SUM(COALESCE(i.return_count, 0)) AS return_count_sum
    FROM erp_purchase_order_items i
    WHERE i.deleted = 0 AND i.order_id IS NOT NULL
    GROUP BY i.order_id
) x ON x.order_id = m.id
SET m.in_count     = COALESCE(m.in_count, x.in_count_sum),
    m.return_count = COALESCE(m.return_count, x.return_count_sum);

UPDATE erp_sale_order m
JOIN (
    SELECT i.order_id AS order_id,
           SUM(COALESCE(i.out_count, 0))    AS out_count_sum,
           SUM(COALESCE(i.return_count, 0)) AS return_count_sum
    FROM erp_sale_order_items i
    WHERE i.deleted = 0 AND i.order_id IS NOT NULL
    GROUP BY i.order_id
) x ON x.order_id = m.id
SET m.out_count    = COALESCE(m.out_count, x.out_count_sum),
    m.return_count = COALESCE(m.return_count, x.return_count_sum);

-- 明细行自身的 NULL → 0（V82 新增列默认 NULL，会让前端 ?? 0 与 SUM 语义不一致）
UPDATE erp_purchase_order_items SET in_count = 0     WHERE in_count IS NULL;
UPDATE erp_purchase_order_items SET return_count = 0 WHERE return_count IS NULL;
UPDATE erp_sale_order_items     SET out_count = 0    WHERE out_count IS NULL;
UPDATE erp_sale_order_items     SET return_count = 0 WHERE return_count IS NULL;
-- ------------------------------------------------------------
-- 7.3 入库单/出库单/退货单 → 订单号反推（order_id / order_no）
--
-- 语义来源（[已核验]）：
--   SaleReturnForm.vue:295 / PurchaseReturnForm.vue:283 均为
--   `item.orderItemId = item.id`，其中 item 来自被选中的订单明细，
--   即 *_items.order_item_id 指向"订单明细 id"（不是出入库明细 id）。
--   入库单/出库单同理（PurchaseInForm.vue:280 / SaleOutForm.vue:297）。
--   因此 order_id 只能经由 order_item_id → 订单明细 → 订单 反推。
--
-- 限界：一张出入库单若跨多张订单（V10 DDL 未禁止），这里取 MIN() 作为主订单，
--   属于有损推断，需在核对报表中单独排查（见 08 号自检 SQL 的第 3 段）。
-- ------------------------------------------------------------
UPDATE erp_purchase_in m
JOIN (
    SELECT i.in_id AS master_id, MIN(o.order_id) AS order_id, MIN(po.no) AS order_no
    FROM erp_purchase_in_items i
    JOIN erp_purchase_order_items o ON o.id = i.order_item_id
    JOIN erp_purchase_order po      ON po.id = o.order_id
    WHERE i.deleted = 0 AND o.deleted = 0 AND i.in_id IS NOT NULL AND i.order_item_id IS NOT NULL
    GROUP BY i.in_id
) x ON x.master_id = m.id
SET m.order_id = COALESCE(m.order_id, x.order_id),
    m.order_no = COALESCE(m.order_no, x.order_no);

UPDATE erp_sale_out m
JOIN (
    SELECT i.out_id AS master_id, MIN(o.order_id) AS order_id, MIN(so.no) AS order_no
    FROM erp_sale_out_items i
    JOIN erp_sale_order_items o ON o.id = i.order_item_id
    JOIN erp_sale_order so      ON so.id = o.order_id
    WHERE i.deleted = 0 AND o.deleted = 0 AND i.out_id IS NOT NULL AND i.order_item_id IS NOT NULL
    GROUP BY i.out_id
) x ON x.master_id = m.id
SET m.order_id = COALESCE(m.order_id, x.order_id),
    m.order_no = COALESCE(m.order_no, x.order_no);

UPDATE erp_purchase_return m
JOIN (
    SELECT i.return_id AS master_id, MIN(o.order_id) AS order_id, MIN(po.no) AS order_no
    FROM erp_purchase_return_items i
    JOIN erp_purchase_order_items o ON o.id = i.order_item_id
    JOIN erp_purchase_order po      ON po.id = o.order_id
    WHERE i.deleted = 0 AND o.deleted = 0 AND i.return_id IS NOT NULL AND i.order_item_id IS NOT NULL
    GROUP BY i.return_id
) x ON x.master_id = m.id
SET m.order_id = COALESCE(m.order_id, x.order_id),
    m.order_no = COALESCE(m.order_no, x.order_no);

UPDATE erp_sale_return m
JOIN (
    SELECT i.return_id AS master_id, MIN(o.order_id) AS order_id, MIN(so.no) AS order_no
    FROM erp_sale_return_items i
    JOIN erp_sale_order_items o ON o.id = i.order_item_id
    JOIN erp_sale_order so      ON so.id = o.order_id
    WHERE i.deleted = 0 AND o.deleted = 0 AND i.return_id IS NOT NULL AND i.order_item_id IS NOT NULL
    GROUP BY i.return_id
) x ON x.master_id = m.id
SET m.order_id = COALESCE(m.order_id, x.order_id),
    m.order_no = COALESCE(m.order_no, x.order_no);
-- ------------------------------------------------------------
-- 7.4 金额兜底（明细汇总）与财务单金额汇总
--
-- 7.4.1 订单主表金额：detail-first 兜底
--   口径来自 DO 注释：total_price = 不含税合计 + 税费 - 优惠 + 其它费用。
--   只在 7.1 之后仍为 NULL 时才用明细汇总补（即旧列 total_amount 也为空的行），
--   避免覆盖已有业务数据。
--   [判断] 主表 total_price 与明细合计不一致时不做自动纠正，只输出核对报表
--   （见 README 的"上线后 24 小时观察项"），以免静默改写业务金额。
-- ------------------------------------------------------------
UPDATE erp_purchase_order m
JOIN (
    SELECT i.order_id AS order_id,
           SUM(COALESCE(i.total_price, 0)) AS product_sum,
           SUM(COALESCE(i.tax_price, 0))   AS tax_sum
    FROM erp_purchase_order_items i
    WHERE i.deleted = 0 AND i.order_id IS NOT NULL
    GROUP BY i.order_id
) x ON x.order_id = m.id
SET m.total_product_price = COALESCE(m.total_product_price, x.product_sum),
    m.total_tax_price     = COALESCE(m.total_tax_price, x.tax_sum),
    m.total_price         = COALESCE(m.total_price, x.product_sum + x.tax_sum);

UPDATE erp_sale_order m
JOIN (
    SELECT i.order_id AS order_id,
           SUM(COALESCE(i.total_price, 0)) AS product_sum,
           SUM(COALESCE(i.tax_price, 0))   AS tax_sum
    FROM erp_sale_order_items i
    WHERE i.deleted = 0 AND i.order_id IS NOT NULL
    GROUP BY i.order_id
) x ON x.order_id = m.id
SET m.total_product_price = COALESCE(m.total_product_price, x.product_sum),
    m.total_tax_price     = COALESCE(m.total_tax_price, x.tax_sum),
    m.total_price         = COALESCE(m.total_price, x.product_sum + x.tax_sum);

-- 7.4.2 erp_purchase_in.payment_price ← 付款单明细（biz_type = 11 采购入库）
--   依据：ErpFinancePaymentServiceImpl:204 直接取 SUM(payment_price) 写入，
--         未取负 → 已付款金额存正数。
UPDATE erp_purchase_in m
JOIN (
    SELECT fi.biz_id AS biz_id, SUM(fi.payment_price) AS amt
    FROM erp_finance_payment_item fi
    WHERE fi.deleted = 0 AND fi.biz_type = 11 AND fi.biz_id IS NOT NULL
    GROUP BY fi.biz_id
) x ON x.biz_id = m.id
SET m.payment_price = COALESCE(m.payment_price, x.amt);

-- 7.4.3 erp_purchase_return.refund_price ← 付款单明细（biz_type = 12 采购退货）取负
--   依据：ErpFinancePaymentServiceImpl:206
--     updatePurchaseReturnRefundPrice(bizId, totalPaymentPrice.negate())
--   退款在付款单明细里记成负数，而 refund_price 存正数
--   （ErpPurchaseReturnMapper 的 refundStatus 判定：0 / (0,total_price) / =total_price）。
UPDATE erp_purchase_return m
JOIN (
    SELECT fi.biz_id AS biz_id, SUM(fi.payment_price) AS amt
    FROM erp_finance_payment_item fi
    WHERE fi.deleted = 0 AND fi.biz_type = 12 AND fi.biz_id IS NOT NULL
    GROUP BY fi.biz_id
) x ON x.biz_id = m.id
SET m.refund_price = COALESCE(m.refund_price, -x.amt);

-- 7.4.4 erp_sale_out.receipt_price ← 收款单明细（biz_type = 21 销售出库）
--   依据：ErpFinanceReceiptServiceImpl:204 直接取 SUM(receipt_price)。
UPDATE erp_sale_out m
JOIN (
    SELECT fi.biz_id AS biz_id, SUM(fi.receipt_price) AS amt
    FROM erp_finance_receipt_item fi
    WHERE fi.deleted = 0 AND fi.biz_type = 21 AND fi.biz_id IS NOT NULL
    GROUP BY fi.biz_id
) x ON x.biz_id = m.id
SET m.receipt_price = COALESCE(m.receipt_price, x.amt);

-- 7.4.5 erp_sale_return.refund_price ← 收款单明细（biz_type = 22 销售退货）取负
--   依据：ErpFinanceReceiptServiceImpl:206
--     updateSaleReturnRefundPrice(bizId, totalReceiptPrice.negate())
UPDATE erp_sale_return m
JOIN (
    SELECT fi.biz_id AS biz_id, SUM(fi.receipt_price) AS amt
    FROM erp_finance_receipt_item fi
    WHERE fi.deleted = 0 AND fi.biz_type = 22 AND fi.biz_id IS NOT NULL
    GROUP BY fi.biz_id
) x ON x.biz_id = m.id
SET m.refund_price = COALESCE(m.refund_price, -x.amt);
-- ------------------------------------------------------------
-- 7.5 零值兜底
--   仍为 NULL 的列统一置 0：MyBatis-Plus 的 apply("t.x < t.y") 在 NULL 下
--   结果为 UNKNOWN，会让"可出库/可退货/未付款"等筛选条件静默丢行。
--   带上 WHERE ... IS NULL，避免无谓的全表重写。
-- ------------------------------------------------------------
UPDATE erp_purchase_order SET
    total_product_price = COALESCE(total_product_price, 0),
    total_tax_price     = COALESCE(total_tax_price, 0),
    total_price         = COALESCE(total_price, 0),
    discount_percent    = COALESCE(discount_percent, 0),
    discount_price      = COALESCE(discount_price, 0),
    deposit_price       = COALESCE(deposit_price, 0),
    in_count            = COALESCE(in_count, 0),
    return_count        = COALESCE(return_count, 0)
WHERE total_product_price IS NULL OR total_tax_price IS NULL OR total_price IS NULL
   OR discount_percent IS NULL OR discount_price IS NULL OR deposit_price IS NULL
   OR in_count IS NULL OR return_count IS NULL;

UPDATE erp_sale_order SET
    total_product_price = COALESCE(total_product_price, 0),
    total_tax_price     = COALESCE(total_tax_price, 0),
    total_price         = COALESCE(total_price, 0),
    discount_percent    = COALESCE(discount_percent, 0),
    discount_price      = COALESCE(discount_price, 0),
    deposit_price       = COALESCE(deposit_price, 0),
    out_count           = COALESCE(out_count, 0),
    return_count        = COALESCE(return_count, 0)
WHERE total_product_price IS NULL OR total_tax_price IS NULL OR total_price IS NULL
   OR discount_percent IS NULL OR discount_price IS NULL OR deposit_price IS NULL
   OR out_count IS NULL OR return_count IS NULL;

UPDATE erp_purchase_in SET
    total_product_price = COALESCE(total_product_price, 0),
    total_tax_price     = COALESCE(total_tax_price, 0),
    total_price         = COALESCE(total_price, 0),
    discount_percent    = COALESCE(discount_percent, 0),
    discount_price      = COALESCE(discount_price, 0),
    other_price         = COALESCE(other_price, 0),
    payment_price       = COALESCE(payment_price, 0)
WHERE total_product_price IS NULL OR total_tax_price IS NULL OR total_price IS NULL
   OR discount_percent IS NULL OR discount_price IS NULL OR other_price IS NULL
   OR payment_price IS NULL;

UPDATE erp_sale_out SET
    total_product_price = COALESCE(total_product_price, 0),
    total_tax_price     = COALESCE(total_tax_price, 0),
    total_price         = COALESCE(total_price, 0),
    discount_percent    = COALESCE(discount_percent, 0),
    discount_price      = COALESCE(discount_price, 0),
    other_price         = COALESCE(other_price, 0),
    receipt_price       = COALESCE(receipt_price, 0)
WHERE total_product_price IS NULL OR total_tax_price IS NULL OR total_price IS NULL
   OR discount_percent IS NULL OR discount_price IS NULL OR other_price IS NULL
   OR receipt_price IS NULL;

UPDATE erp_purchase_return SET
    total_product_price = COALESCE(total_product_price, 0),
    total_tax_price     = COALESCE(total_tax_price, 0),
    total_price         = COALESCE(total_price, 0),
    discount_percent    = COALESCE(discount_percent, 0),
    discount_price      = COALESCE(discount_price, 0),
    other_price         = COALESCE(other_price, 0),
    refund_price        = COALESCE(refund_price, 0)
WHERE total_product_price IS NULL OR total_tax_price IS NULL OR total_price IS NULL
   OR discount_percent IS NULL OR discount_price IS NULL OR other_price IS NULL
   OR refund_price IS NULL;

UPDATE erp_sale_return SET
    total_product_price = COALESCE(total_product_price, 0),
    total_tax_price     = COALESCE(total_tax_price, 0),
    total_price         = COALESCE(total_price, 0),
    discount_percent    = COALESCE(discount_percent, 0),
    discount_price      = COALESCE(discount_price, 0),
    other_price         = COALESCE(other_price, 0),
    refund_price        = COALESCE(refund_price, 0)
WHERE total_product_price IS NULL OR total_tax_price IS NULL OR total_price IS NULL
   OR discount_percent IS NULL OR discount_price IS NULL OR other_price IS NULL
   OR refund_price IS NULL;

DROP PROCEDURE IF EXISTS p_v90_copy_legacy;
-- ============================================================
-- 8. 索引补充
--
-- 背景：7.x 的回填与单据列表都会按这些新列过滤/关联，无索引时在大表上退化为
--       全表扫描（V15 只给"迁移时已存在"的列建了索引，看不到新列）。
-- 命名沿用 V15 的 `idx_<table>_<column>` 约定；索引名已存在则跳过。
-- 注意：V15 给明细表建的 `idx_*_items_order_id` 实际建在旧列 `master_id` 上，
--       与新列 `order_id` 同名不同列。本脚本不重复建 items 索引，避免同名冲突；
--       该"索引名与列名不一致"的债务见 03-目标架构与升级方案.md。
-- ============================================================
DROP PROCEDURE IF EXISTS p_v90_add_index;
DELIMITER $$
CREATE PROCEDURE p_v90_add_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_cols VARCHAR(500))
BEGIN
    DECLARE CONTINUE HANDLER FOR 1072 BEGIN END;  -- 列不存在时静默跳过，避免迁移 Job 阻断发布
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index) THEN
        SET @v90_sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE v90_stmt FROM @v90_sql;
        EXECUTE v90_stmt;
        DEALLOCATE PREPARE v90_stmt;
    END IF;
END$$
DELIMITER ;

-- 8.1 订单主表：下单时间（列表默认按时间范围筛选）+ 账户 + 销售员
CALL p_v90_add_index('erp_sale_order',     'idx_erp_sale_order_order_time',     'order_time');
CALL p_v90_add_index('erp_sale_order',     'idx_erp_sale_order_account_id',     'account_id');
CALL p_v90_add_index('erp_sale_order',     'idx_erp_sale_order_sale_user_id',   'sale_user_id');
CALL p_v90_add_index('erp_purchase_order', 'idx_erp_purchase_order_order_time', 'order_time');
CALL p_v90_add_index('erp_purchase_order', 'idx_erp_purchase_order_account_id', 'account_id');

-- 8.2 出入库/退货单 → 上游订单（可出库/可退货列表、订单追踪）
CALL p_v90_add_index('erp_purchase_in',     'idx_erp_purchase_in_order_id',        'order_id');
CALL p_v90_add_index('erp_purchase_in',     'idx_erp_purchase_in_account_id',      'account_id');
CALL p_v90_add_index('erp_sale_out',        'idx_erp_sale_out_order_id',           'order_id');
CALL p_v90_add_index('erp_sale_out',        'idx_erp_sale_out_account_id',         'account_id');
CALL p_v90_add_index('erp_purchase_return', 'idx_erp_purchase_return_order_id',    'order_id');
CALL p_v90_add_index('erp_sale_return',     'idx_erp_sale_return_order_id',        'order_id');

DROP PROCEDURE IF EXISTS p_v90_add_index;
-- ============================================================
-- 9. 自检门禁（不通过则迁移失败）
--
-- 目的：本脚本的价值全在"71 列一定补齐"。若因表名/列名拼写、存储过程被跳过、
--       权限不足等原因漏补，必须在部署时**响亮地失败**，而不是让应用带着
--       Unknown column 上线（Helm 的 migration Job 失败会阻断发布）。
-- ============================================================
DROP PROCEDURE IF EXISTS p_v90_self_check;
DELIMITER $$
CREATE PROCEDURE p_v90_self_check()
BEGIN
    DECLARE v_expected INT DEFAULT 0;
    DECLARE v_missing  INT DEFAULT 0;
    DECLARE v_message  VARCHAR(128);

    CREATE TEMPORARY TABLE IF NOT EXISTS tmp_v90_expected (
        table_name  VARCHAR(64) NOT NULL,
        column_name VARCHAR(64) NOT NULL,
        PRIMARY KEY (table_name, column_name)
    ) ENGINE = MEMORY;

    TRUNCATE TABLE tmp_v90_expected;
    INSERT INTO tmp_v90_expected (table_name, column_name) VALUES
        ('erp_sale_order', 'account_id'), ('erp_sale_order', 'sale_user_id'),
        ('erp_sale_order', 'order_time'), ('erp_sale_order', 'total_product_price'),
        ('erp_sale_order', 'total_tax_price'), ('erp_sale_order', 'total_price'),
        ('erp_sale_order', 'discount_percent'), ('erp_sale_order', 'discount_price'),
        ('erp_sale_order', 'deposit_price'), ('erp_sale_order', 'out_count'),
        ('erp_sale_order', 'return_count'), ('erp_sale_order', 'file_url'),
        ('erp_purchase_order', 'account_id'), ('erp_purchase_order', 'order_time'),
        ('erp_purchase_order', 'total_product_price'), ('erp_purchase_order', 'total_tax_price'),
        ('erp_purchase_order', 'total_price'), ('erp_purchase_order', 'discount_percent'),
        ('erp_purchase_order', 'discount_price'), ('erp_purchase_order', 'deposit_price'),
        ('erp_purchase_order', 'in_count'), ('erp_purchase_order', 'return_count'),
        ('erp_purchase_order', 'file_url'),
        ('erp_purchase_in', 'account_id'), ('erp_purchase_in', 'order_id'),
        ('erp_purchase_in', 'order_no'), ('erp_purchase_in', 'total_product_price'),
        ('erp_purchase_in', 'total_tax_price'), ('erp_purchase_in', 'total_price'),
        ('erp_purchase_in', 'discount_percent'), ('erp_purchase_in', 'discount_price'),
        ('erp_purchase_in', 'other_price'), ('erp_purchase_in', 'payment_price'),
        ('erp_purchase_in', 'file_url'),
        ('erp_sale_out', 'account_id'), ('erp_sale_out', 'sale_user_id'),
        ('erp_sale_out', 'order_id'), ('erp_sale_out', 'order_no'),
        ('erp_sale_out', 'total_product_price'), ('erp_sale_out', 'total_tax_price'),
        ('erp_sale_out', 'total_price'), ('erp_sale_out', 'discount_percent'),
        ('erp_sale_out', 'discount_price'), ('erp_sale_out', 'other_price'),
        ('erp_sale_out', 'receipt_price'), ('erp_sale_out', 'file_url'),
        ('erp_purchase_return', 'account_id'), ('erp_purchase_return', 'order_id'),
        ('erp_purchase_return', 'order_no'), ('erp_purchase_return', 'return_time'),
        ('erp_purchase_return', 'total_product_price'), ('erp_purchase_return', 'total_tax_price'),
        ('erp_purchase_return', 'total_price'), ('erp_purchase_return', 'discount_percent'),
        ('erp_purchase_return', 'discount_price'), ('erp_purchase_return', 'other_price'),
        ('erp_purchase_return', 'refund_price'), ('erp_purchase_return', 'file_url'),
        ('erp_sale_return', 'account_id'), ('erp_sale_return', 'sale_user_id'),
        ('erp_sale_return', 'order_id'), ('erp_sale_return', 'order_no'),
        ('erp_sale_return', 'return_time'), ('erp_sale_return', 'total_product_price'),
        ('erp_sale_return', 'total_tax_price'), ('erp_sale_return', 'total_price'),
        ('erp_sale_return', 'discount_percent'), ('erp_sale_return', 'discount_price'),
        ('erp_sale_return', 'other_price'), ('erp_sale_return', 'refund_price'),
        ('erp_sale_return', 'file_url');

    SELECT COUNT(*) INTO v_expected FROM tmp_v90_expected;
    SELECT COUNT(*) INTO v_missing
    FROM tmp_v90_expected e
    LEFT JOIN information_schema.columns ic
           ON ic.table_schema = DATABASE()
          AND ic.table_name = e.table_name
          AND ic.column_name = e.column_name
    WHERE ic.column_name IS NULL;

    SET v_message = CONCAT('V90 self-check failed: ', v_expected - v_missing, '/', v_expected,
                           ' expected columns exist, ', v_missing, ' missing');
    IF v_expected <> 71 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V90 self-check failed: expected list is not 71 rows';
    END IF;
    IF v_missing > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_v90_expected;
END$$
DELIMITER ;

CALL p_v90_self_check();
DROP PROCEDURE IF EXISTS p_v90_self_check;
DROP PROCEDURE IF EXISTS p_v90_add_column;
-- ============================================================
-- 10. 回滚说明
--
-- 本脚本**不删除任何列、不删除任何数据**，只做 ADD COLUMN / UPDATE(仅 NULL) /
-- ADD INDEX，因此回滚是"停用新列"而不是"删列"：
--   1) 应用回滚到旧版本：旧代码不认新列，可正常运行（新列留 NULL 不写入）；
--   2) 若必须物理回滚：DROP INDEX -> 将新列置 NULL -> 视需要 DROP COLUMN；
--      但一旦新版本写入了 total_price / out_count 等业务值，物理回滚会丢数据，
--      因此建议保留列、只回滚代码。
--
-- 人工复核 SQL 见 script/enterprise-platform-upgrade/verification/V90_verify.sql
-- （information_schema 存在性、主从金额核对、重复单号、孤儿指向等）。
-- ============================================================

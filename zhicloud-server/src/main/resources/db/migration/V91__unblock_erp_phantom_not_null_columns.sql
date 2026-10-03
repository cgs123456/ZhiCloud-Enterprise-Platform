-- ============================================================
-- V91: 解除 ERP 25 个「幽灵 NOT NULL 列」造成的写入阻断
--
-- 背景（静态核验 + 上游逐字段比对，2026-09-24）
-- ------------------------------------------------------------
-- `V10__erp_ddl.sql` 与 `sql/mysql/erp.sql` 是二开阶段手写的 DDL。其中 25 个列
-- 既不是 Java DO 的字段，也不存在于上游 yudao(YunaiV/ruoyi-vue-pro) 对应 DO：
--
--   表                          幽灵 NOT NULL 列（无默认值）
--   erp_product                 code
--   erp_supplier                code
--   erp_customer                code
--   erp_warehouse               code
--   erp_account                 code, type
--   erp_stock_move              from_warehouse_id, to_warehouse_id
--   erp_stock_check             warehouse_id
--   erp_purchase_order_items    master_id
--   erp_purchase_in_items       master_id
--   erp_purchase_return_items   master_id
--   erp_sale_order_items        master_id
--   erp_sale_out_items          master_id
--   erp_sale_return_items       master_id
--   erp_stock_in_item           master_id
--   erp_stock_out_item          master_id
--   erp_stock_move_item         master_id
--   erp_stock_check_item        master_id
--   erp_finance_payment_item    master_id, bill_id, bill_type
--   erp_finance_receipt_item    master_id, bill_id, bill_type
--   （合计 25 列 / 19 表）
--
-- 为什么必须改：MyBatis-Plus 默认 FieldStrategy.NOT_NULL，DO 里没有的列不会出现在
-- INSERT 语句里；MySQL 8.0 默认 sql_mode 含 STRICT_TRANS_TABLES（本项目未覆盖
-- sql_mode，见 application.yaml / docker-compose / Helm values），于是：
--   * 严格模式：插入第 1 行即报
--       ERROR 1364 (HY000): Field 'xxx' doesn't have a default value
--   * 非严格模式：隐式写入 '' / 0，但 code 系列上还挂着唯一索引
--       uk_erp_{product,supplier,customer,warehouse,account}_code_tenant(code, tenant_id)
--     同一租户插入第 2 行即报 ERROR 1062 重复键
--   —— 两条路都走不通，不是"环境差异"，是 schema 与应用契约不一致。
--
-- 证据（docs/enterprise-platform/evidence/）：
--   * insert_blockers.json  —— 25 列 / 19 表（判据：DDL NOT NULL 且无 DEFAULT 且 DO 无字段）
--   * do_ddl_drift.json     —— 反方向漂移：6 张单据主表缺 71 列（由 V90 修复）
--   * upstream_do_diff.md   —— 上游逐字段比对结论：
--       ErpProductDO(13) / ErpSupplierDO(15) / ErpWarehouseDO(10) / ErpAccountDO(7) /
--       ErpStockMoveDO(8) / ErpStockCheckDO(8) / ErpFinance{Payment,Receipt}ItemDO 与
--       本地 DO 完全一致（ErpCustomerDO 仅多 creditLimit/creditPeriod/usedCredit 三个
--       二开增值字段）；**上述 25 列在上游同样不存在**，说明它们是手写 DDL 的臆造列，
--       而不是"上游有、本地 DO 漏了"。
--   * 全仓检索：无任何 Java/XML/SQL 引用 masterId / billId / billType；
--       from_warehouse_id / to_warehouse_id 的真实语义在 erp_stock_move_item
--       （V82 已补齐，且与上游 ErpStockMoveItemDO 一致）。
--
-- 策略
-- ------------------------------------------------------------
--   1) 只放宽可空性（NOT NULL -> NULL），**不删列、不删数据**，可安全回滚；
--   2) 对 V82 已经建立"新列 = 真相源"的 12 张表，把旧列用新列回填一次
--      （`WHERE 旧列 IS NULL` 守卫），让仍在 join 旧列的老 SQL / 报表保持可用；
--   3) 幂等：全部先查 information_schema 再动态执行，重复执行无副作用；
--   4) 自检门禁：25 列必须全部 IS_NULLABLE='YES'，否则 SIGNAL 让迁移 Job 失败
--      （Helm 的 PreSync Job 会阻断发布，不会静默上线）。
--
-- ⚠️ 为什么不用 DEFAULT 0 / DEFAULT '' 代替 NULL：
--      那样 code 系列在同一租户下会全部取 ''，直接撞唯一索引，问题只是被推迟；
--      且 0 / '' 会被下游误读成"真实编码"。NULL 才是"该列不由应用维护"的诚实表达。
--
-- ⚠️ 收尾（本脚本不做，见 docs/enterprise-platform/03-目标架构与升级方案.md 阶段 1）：
--      在真库确认这些列 100% 为 NULL 后，再 DROP COLUMN（连带 DROP INDEX
--      uk_erp_*_code_tenant）；届时 DDL 与 DO 才真正同构。
--
-- ⚠️ 本脚本**不修改** sql/mysql/erp.sql 与 V10__erp_ddl.sql（历史迁移不可改写，
--      Flyway checksum 会失配）；新库仍会先建出这些列，再被本脚本放宽。
--      干净的做法是把 V10 的列定义随 v2 基线一起修正，见目标架构文档。
--
-- 限界：本机无 JDK / Maven / MySQL，脚本未在真库执行过；请先在预发布库跑
--      script/enterprise-platform-upgrade/verification/V91_verify.sql 复核。
-- ============================================================

-- ============================================================
-- 0. 放宽可空性的通用存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_v91_make_nullable;
DELIMITER $$
CREATE PROCEDURE p_v91_make_nullable(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
    DECLARE v_nullable VARCHAR(3) DEFAULT NULL;
    DECLARE v_type     LONGTEXT DEFAULT NULL;
    DECLARE v_charset  VARCHAR(64) DEFAULT NULL;
    DECLARE v_collate  VARCHAR(64) DEFAULT NULL;
    DECLARE v_extra    VARCHAR(255) DEFAULT NULL;
    DECLARE v_comment  TEXT DEFAULT NULL;
    DECLARE v_sql      LONGTEXT DEFAULT NULL;

    -- 表不存在（例如精简部署裁掉了某个模块）直接跳过
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table) THEN

        SELECT c.IS_NULLABLE, c.COLUMN_TYPE, c.CHARACTER_SET_NAME, c.COLLATION_NAME,
               c.EXTRA, c.COLUMN_COMMENT
          INTO v_nullable, v_type, v_charset, v_collate, v_extra, v_comment
          FROM information_schema.columns c
         WHERE c.table_schema = DATABASE()
           AND c.table_name = p_table
           AND c.column_name = p_column;

        -- 列已可空（或列本就不存在）→ 幂等跳过
        IF v_nullable = 'NO' THEN
            SET v_sql = CONCAT(
                'ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_column, '` ', v_type,
                IF(v_charset IS NULL, '', CONCAT(' CHARACTER SET ', v_charset)),
                IF(v_collate IS NULL, '', CONCAT(' COLLATE ', v_collate)),
                ' NULL DEFAULT NULL',
                -- EXTRA 里只保留 "on update ..." 这类真实属性；
                -- DEFAULT_GENERATED / auto_increment 不能这样带过来
                IF(v_extra IS NULL OR v_extra = '' OR v_extra LIKE '%auto_increment%'
                   OR v_extra LIKE '%DEFAULT_GENERATED%',
                   '',
                   CONCAT(' ', REPLACE(v_extra, 'DEFAULT_GENERATED', ''))),
                IF(v_comment IS NULL OR v_comment = '', '', CONCAT(' COMMENT ', QUOTE(v_comment)))
            );
            SET @v91_sql = v_sql;
            PREPARE v91_stmt FROM @v91_sql;
            EXECUTE v91_stmt;
            DEALLOCATE PREPARE v91_stmt;
        END IF;

    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 1. 主数据（5 张表 / 6 列）
--    上游 ErpProductDO 只有 barCode（条码）没有 code；ErpSupplierDO/ErpWarehouseDO
--    同理；ErpAccountDO 用的是 no（账户号）+ sort + default_status，既没有 code
--    也没有 type。这 6 列纯属手写 DDL 的臆造列，且被 NOT NULL 钉死。
-- ============================================================
CALL p_v91_make_nullable('erp_product',   'code');
CALL p_v91_make_nullable('erp_supplier',  'code');
CALL p_v91_make_nullable('erp_customer',  'code');
CALL p_v91_make_nullable('erp_warehouse', 'code');
CALL p_v91_make_nullable('erp_account',   'code');
CALL p_v91_make_nullable('erp_account',   'type');

-- ============================================================
-- 2. 库存单据主表（2 张表 / 3 列）
--    ErpStockMoveDO / ErpStockCheckDO 的字段与上游完全一致，仓库维度都在**明细**
--    （erp_stock_move_item.from_warehouse_id / to_warehouse_id / warehouse_id），
--    主表上的这三个列没有任何代码读写。
-- ============================================================
CALL p_v91_make_nullable('erp_stock_move',  'from_warehouse_id');
CALL p_v91_make_nullable('erp_stock_move',  'to_warehouse_id');
CALL p_v91_make_nullable('erp_stock_check', 'warehouse_id');

-- ============================================================
-- 3. 采购 / 销售明细（6 张表 / 6 列）
--    V82 已把语义列补上（order_id / in_id / return_id / out_id），并把旧列
--    master_id 的值复制过去；但旧列仍是 NOT NULL 且无默认值，而 DO 里只有新列，
--    于是 INSERT 必然漏掉 master_id。
-- ============================================================
CALL p_v91_make_nullable('erp_purchase_order_items',  'master_id');
CALL p_v91_make_nullable('erp_purchase_in_items',     'master_id');
CALL p_v91_make_nullable('erp_purchase_return_items', 'master_id');
CALL p_v91_make_nullable('erp_sale_order_items',      'master_id');
CALL p_v91_make_nullable('erp_sale_out_items',        'master_id');
CALL p_v91_make_nullable('erp_sale_return_items',     'master_id');

-- ============================================================
-- 4. 其它出入库 / 调拨 / 盘点明细（4 张表 / 4 列）
-- ============================================================
CALL p_v91_make_nullable('erp_stock_in_item',    'master_id');
CALL p_v91_make_nullable('erp_stock_out_item',   'master_id');
CALL p_v91_make_nullable('erp_stock_move_item',  'master_id');
CALL p_v91_make_nullable('erp_stock_check_item', 'master_id');

-- ============================================================
-- 5. 收付款明细（2 张表 / 6 列）
--    V82 已补 payment_id / receipt_id / biz_type / biz_id；旧列
--    master_id / bill_id / bill_type 仍是 NOT NULL 且无默认值。
-- ============================================================
CALL p_v91_make_nullable('erp_finance_payment_item', 'master_id');
CALL p_v91_make_nullable('erp_finance_payment_item', 'bill_id');
CALL p_v91_make_nullable('erp_finance_payment_item', 'bill_type');
CALL p_v91_make_nullable('erp_finance_receipt_item', 'master_id');
CALL p_v91_make_nullable('erp_finance_receipt_item', 'bill_id');
CALL p_v91_make_nullable('erp_finance_receipt_item', 'bill_type');

-- 1+2+3+4+5 = 6 + 3 + 6 + 4 + 6 = 25 列，与 evidence/insert_blockers.json 一致
-- ============================================================
-- 6. 旧列回填（只填 NULL，不覆盖已有值）
--
--    目的：V82 把"新列"确立为真相源后，旧列会随着新写入的行变成 NULL。为了让
--    仍然 join 旧列的历史 SQL / 报表 / 数据同步作业不立刻断掉，这里把旧列用新列
--    回填一次。
--
--    正确性依据 [已核验]：V82__fix_do_column_mappings.sql:118-149 的原始回填就是
--    同一组"旧列 -> 新列"映射（`SET in_id = master_id` 等），方向反过来即本段。
--    [判断] 若真库中某行的旧列与新列同时有值且不一致，本段不覆盖（WHERE 旧列 IS
--    NULL 守卫），不一致本身应作为数据质量问题单独排查。
--
--    注意：这 16 列之外，erp_product/supplier/customer/warehouse/account.code、
--    erp_account.type、erp_stock_move.{from,to}_warehouse_id、erp_stock_check.
--    warehouse_id 共 9 列**没有**对应的新列可回填 —— 它们纯粹是死列，
--    本次只放宽可空性，值保持 NULL。
-- ============================================================
DROP PROCEDURE IF EXISTS p_v91_copy_legacy;
DELIMITER $$
CREATE PROCEDURE p_v91_copy_legacy(IN p_table VARCHAR(64), IN p_legacy VARCHAR(64), IN p_source VARCHAR(64))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    DECLARE v_sql LONGTEXT DEFAULT NULL;

    SELECT COUNT(*) INTO v_cnt
      FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = p_table
       AND column_name IN (p_legacy, p_source);

    -- 两个列都在才回填（老库/裁剪库缺列时静默跳过）
    IF v_cnt = 2 THEN
        SET v_sql = CONCAT('UPDATE `', p_table, '` SET `', p_legacy, '` = `', p_source,
                           '` WHERE `', p_legacy, '` IS NULL AND `', p_source, '` IS NOT NULL');
        SET @v91_sql = v_sql;
        PREPARE v91_stmt FROM @v91_sql;
        EXECUTE v91_stmt;
        DEALLOCATE PREPARE v91_stmt;
    END IF;
END$$
DELIMITER ;

-- 6.1 采购 / 销售明细：master_id <- 语义列
CALL p_v91_copy_legacy('erp_purchase_order_items',  'master_id', 'order_id');
CALL p_v91_copy_legacy('erp_purchase_in_items',     'master_id', 'in_id');
CALL p_v91_copy_legacy('erp_purchase_return_items', 'master_id', 'return_id');
CALL p_v91_copy_legacy('erp_sale_order_items',      'master_id', 'order_id');
CALL p_v91_copy_legacy('erp_sale_out_items',        'master_id', 'out_id');
CALL p_v91_copy_legacy('erp_sale_return_items',     'master_id', 'return_id');

-- 6.2 其它出入库 / 调拨 / 盘点明细
CALL p_v91_copy_legacy('erp_stock_in_item',    'master_id', 'in_id');
CALL p_v91_copy_legacy('erp_stock_out_item',   'master_id', 'out_id');
CALL p_v91_copy_legacy('erp_stock_move_item',  'master_id', 'move_id');
CALL p_v91_copy_legacy('erp_stock_check_item', 'master_id', 'check_id');

-- 6.3 收付款明细
CALL p_v91_copy_legacy('erp_finance_payment_item', 'master_id', 'payment_id');
CALL p_v91_copy_legacy('erp_finance_payment_item', 'bill_id',   'biz_id');
CALL p_v91_copy_legacy('erp_finance_payment_item', 'bill_type', 'biz_type');
CALL p_v91_copy_legacy('erp_finance_receipt_item', 'master_id', 'receipt_id');
CALL p_v91_copy_legacy('erp_finance_receipt_item', 'bill_id',   'biz_id');
CALL p_v91_copy_legacy('erp_finance_receipt_item', 'bill_type', 'biz_type');

DROP PROCEDURE IF EXISTS p_v91_copy_legacy;
-- ============================================================
-- 7. 自检门禁
--    25 列必须全部存在且 IS_NULLABLE='YES'，否则 SIGNAL 让迁移 Job 失败。
-- ============================================================
DROP PROCEDURE IF EXISTS p_v91_self_check;
DELIMITER $$
CREATE PROCEDURE p_v91_self_check()
BEGIN
    DECLARE v_expected INT DEFAULT 0;
    DECLARE v_missing  INT DEFAULT 0;
    DECLARE v_message  VARCHAR(500) DEFAULT NULL;
    DECLARE v_sql_mode VARCHAR(500) DEFAULT NULL;

    DROP TEMPORARY TABLE IF EXISTS tmp_v91_expected;
    CREATE TEMPORARY TABLE tmp_v91_expected (
        table_name  VARCHAR(64) NOT NULL,
        column_name VARCHAR(64) NOT NULL,
        PRIMARY KEY (table_name, column_name)
    ) ENGINE = InnoDB;

    INSERT INTO tmp_v91_expected (table_name, column_name) VALUES
        ('erp_product', 'code'), ('erp_supplier', 'code'), ('erp_customer', 'code'),
        ('erp_warehouse', 'code'), ('erp_account', 'code'), ('erp_account', 'type'),
        ('erp_stock_move', 'from_warehouse_id'), ('erp_stock_move', 'to_warehouse_id'),
        ('erp_stock_check', 'warehouse_id'),
        ('erp_purchase_order_items', 'master_id'), ('erp_purchase_in_items', 'master_id'),
        ('erp_purchase_return_items', 'master_id'), ('erp_sale_order_items', 'master_id'),
        ('erp_sale_out_items', 'master_id'), ('erp_sale_return_items', 'master_id'),
        ('erp_stock_in_item', 'master_id'), ('erp_stock_out_item', 'master_id'),
        ('erp_stock_move_item', 'master_id'), ('erp_stock_check_item', 'master_id'),
        ('erp_finance_payment_item', 'master_id'), ('erp_finance_payment_item', 'bill_id'),
        ('erp_finance_payment_item', 'bill_type'),
        ('erp_finance_receipt_item', 'master_id'), ('erp_finance_receipt_item', 'bill_id'),
        ('erp_finance_receipt_item', 'bill_type');

    SELECT COUNT(*) INTO v_expected FROM tmp_v91_expected;

    SELECT COUNT(*) INTO v_missing
      FROM tmp_v91_expected e
      LEFT JOIN information_schema.columns ic
             ON ic.table_schema = DATABASE()
            AND ic.table_name = e.table_name
            AND ic.column_name = e.column_name
     WHERE ic.column_name IS NULL
        OR ic.IS_NULLABLE <> 'YES';

    SET v_message = CONCAT('V91 self-check failed: ', v_expected - v_missing, '/', v_expected,
                           ' columns are nullable, ', v_missing, ' still NOT NULL or missing');

    IF v_expected <> 25 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'V91 self-check failed: expected list is not 25 rows';
    END IF;
    IF v_missing > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_message;
    END IF;

    -- 信息性输出：sql_mode 不含 STRICT_TRANS_TABLES 时，说明该库的 sql_mode 被改过。
    -- 这里**不** SIGNAL —— 非严格模式不是本迁移要修的问题，但生产必须为严格模式，
    -- 否则任何漏写的 NOT NULL 列都会被静默填成 '' / 0（比报错更危险）。
    SET v_sql_mode = @@GLOBAL.sql_mode;
    IF v_sql_mode NOT LIKE '%STRICT_TRANS_TABLES%' THEN
        SELECT 'WARN' AS level, 'sql_mode 不含 STRICT_TRANS_TABLES，请在变更单中确认这是有意为之' AS message, v_sql_mode AS sql_mode;
    ELSE
        SELECT 'OK' AS level, 'V91 self-check passed' AS message, v_sql_mode AS sql_mode;
    END IF;

    DROP TEMPORARY TABLE IF EXISTS tmp_v91_expected;
END$$
DELIMITER ;

CALL p_v91_self_check();
DROP PROCEDURE IF EXISTS p_v91_self_check;
DROP PROCEDURE IF EXISTS p_v91_make_nullable;
-- ============================================================
-- 8. 回滚说明
--
-- 本脚本**不删列、不删数据、不改类型、不建/删索引**，唯一的结构变更是把 25 列的
-- NOT NULL 放宽为 NULL，因此：
--   1) 应用回滚到旧版本：旧代码本来就不写这些列，schema 变宽不会影响旧代码；
--   2) 若要物理回滚（重新收紧为 NOT NULL）：只有在确认这 25 列全为 NULL、
--      且应用确定不会再插入不带这些列的行时才可以做，否则会重新触发 1364。
--      回滚 SQL 形如：
--        ALTER TABLE `erp_product` MODIFY COLUMN `code` VARCHAR(64) NOT NULL COMMENT '产品编码';
--      （必须逐列带上原类型/字符集/注释，建议先用 information_schema 导出原定义。）
--   3) 第 6 节的回填是幂等的（WHERE 旧列 IS NULL），回滚不会因为重复执行而变化。
--
-- 人工复核 SQL 见 script/enterprise-platform-upgrade/verification/V91_verify.sql
--   * 25 列的 IS_NULLABLE / COLUMN_DEFAULT 现状
--   * @@GLOBAL.sql_mode / @@SESSION.sql_mode
--   * 旧列与新列的取值一致性（供 DROP COLUMN 决策）
--   * 旧列仍为 NOT NULL 时，用 information_schema 直接定位漏改的表
-- ============================================================

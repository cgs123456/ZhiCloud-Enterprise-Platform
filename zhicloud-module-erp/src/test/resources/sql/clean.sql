-- ERP 模块 H2 清理脚本（§12.248 随付款/收款真表一并补齐）
-- 必要性：BaseDbUnitTest 上挂着 @Sql(scripts = "/sql/clean.sql", AFTER_TEST_METHOD)，
-- 没有它则本模块任何 BaseDbUnitTest 子类都会在用例后报 FileNotFoundException——
-- 这也是此前 ERP 一个真表用例都没有的原因（schema 无人消费）。
-- 顺序：子表先于主表。
DELETE FROM "erp_finance_payment_item";
DELETE FROM "erp_finance_receipt_item";
DELETE FROM "erp_finance_payment";
DELETE FROM "erp_finance_receipt";
DELETE FROM "inventory_item";
DELETE FROM "erp_stock";
DELETE FROM "erp_gl_initial_balance";

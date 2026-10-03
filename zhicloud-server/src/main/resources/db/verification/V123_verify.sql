-- ============================================================
-- V123 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：商城+支付 63 张表必须齐备，且每张表都要有 tenant_id
--   （租户拦截器对全部表追加条件）与自增主键 id。
-- ============================================================

SELECT 'V123_PAY_TABLE_COUNT' AS check_name,
       CASE WHEN COUNT(*) = 14 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'pay\_%';

SELECT 'V123_PRODUCT_TABLE_COUNT' AS check_name,
       CASE WHEN COUNT(*) = 10 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'product\_%';

SELECT 'V123_PROMOTION_TABLE_COUNT' AS check_name,
       CASE WHEN COUNT(*) = 23 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'promotion\_%';

SELECT 'V123_TRADE_TABLE_COUNT' AS check_name,
       CASE WHEN COUNT(*) = 16 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(*) AS actual
  FROM information_schema.tables
 WHERE table_schema = DATABASE()
   AND table_name LIKE 'trade\_%';

SELECT 'V123_TENANT_COLUMN_COVERAGE' AS check_name,
       CASE WHEN COUNT(DISTINCT table_name) = 63 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(DISTINCT table_name) AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND column_name = 'tenant_id'
   AND (table_name LIKE 'pay\_%' OR table_name LIKE 'product\_%'
        OR table_name LIKE 'promotion\_%' OR table_name LIKE 'trade\_%');

SELECT 'V123_AUTO_INCREMENT_COVERAGE' AS check_name,
       CASE WHEN COUNT(DISTINCT table_name) = 63 THEN 'PASS' ELSE 'FAIL' END AS result,
       COUNT(DISTINCT table_name) AS actual
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND column_name = 'id'
   AND EXTRA LIKE '%auto_increment%'
   AND (table_name LIKE 'pay\_%' OR table_name LIKE 'product\_%'
        OR table_name LIKE 'promotion\_%' OR table_name LIKE 'trade\_%');

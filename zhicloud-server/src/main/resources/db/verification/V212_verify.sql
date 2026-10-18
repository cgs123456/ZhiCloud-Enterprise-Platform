-- V212 验证：MES 工艺路线产能表 + MRP 结果行→工单回链表（D4，ADR 270；台账 §12.270）

-- 1. 产能表存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V212_ROUTE_CAPACITY_TABLE_PASS'
            ELSE CONCAT('V212_ROUTE_CAPACITY_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_route_capacity';

-- 2. 产能/效率列齐 + 非空
SELECT CASE WHEN COUNT(*) = 2 THEN 'V212_ROUTE_CAPACITY_COLUMNS_PASS'
            ELSE CONCAT('V212_ROUTE_CAPACITY_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_route_capacity'
   AND COLUMN_NAME IN ('capacity', 'efficiency') AND IS_NULLABLE = 'NO';

-- 3. 产能表唯一键在（tenant_id, route_id）
SELECT CASE WHEN COUNT(DISTINCT INDEX_NAME) = 1 THEN 'V212_ROUTE_CAPACITY_UNIQUE_KEY_PASS'
            ELSE CONCAT('V212_ROUTE_CAPACITY_UNIQUE_KEY_FAIL ', COUNT(DISTINCT INDEX_NAME)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_route_capacity'
   AND INDEX_NAME = 'uk_mes_pro_route_capacity_route';

-- 4. 回链表存在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V212_CONVERT_LINK_TABLE_PASS'
            ELSE CONCAT('V212_CONVERT_LINK_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.TABLES
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_mrp_convert_link';

-- 5. 回链三键列齐 + 非空
SELECT CASE WHEN COUNT(*) = 3 THEN 'V212_CONVERT_LINK_COLUMNS_PASS'
            ELSE CONCAT('V212_CONVERT_LINK_COLUMNS_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_mrp_convert_link'
   AND COLUMN_NAME IN ('plan_id', 'result_id', 'work_order_id') AND IS_NULLABLE = 'NO';

-- 6. 回链表唯一键齐（result_id / work_order_id 两枚）
SELECT CASE WHEN COUNT(DISTINCT INDEX_NAME) = 2 THEN 'V212_CONVERT_LINK_UNIQUE_KEYS_PASS'
            ELSE CONCAT('V212_CONVERT_LINK_UNIQUE_KEYS_FAIL ', COUNT(DISTINCT INDEX_NAME)) END AS check_result
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_pro_mrp_convert_link'
   AND INDEX_NAME IN ('uk_mes_pro_mrp_convert_link_result', 'uk_mes_pro_mrp_convert_link_order');

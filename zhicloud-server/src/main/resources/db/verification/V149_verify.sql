-- V149 验证：财务报表 + 库存对账菜单权限种子的行为断言
-- 1. Flyway 通道
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '149' AND success = 1) = 1
            THEN 'V149_FLYWAY_APPLIED PASS'
            ELSE 'V149_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. 权限串与端点 @PreAuthorize 一字不差（防将来改串忘改种子）
SELECT CASE WHEN (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'erp:financial-statement:query') >= 1
             AND (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'erp:inventory-reconcile:query') >= 1
            THEN 'V149_PERMISSIONS_MATCH_ENDPOINTS PASS'
            ELSE 'V149_PERMISSIONS_MATCH_ENDPOINTS FAIL' END AS check_result;

-- 3. 两行都挂在真实的 ERP 父目录下（2645 财务管理 / 2583 库存管理），非顶级散菜单
SELECT CASE WHEN (SELECT COUNT(*) FROM system_menu c
                   JOIN system_menu p ON p.id = c.parent_id AND p.deleted = b'0'
                  WHERE c.deleted = b'0' AND c.id IN (8100, 8101)) = 2
            THEN 'V149_PARENTHOOD PASS'
            ELSE 'V149_PARENTHOOD FAIL' END AS check_result;

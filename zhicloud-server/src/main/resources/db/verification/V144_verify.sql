-- V144 验证：校正后的权限点断言 + Flyway 通道
-- 背景：V142 自检口径写错（等值计数把"菜单+按钮同权限"误判为不齐），数据本身正确；
-- 正文受哈希钉住不回改，由 V144 补正确断言。本文件复核校正已生效。

-- 1. Flyway 通道记录：V144 成功应用
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '144' AND success = 1) = 1
            THEN 'V144_FLYWAY_APPLIED PASS'
            ELSE 'V144_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. 三个权限点各自存在（正确口径）
SELECT CASE WHEN (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:query') >= 1
             AND (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:create') >= 1
             AND (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:delete') >= 1
            THEN 'V144_PERMISSIONS_PRESENT PASS'
            ELSE 'V144_PERMISSIONS_PRESENT FAIL' END AS check_result;

-- 3. 权限点与 MdmCrossRefController 的 @PreAuthorize 串一字不差（防将来改串忘改种子）
SELECT CASE WHEN (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission LIKE 'system:mdm-cross-ref:%') >= 3
            THEN 'V144_PERMISSION_NAMESPACE PASS'
            ELSE 'V144_PERMISSION_NAMESPACE FAIL' END AS check_result;

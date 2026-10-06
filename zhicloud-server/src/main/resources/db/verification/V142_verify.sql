-- V142 验证：主数据对照管理 API 的权限点与菜单完整性
-- 分工：迁移内自检查"权限点齐了吗、有没有孤儿"，本文件复核"Flyway 通道 + 权限点精确匹配 controller 用到的字符串"。

-- 1. Flyway 通道记录：V142 成功应用
SELECT CASE WHEN (SELECT COUNT(*) FROM flyway_schema_history
                   WHERE version = '142' AND success = 1) = 1
            THEN 'V142_FLYWAY_APPLIED PASS'
            ELSE 'V142_FLYWAY_APPLIED FAIL' END AS check_result;

-- 2. 三个权限点逐一在（与 MdmCrossRefController 的 @PreAuthorize 字符串一字不差，
--    对不上就是"接口写了权限但菜单没种"→恒 403，这是 §12.95.3 的教训）
SELECT CASE WHEN (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:query') = 1
             AND (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:create') = 1
             AND (SELECT COUNT(*) FROM system_menu WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:delete') = 1
            THEN 'V142_PERMISSIONS_MATCH_CONTROLLER PASS'
            ELSE 'V142_PERMISSIONS_MATCH_CONTROLLER FAIL 权限点与 @PreAuthorize 串不匹配' END AS check_result;

-- 3. 菜单挂在「系统管理」(id=1) 目录下，不是顶级散菜单
SELECT CASE WHEN (SELECT COUNT(*) FROM system_menu
                   WHERE deleted = b'0' AND parent_id = 1
                     AND permission = 'system:mdm-cross-ref:query' AND type = 2) = 1
            THEN 'V142_MENU_PARENTHOOD PASS'
            ELSE 'V142_MENU_PARENTHOOD FAIL 未挂到系统管理目录' END AS check_result;

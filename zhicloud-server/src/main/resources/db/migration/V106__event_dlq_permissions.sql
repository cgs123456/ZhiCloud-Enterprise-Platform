-- ============================================================
-- V106: 死信治理权限种子
--
-- 背景：
--   V105 已提供死信重放审计表，管理端也已暴露查询与重放接口。
--   接口上的 @PreAuthorize 只负责校验权限码，若权限码没有登记到
--   system_menu，非超级管理员将无法被授予这些权限。
--
-- 处理：
--   1. 登记 infra:event-dlq:query 与 infra:event-dlq:replay 两个按钮权限。
--   2. 不写死菜单主键，按 permission 判断是否已存在，避免存量库自增值
--      已被运行时菜单推进时发生主键碰撞并被 INSERT IGNORE 静默跳过。
--   3. 不自动授予任何角色；生产环境应由安全管理员按最小权限原则分配。
--
-- 风险与边界：
--   * 权限记录本身不会让用户获得权限，必须由角色-菜单关系显式授予。
--   * 当前没有前端死信治理页面，因此只登记按钮权限，不创建可见菜单路由。
--   * 超级管理员是否绕过权限校验取决于运行配置，不能据此认为普通角色已获权。
-- ============================================================

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
                         status, creator, create_time, updater, update_time, deleted)
SELECT '死信查询', 'infra:event-dlq:query', 3, 91, 2, '', '', '', 0,
       'system', NOW(), 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE permission = 'infra:event-dlq:query' AND deleted = 0
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component,
                         status, creator, create_time, updater, update_time, deleted)
SELECT '死信重放', 'infra:event-dlq:replay', 3, 92, 2, '', '', '', 0,
       'system', NOW(), 'system', NOW(), 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu
    WHERE permission = 'infra:event-dlq:replay' AND deleted = 0
);

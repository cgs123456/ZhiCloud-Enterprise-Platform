-- ============================================================
-- V75: WMS PDA 移动端权限菜单初始化（安全加固阶段 A）
--
-- 背景：WmsPda{Pick,Inventory,Receipt}Controller 与 WmsPdaController 此前标注类级
--       @PermitAll，导致 /app-api/wms/pda/** 全量接口匿名可访问；其中
--       my-pick-tasks 还以前端传入的 pickerUserId 作为查询条件，构成越权(IDOR)。
--
-- 修复：
--   1) 代码侧移除类级 @PermitAll（仅保留 /login），并为全部写接口补 @PreAuthorize；
--      拣货任务查询改由 SecurityFrameworkUtils.getLoginUserId() 推导身份。
--   2) 本脚本补齐对应的 system_menu 权限记录，否则新加的 wms:pda:* 权限码
--      在库中不存在，任何角色都无法被授予，PDA 会从「人人可用」直接变成「人人不可用」。
--
-- 幂等性：菜单以 permission 唯一性做 NOT EXISTS 判定，全新库与存量库均可重复执行。
--
-- 上线提示：执行后需在「系统管理 - 角色管理」中把 wms:pda:* 勾选给 PDA 作业角色，
--          否则现场设备登录后调用作业接口会返回 403。
-- ============================================================

-- ----------------------------
-- 1. PDA 作业目录（二级菜单，挂在根节点下）
-- ----------------------------
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 作业', '', 2, 90, 0, '/wms/pda', 'ep:cellphone', 'wms/pda/index', 0,
       'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE name = 'PDA 作业' AND permission = '' AND deleted = 0
);

-- ----------------------------
-- 2. 按钮级权限（type=3），父节点为上面的「PDA 作业」
-- ----------------------------
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 扫码', 'wms:pda:scan', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:scan')
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 收货确认', 'wms:pda:receipt', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:receipt')
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 上架执行', 'wms:pda:putaway', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:putaway')
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 拣货执行', 'wms:pda:pick', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:pick')
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 打包确认', 'wms:pda:pack', 3, 5, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:pack')
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 移库确认', 'wms:pda:move', 3, 6, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:move')
LIMIT 1;

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 盘点录入', 'wms:pda:check', 3, 7, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 作业' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'wms:pda:check')
LIMIT 1;

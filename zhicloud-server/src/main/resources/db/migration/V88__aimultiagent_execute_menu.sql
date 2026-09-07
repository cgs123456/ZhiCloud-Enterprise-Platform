-- ======================== 多 Agent 编排执行控制台：菜单 ========================
-- 作者：zhicloud
-- 说明：AI 大模型（2758）下新增「Agent 编排执行」页面（SSE 实时进度 + 中断恢复），
--       按钮权限复用已有 aimultiagent:execute:query / run（控制器注解已存在，此处仅补菜单可见性）。
--       超级管理员自动拥有全部菜单，无需补 system_role_menu。

-- ============================================================
-- Agent 编排执行页面，挂载 AI 大模型 2758 下
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6100, 'Agent编排执行', '', 2, 7, 2758, 'multiagent-execute', 'ep:cpu', 'ai/multiagent/index', 'AiMultiAgentExecute', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (6101, 'Agent编排执行查询', 'aimultiagent:execute:query', 3, 1, 6100, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (6102, 'Agent编排执行', 'aimultiagent:execute:run', 3, 2, 6100, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- ============================================================
-- V76: MES PDA 报工权限菜单初始化（安全加固阶段 A · 互检返工）
--
-- 背景：MesProPdaFeedbackController 此前四个接口全部标注 @PermitAll，
--       类注释写的是「由 App 端 token 认证」，但 @PermitAll 的语义恰恰是
--       <跳过> 认证，注释与实现完全相反。实际暴露面：
--         POST /mes-api/pro/feedback/scan-work-order   匿名枚举全部工单号与工艺路线
--         POST /mes-api/pro/feedback/submit-feedback   匿名灌入伪造报工，污染产量/工时/不良数
--         POST /mes-api/pro/feedback/scan-machinery    匿名枚举全部设备台账
--         GET  /mes-api/pro/feedback/my-feedback-list  匿名 + 前端传 feedbackUserId ⇒ 越权(IDOR)
--                                                      遍历自增 ID 即可拖走全厂人员报工明细
--
-- 修复：
--   1) 代码侧四个接口全部改为 @PreAuthorize；my-feedback-list 移除 feedbackUserId 入参，
--      改由 SecurityFrameworkUtils.getLoginUserId() 从登录态推导身份。
--   2) 本脚本补齐对应的 system_menu 权限记录。若只改代码不建权限码，
--      库中不存在 mes:pda:scan / mes:pda:feedback，任何角色都无法被授予，
--      PDA 会从「人人可用」直接翻转为「人人不可用」（全量 403）。
--
-- 幂等性：菜单以 permission 唯一性做 NOT EXISTS 判定，全新库与存量库均可重复执行。
--
-- 上线提示：执行后需在「系统管理 - 角色管理」中把 mes:pda:* 勾选给车间报工角色，
--          否则工位机登录后调用报工接口会返回 403。
-- ============================================================

-- ----------------------------
-- 1. PDA 报工目录（二级菜单，挂在根节点下）
-- ----------------------------
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 报工', '', 2, 91, 0, '/mes/pda', 'ep:cellphone', 'mes/pda/index', 0,
       'admin', NOW(), 'admin', NOW(), 0
WHERE NOT EXISTS (
    SELECT 1 FROM system_menu WHERE name = 'PDA 报工' AND permission = '' AND deleted = 0
);

-- ----------------------------
-- 2. 按钮级权限（type=3），父节点为上面的「PDA 报工」
-- ----------------------------

-- 扫码类：扫工单二维码、扫设备二维码
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 扫码', 'mes:pda:scan', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 报工' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'mes:pda:scan')
LIMIT 1;

-- 报工类：提交报工、查询本人报工记录
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status,
                         creator, create_time, updater, update_time, deleted)
SELECT 'PDA 报工提交', 'mes:pda:feedback', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m
WHERE m.name = 'PDA 报工' AND m.permission = '' AND m.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission = 'mes:pda:feedback')
LIMIT 1;

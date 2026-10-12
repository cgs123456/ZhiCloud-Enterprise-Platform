-- ============================================================
-- V185：退役旧 /hr 人事域菜单（前端页面 + 后端接口 + 菜单树）
-- ============================================================
-- 背景（台账 §12.201）：HRM 新链（/hrm/*）已全量接线并通过多轮真机回归
-- （12 组端到端 × 3 轮 / 404 断言 / 全绿，单测 74/74），旧 /hr 域前端页面、
-- 后端 Controller 与菜单已无入口。本刀把旧 /hr 的菜单树从 system_menu 摘掉。
--
-- 为什么用「反向清理」而不是改历史迁移：V134 正文已被哈希钉住不许改，
-- 且 V134 是既有事实（同文件里还有 QMS 段，不能整体回退）；本迁移在其后执行
-- 达成净效果，从零建库同样收敛到无 /hr 菜单。
--
-- 命中口径（三个条件取并，刻意不含 QMS）：
--   1. 目录根：path = '/hr'（type=1）→ 递归闭包收全部子孙；
--   2. 旧权限点：permission LIKE 'hr:%'（hrm:% 不会被 LIKE 'hr:%' 命中，已实测）；
--   3. 旧组件：component LIKE 'hr/%'（hrm/* 组件不会被命中，已实测）。
-- 表与字典一律不动：hr_leave_request 仍被 V126/V130 的报表视图
-- v_bpm_approval_inflight 读取（视图正文哈希钉住），hr_employee/hr_position
-- 仍是 HRM 复用的主数据表。本迁移只删菜单与角色关联，不碰任何表。
-- ============================================================

-- 1. 旧 /hr 菜单闭包（目录 + 全部子孙 + 旧权限点 + 旧组件），先收后删
CREATE TEMPORARY TABLE tmp_hr_menu (id BIGINT PRIMARY KEY);
INSERT INTO tmp_hr_menu
WITH RECURSIVE old_hr AS (
    SELECT id FROM system_menu WHERE deleted = b'0'
      AND ( `path` = '/hr'
         OR permission LIKE 'hr:%'
         OR component LIKE 'hr/%' )
    UNION ALL
    SELECT c.id FROM system_menu c JOIN old_hr p ON c.parent_id = p.id AND c.deleted = b'0'
) SELECT DISTINCT id FROM old_hr;  -- 基础命中与父链递归会从两条路产出同一 id，DISTINCT 收口

DELETE rm FROM system_role_menu rm JOIN tmp_hr_menu t ON rm.menu_id = t.id;
DELETE m FROM system_menu m JOIN tmp_hr_menu t ON m.id = t.id;
DROP TEMPORARY TABLE tmp_hr_menu;

-- 自检 1：旧 /hr 菜单清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V185_NO_HR_MENUS PASS'
            ELSE CONCAT('V185_NO_HR_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu WHERE deleted = b'0'
   AND ( `path` = '/hr' OR permission LIKE 'hr:%' OR component LIKE 'hr/%' );

-- 自检 2：HRM 菜单树完好（防止误删——/hrm 根与职位菜单必须在）
SELECT CASE WHEN COUNT(*) >= 2 THEN 'V185_HRM_MENUS_INTACT PASS'
            ELSE CONCAT('V185_HRM_MENUS_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM system_menu WHERE deleted = b'0'
   AND ( `path` = '/hrm' OR `component` = 'hrm/position/index' );

-- 自检 3：菜单树无孤儿（父被删子残留会在这暴露）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V185_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V185_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM system_menu m WHERE m.deleted = b'0' AND m.parent_id <> 0
   AND NOT EXISTS (SELECT 1 FROM system_menu p WHERE p.id = m.parent_id AND p.deleted = b'0');

-- 自检 4：HRM 主数据表仍在（hr_employee / hr_position 是 HRM 复用的表，绝不能被碰）
SELECT CASE WHEN COUNT(*) = 2 THEN 'V185_HRM_TABLES_INTACT PASS'
            ELSE CONCAT('V185_HRM_TABLES_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND table_name IN ('hr_employee', 'hr_position');

-- 自检 5：报表视图仍在（v_bpm_approval_inflight 依赖 hr_leave_request，必须保留）
SELECT CASE WHEN COUNT(*) = 1 THEN 'V185_INFLIGHT_VIEW_INTACT PASS'
            ELSE CONCAT('V185_INFLIGHT_VIEW_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.views WHERE table_schema = DATABASE()
   AND table_name = 'v_bpm_approval_inflight';

-- 自检 6：企业域菜单总量仍在合理量级（防止整棵树被清空）
SELECT CASE WHEN COUNT(*) > 200 THEN CONCAT('V185_ENTERPRISE_MENUS_INTACT PASS ', COUNT(*))
            ELSE CONCAT('V185_ENTERPRISE_MENUS_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM system_menu WHERE deleted = b'0';

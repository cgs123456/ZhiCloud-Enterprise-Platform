-- ============================================================
-- V153：修正「代码生成案例」目录菜单的组件字段（目录不应指向页面）
-- ============================================================
-- 背景（2026-10-10 全量核实）：应用返回给前端的菜单里，id=1070「代码生成案例」是
--   一个 **目录**（type=1），却被种上了 component='infra/testDemo/index'（V1.1 基线），
--   而该路径在前端不存在。它的 5 个子菜单（id 2472/2478/2484/2490/2497）全部指向
--   真实存在的演示页面（infra/demo/demo01|demo02|demo03/...），所以**只有父目录这一格
--   是死的**：前端按目录展开子项，父级 component 本就应为空。
--
-- 处置：把 id=1070 的 component 置空（目录不承载页面），不动任何子菜单与按钮。
-- 幂等：只在 component 仍为旧错值时才改。只认 deleted=b'0'。
-- 说明：type=1 目录的 component 为空是本仓库既有约定（例如 /hr、/qms、/oa 等目录都是 ''）。
-- ============================================================

UPDATE `system_menu`
   SET `component` = '',
       `updater` = 'system',
       `update_time` = NOW()
 WHERE `deleted` = b'0'
   AND `id` = 1070
   AND `type` = 1
   AND `component` = 'infra/testDemo/index';

-- 自检 1：该目录的 component 已为空
SELECT CASE WHEN COUNT(*) = 1 THEN 'V153_TESTDEMO_DIR_COMPONENT PASS'
            ELSE CONCAT('V153_TESTDEMO_DIR_COMPONENT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `id` = 1070 AND `type` = 1 AND `component` = '';

-- 自检 2：其 5 个子菜单仍在且仍指向 demo 页面（本迁移不误伤子项）
SELECT CASE WHEN COUNT(*) = 5 THEN 'V153_TESTDEMO_CHILDREN PASS'
            ELSE CONCAT('V153_TESTDEMO_CHILDREN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `parent_id` = 1070
   AND `component` LIKE 'infra/demo/%';

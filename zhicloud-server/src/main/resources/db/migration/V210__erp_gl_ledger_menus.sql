-- ============================================================
-- V210：FMS 账表（ledger 域）重建——ERP 端点配套菜单播种（C6②，ADR 266）
-- ============================================================
-- 背景（台账 §12.180/§12.210、ADR 266）：FMS/ERP 财务融合（V190，提交 412be4c6c）删掉的
-- FMS 账表死页（8 页面 / 16 端点契约）按重建矩阵 C6② 重建：后端自零建于 ERP 模块
-- /erp/gl-ledger/**（9 端点 = 金额式四族 × list+export + 明细账科目树），FMS 页面直连
-- ERP（V194 科目先例，不建平行表/代理层）；权限的单一真相在 ERP（erp:gl-ledger:*）。
-- 本迁移**不含 DDL**：账表是纯查询域，凭证/分录/科目/期初四张表均已就位（期初表 = V209
-- C6③）；唯一存量变更是菜单播种——即 V158 头注计划里"账簿待后端完成后追加"的那一步。
-- 幂等：页面按 (id, path) 预检、按钮按 (id, permission) 预检。四个页面共用同一权限串
-- erp:gl-ledger:query，页面**不能**沿用 V209 的 permission 预检（否则第二页起会被首行
-- 误挡），此处按需调整并在自检里以「4 页面 / 4 唯一 path」双向断言加固。
-- 只认 deleted=b'0'；system_menu 无 tenant_id（全局表，租户可见性由角色决定）。
-- 刻意不写 system_role_menu：超管走旁路即可见，给普通角色授权是运营动作（V149/V158 先例）。
-- ============================================================

-- 1. 四个账表页面（挂 FMS 目录 9000；sort 10–13 排在 9090 期初余额之后）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9081 AS `id`, '明细账' AS `name`, 'erp:gl-ledger:query' AS `permission`, 2 AS `type`, 10 AS `sort`, 9000 AS `parent_id`, 'ledger/detail' AS `path`, 'ep:list' AS `icon`, 'fms/ledger/detail/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9082, '总账', 'erp:gl-ledger:query', 2, 11, 9000, 'ledger/general', 'ep:document', 'fms/ledger/general/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9083, '科目余额表', 'erp:gl-ledger:query', 2, 12, 9000, 'ledger/subject-balance', 'ep:grid', 'fms/ledger/subject-balance/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9084, '多栏账', 'erp:gl-ledger:query', 2, 13, 9000, 'ledger/multi-column', 'ep:histogram', 'fms/ledger/multi-column/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
   WHERE m.`deleted` = b'0'
     AND ( m.`id` = t.`id`
        OR (t.`type` = 2 AND m.`type` = 2 AND m.`parent_id` = t.`parent_id` AND m.`path` = t.`path`) )
);

-- 2. 导出/打印共享按钮（挂 9000，四页共用；print 是纯前端权限，无后端端点）
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9085 AS `id`, '账簿导出' AS `name`, 'erp:gl-ledger:export' AS `permission`, 3 AS `type`, 1 AS `sort`, 9000 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9086, '账簿打印', 'erp:gl-ledger:print', 3, 2, 9000, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
   WHERE m.`deleted` = b'0'
     AND ( m.`id` = t.`id`
        OR (t.`type` = 3 AND m.`type` = 3 AND m.`permission` = t.`permission`) )
);

-- 3. 自检 1：6 个菜单行在位（缺一即页面 404 / 按钮权限拿不到）
SELECT CASE WHEN COUNT(*) = 6 THEN 'V210_MENU_COUNT PASS'
            ELSE CONCAT('V210_MENU_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `id` IN (9081, 9082, 9083, 9084, 9085, 9086);

-- 4. 自检 2：4 个页面 component 与 path 齐（组件指错 = 打开白屏；顺带证明幂等复跑未复制行）
SELECT CASE WHEN COUNT(DISTINCT `component`) = 4 AND COUNT(DISTINCT `path`) = 4 THEN 'V210_PAGE_COMPONENT PASS'
            ELSE CONCAT('V210_PAGE_COMPONENT FAIL comp=', COUNT(DISTINCT `component`),
                        ' path=', COUNT(DISTINCT `path`)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `component` IN ('fms/ledger/detail/index', 'fms/ledger/general/index',
                       'fms/ledger/subject-balance/index', 'fms/ledger/multi-column/index')
   AND `path` IN ('ledger/detail', 'ledger/general', 'ledger/subject-balance', 'ledger/multi-column');

-- 5. 自检 3：权限串恰好 3 个（query/export/print），且 4 页共用 query
SELECT CASE WHEN COUNT(DISTINCT `permission`) = 3
             AND SUM(`permission` = 'erp:gl-ledger:query') = 4
             AND SUM(`permission` = 'erp:gl-ledger:export') = 1
             AND SUM(`permission` = 'erp:gl-ledger:print') = 1
            THEN 'V210_PERMISSION PASS'
            ELSE CONCAT('V210_PERMISSION FAIL ', COUNT(DISTINCT `permission`)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `id` IN (9081, 9082, 9083, 9084, 9085, 9086);

-- 6. 自检 4：FMS 侧不得出现平行权限串（防 fms:ledger:* 回潮）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V210_NO_PARALLEL_PERMISSION PASS'
            ELSE CONCAT('V210_NO_PARALLEL_PERMISSION FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `permission` LIKE 'fms:ledger%';

-- 7. 自检 5：未种 role_menu（授权是运营动作，不在种子里伪造）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V210_NO_ROLE_MENU PASS'
            ELSE CONCAT('V210_NO_ROLE_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_role_menu`
 WHERE `menu_id` IN (9081, 9082, 9083, 9084, 9085, 9086);

-- 8. 自检 6：无孤儿（父 9000 必须在，否则 FMS 菜单树挂不上）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V210_NO_ORPHAN PASS'
            ELSE CONCAT('V210_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m
 WHERE m.`deleted` = b'0'
   AND m.`id` IN (9081, 9082, 9083, 9084, 9085, 9086)
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

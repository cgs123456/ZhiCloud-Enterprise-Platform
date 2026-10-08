-- ============================================================
-- V149：财务报表与库存对账的菜单权限种子（§12.146 补两类"接了 @PreAuthorize 却没种菜单"）
-- ============================================================
-- 复核发现两处同型缺口（§12.95.3 的教训，MDM 已在 V142 补过）：
--   1. ErpFinancialStatementController 三个端点（balance-sheet/income-statement/cash-flow）
--      全挂 @PreAuthorize('erp:financial-statement:query')，但**全链迁移从未种过该权限的
--      system_menu 行**——普通角色永远 403，只有 super_admin 走旁路能用，报表形同只对管理员存在。
--   2. 新增的库存对账只读端点（InventoryReconciliationService 暴露，见 §12.146）用
--      'erp:inventory-reconcile:query'，同批种行，避免再造一次"接口交上去用不了"。
-- 列集合按 information_schema 核对：system_menu 无 tenant_id（全局表，租户可见性由角色决定）。
-- 幂等：按 (type, permission) 预检、只认 deleted=b'0'（与 V134/V142 一致）。
-- 刻意不写 system_role_menu：超管走旁路即可见，给普通角色授权是运营动作，不在种子里伪造。
-- ============================================================

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 8100 AS `id`, '财务报表' AS `name`, 'erp:financial-statement:query' AS `permission`, 2 AS `type`, 90 AS `sort`, 2645 AS `parent_id`, 'financial-statement' AS `path`, 'ep:data-line' AS `icon`, 'erp/finance/financialStatement/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 8101, '库存对账', 'erp:inventory-reconcile:query', 2, 90, 2583, 'inventory-reconcile', 'ep:sort', 'erp/stock/reconcile/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
    SELECT 1 FROM `system_menu` e
     WHERE e.`deleted` = b'0'
       AND e.`type` = t.`type` AND e.`permission` = t.`permission` AND t.`permission` <> ''
);

-- 自检 1：两个查询权限点都存在（缺一即端点对普通角色恒 403）
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_menu` WHERE deleted = b'0'
                   AND permission = 'erp:financial-statement:query') >= 1
             AND (SELECT COUNT(*) FROM `system_menu` WHERE deleted = b'0'
                   AND permission = 'erp:inventory-reconcile:query') >= 1
            THEN 'V149_STATEMENT_RECONCILE_MENUS PASS'
            ELSE 'V149_STATEMENT_RECONCILE_MENUS FAIL 权限点不齐' END AS check_result;

-- 自检 2：无孤儿（父必须在）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V149_NO_ORPHAN PASS'
            ELSE CONCAT('V149_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.deleted = b'0' AND m.parent_id <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.id = m.parent_id AND p.deleted = b'0');

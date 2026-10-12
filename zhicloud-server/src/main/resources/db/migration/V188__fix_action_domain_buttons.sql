-- ============================================================
-- V188：修正 V186 臆造权限串——按 controller 真实 @PreAuthorize 补种按钮
-- ============================================================
-- 背景（台账 §12.203）：V186 生成按钮时用「模块:URL 路径段」组合权限串，
-- 与 controller 实际 @PreAuthorize 不一致，例如：
--   /mes/dashboard 实际是 mes:home:query（V186 种成 mes:dashboard:query）
--   /erp/sale-order-ddd 实际是 erp:sale-order:query
--   /mes/dv/oee 实际是 mes:dv-oee:calculate / mes:dv-oee:query
-- 后果：页面能打开但动作按钮 403。本刀按源码真实权限点补种到各自菜单页下，
-- 同时把 V186 误挂在模块根目录（parent_id=2563/6400/5100）的按钮归位到菜单页。
-- 幂等：NOT EXISTS 按 permission+type 预检。id 段 9727+。
-- ============================================================
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9727 AS `id`, '供应链BI查询' AS `name`, 'erp:scm-bi:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9700 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9728 AS `id`, '合并引擎生成' AS `name`, 'erp:consolidation-engine:generate' AS `permission`, 3 AS `type`, 1 AS `sort`, 9702 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9729 AS `id`, '合并引擎报表查看' AS `name`, 'erp:consolidation-engine:report' AS `permission`, 3 AS `type`, 1 AS `sort`, 9702 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9730 AS `id`, '成本计算计算' AS `name`, 'erp:cost-calculation:calculate' AS `permission`, 3 AS `type`, 1 AS `sort`, 9703 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9731 AS `id`, '增值税计算查询' AS `name`, 'erp:vat-calculation:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9704 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9732 AS `id`, '销售订单查询查询' AS `name`, 'erp:sale-order:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9706 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9733 AS `id`, 'ABC分析修改' AS `name`, 'wms:abc-analysis:update' AS `permission`, 3 AS `type`, 1 AS `sort`, 9708 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9734 AS `id`, 'ABC分析查询' AS `name`, 'wms:abc-analysis:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9708 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9735 AS `id`, '设备综合效率计算' AS `name`, 'mes:dv-oee:calculate' AS `permission`, 3 AS `type`, 1 AS `sort`, 9711 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9736 AS `id`, '设备综合效率查询' AS `name`, 'mes:dv-oee:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9711 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9737 AS `id`, '设备数据采集查询' AS `name`, 'mes:dv-device-data:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9713 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9738 AS `id`, '设备数据采集删除' AS `name`, 'mes:dv-device-data:delete' AS `permission`, 3 AS `type`, 1 AS `sort`, 9713 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9739 AS `id`, '设备数据采集采集' AS `name`, 'mes:dv-device-data:collect' AS `permission`, 3 AS `type`, 1 AS `sort`, 9713 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9740 AS `id`, 'TPM指标计算' AS `name`, 'mes:dv-tp-kpi:calculate' AS `permission`, 3 AS `type`, 1 AS `sort`, 9716 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9741 AS `id`, 'TPM指标查询' AS `name`, 'mes:dv-tp-kpi:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9716 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9742 AS `id`, '生产看板查询' AS `name`, 'mes:home:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9718 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9743 AS `id`, '高级排产生成' AS `name`, 'mes:pro-aps:generate' AS `permission`, 3 AS `type`, 1 AS `sort`, 9720 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9744 AS `id`, '高级排产查询' AS `name`, 'mes:pro-aps:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9720 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9745 AS `id`, '高级排产修改' AS `name`, 'mes:pro-aps:update' AS `permission`, 3 AS `type`, 1 AS `sort`, 9720 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9746 AS `id`, '物料需求计划新增' AS `name`, 'mes:pro-mrp:create' AS `permission`, 3 AS `type`, 1 AS `sort`, 9723 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9747 AS `id`, '物料需求计划计算' AS `name`, 'mes:pro-mrp:calculate' AS `permission`, 3 AS `type`, 1 AS `sort`, 9723 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9748 AS `id`, '物料需求计划查询' AS `name`, 'mes:pro-mrp:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9723 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL   SELECT 9749 AS `id`, '工单齐套分析执行' AS `name`, 'mes:pro:work-order-kitting:analyze' AS `permission`, 3 AS `type`, 1 AS `sort`, 9726 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (m.`permission` = t.`permission` AND m.`type` = t.`type`) )
);

-- 把 V186 误挂在模块根目录下的按钮归位到各自菜单页
UPDATE `system_menu` SET `parent_id` = 9700 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 2563 AND `permission` LIKE 'erp:scm-bi:%';
UPDATE `system_menu` SET `parent_id` = 9702 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 2563 AND `permission` LIKE 'erp:consolidation-engine:%';
UPDATE `system_menu` SET `parent_id` = 9703 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 2563 AND `permission` LIKE 'erp:cost-calculation:%';
UPDATE `system_menu` SET `parent_id` = 9704 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 2563 AND `permission` LIKE 'erp:vat-calculation:%';
UPDATE `system_menu` SET `parent_id` = 9706 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 2563 AND `permission` LIKE 'erp:sale-order-ddd:%';
UPDATE `system_menu` SET `parent_id` = 9708 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 6400 AND `permission` LIKE 'wms:abc-analysis:%';
UPDATE `system_menu` SET `parent_id` = 9711 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:dv:oee:%';
UPDATE `system_menu` SET `parent_id` = 9713 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:dv-device-data:%';
UPDATE `system_menu` SET `parent_id` = 9716 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:dv:tp-kpi:%';
UPDATE `system_menu` SET `parent_id` = 9718 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:dashboard:%';
UPDATE `system_menu` SET `parent_id` = 9720 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:pro:aps:%';
UPDATE `system_menu` SET `parent_id` = 9723 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:pro:mrp:%';
UPDATE `system_menu` SET `parent_id` = 9726 WHERE `deleted` = b'0' AND `type` = 3 AND `parent_id` = 5100 AND `permission` LIKE 'mes:pro:work-order-kitting:%';

-- 自检 1：各动作域菜单页下至少有 1 按钮
SELECT CASE WHEN COUNT(*) >= 10 THEN CONCAT('V188_BUTTONS_SEEDED PASS ', COUNT(*))
            ELSE CONCAT('V188_BUTTONS_SEEDED FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0' AND `id` BETWEEN 9727 AND 9749;

-- 自检 2：菜单树无孤儿
SELECT CASE WHEN COUNT(*) = 0 THEN 'V188_NO_ORPHAN_MENUS PASS'
            ELSE CONCAT('V188_NO_ORPHAN_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` m WHERE m.`deleted` = b'0' AND m.`parent_id` <> 0
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p WHERE p.`id` = m.`parent_id` AND p.`deleted` = b'0');

-- 自检 3：企业域菜单总量保持在合理量级
SELECT CASE WHEN COUNT(*) > 1945 THEN CONCAT('V188_ENTERPRISE_MENUS PASS ', COUNT(*))
            ELSE CONCAT('V188_ENTERPRISE_MENUS FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` WHERE `deleted` = b'0';

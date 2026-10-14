-- ============================================================
-- V198：TMS 运费报表菜单（前端聚合页，零后端改动）
-- ============================================================
-- 背景（台账 §12.221）：运费报表是「未覆盖业务流程」增强项之一。
-- 实现决策：不新增后端汇总端点——运费报表本质是运费结算单（tms_freight）的
-- 多维汇总，既有 /tms/freight/page 已能按 单号/承运商/状态 过滤，
-- 由前端聚合（按承运商、按月、按状态）并附明细下钻。避免为展示新增聚合接口。
-- 权限沿用 tms:freight:query（报表不引入新权限串）。
-- ============================================================

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 3166 AS `id`, '运费报表' AS `name`, 'tms:freight:query' AS `permission`, 2 AS `type`, 6 AS `sort`, 3160 AS `parent_id`, 'freight-report' AS `path`, 'ep:histogram' AS `icon`, 'tms/freight/report/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND (m.`id` = t.`id`
         OR (t.`type` = 2 AND m.`permission` = t.`permission` AND m.`type` = 2
             AND m.`parent_id` = t.`parent_id`))
);

-- 自检：菜单在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V198_FREIGHT_REPORT_MENU PASS'
            ELSE CONCAT('V198_FREIGHT_REPORT_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `component` = 'tms/freight/report/index';


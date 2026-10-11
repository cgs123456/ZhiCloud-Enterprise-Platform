-- V173: HRM 工作台与操作日志权限种子（挂 9100 /hrm 目录）
-- ============================================================
-- 来源：views/hrm/home/{hr,team} 工作台（无 v-hasPermi，后端 @PreAuthorize 消费）与
--       views/hrm/**/detail 的操作日志页签（api/hrm/operate-log）。
--   * hrm:home:query：HR 首页 + 团队工作台统计/日历
--   * hrm:operate-log:query：详情页操作日志分页
-- 工作台是首页聚合视图（非独立菜单页），两个权限以按钮形式挂 /hrm 目录下便于授权。
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9204-9205 段。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9204 AS `id`, '工作台查询' AS `name`, 'hrm:home:query' AS `permission`, 3 AS `type`, 1 AS `sort`, 9100 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9205, '操作日志查询', 'hrm:operate-log:query', 3, 2, 9100, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` = 3 AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = 3) )
);

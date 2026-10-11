-- V182: HRM 保险域月度社保菜单与按钮权限种子
-- ============================================================
-- 来源：views/hrm/insurance/month-record v-hasPermi 与后端 @PreAuthorize 交集。
--   * 菜单 1 页：月度社保表（component=hrm/insurance/month-record/index）
--   * 按钮：month-record/{query,create,update,delete}（query 为菜单权限；update 覆盖员工参保增改停）
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9254-9258 段。
-- 幂等：菜单按 (permission,type,parent_id) 预检。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9254 AS `id`, '月度社保' AS `name`, 'hrm:insurance:month-record:query' AS `permission`, 2 AS `type`, 23 AS `sort`, 9100 AS `parent_id`, 'insurance/month-record' AS `path`, 'ep:calendar' AS `icon`, 'hrm/insurance/month-record/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9255, '社保表新建', 'hrm:insurance:month-record:create', 3, 1, 9254, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9256, '社保表修改', 'hrm:insurance:month-record:update', 3, 2, 9254, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9257, '社保表删除', 'hrm:insurance:month-record:delete', 3, 3, 9254, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

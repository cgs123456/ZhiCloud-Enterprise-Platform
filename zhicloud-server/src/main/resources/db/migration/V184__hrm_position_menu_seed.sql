-- V184: HRM 职位管理菜单与按钮权限种子（HR/HRM 融合：职位主数据入口）
-- ============================================================
-- 背景（台账 §12.199）：HRM 缺职位主数据能力——旧 /hr 的 hr_position（职位编码/名称/部门/职级/基本工资）
-- 在 HRM 无对应物，导致旧 /hr 无法整体退役。本刀在 HRM 补职位入口：**复用 hr_position 表与
-- HrPositionService（单一真相，不建平行表）**，前端新增 views/hrm/position 页。
-- 菜单：1 页（职位管理，component=hrm/position/index）+ 3 按钮（create/update/delete；query 为菜单权限）。
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9258-9261 段。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9258 AS `id`, '职位管理' AS `name`, 'hrm:position:query' AS `permission`, 2 AS `type`, 24 AS `sort`, 9100 AS `parent_id`, 'position' AS `path`, 'ep:briefcase' AS `icon`, 'hrm/position/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9259, '职位新增', 'hrm:position:create', 3, 1, 9258, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9260, '职位修改', 'hrm:position:update', 3, 2, 9258, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9261, '职位删除', 'hrm:position:delete', 3, 3, 9258, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

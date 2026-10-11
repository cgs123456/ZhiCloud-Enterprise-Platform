-- V164: HR/HRM 融合第一刀——HRM 员工/部门菜单与按钮权限种子（目录 1 + 菜单 2 + 按钮 8 = 11 行）
-- 来源: views/hrm/employee、views/hrm/dept 页面 v-hasPermi/checkPermi 与后端 @PreAuthorize 双侧交集。
--   * 员工页按钮 6 个: create/update/delete/export/import/config:update（config 为档案字段配置页保存按钮）；
--     「发送填写档案通知」由 hrm:employee:update 门控，不设独立权限；personal-note 双权限后端声明于
--     /hrm/employee/personal-note/**，前端消费方为 home 日历组件，按钮挂员工菜单下便于授权。
--   * 部门页复用平台 /system/dept 接口与既有 system:dept:* 权限（create/update/delete 按钮平台已种），
--     故本刀只种菜单行（permission=system:dept:query），不重复种按钮。
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9100-9199 段（避开 7200-7334 与 9000-9080）。
-- system_menu 无 tenant_id（全局表）；幂等：目录按 (path,type=1)、菜单/按钮按 (permission,type) 预检。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9100 AS `id`, '人力资源 HRM' AS `name`, '' AS `permission`, 1 AS `type`, 1 AS `sort`, 0 AS `parent_id`, '/hrm' AS `path`, 'ep:box' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9110, '员工管理', 'hrm:employee:query', 2, 1, 9100, 'employee', 'ep:user', 'hrm/employee/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9111, '员工新增', 'hrm:employee:create', 3, 1, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9112, '员工修改', 'hrm:employee:update', 3, 2, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9113, '员工删除', 'hrm:employee:delete', 3, 3, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9114, '员工导出', 'hrm:employee:export', 3, 4, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9115, '员工导入', 'hrm:employee:import', 3, 5, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9116, '员工字段配置', 'hrm:employee:config:update', 3, 6, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9117, '个人备忘新增', 'hrm:employee:personal-note:create', 3, 7, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9118, '个人备忘删除', 'hrm:employee:personal-note:delete', 3, 8, 9110, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9120, '部门管理', 'system:dept:query', 2, 2, 9100, 'dept', 'ep:office-building', 'hrm/dept/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` = 1 AND m.`path` = t.`path` AND m.`type` = 1 AND m.`parent_id` = 0)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

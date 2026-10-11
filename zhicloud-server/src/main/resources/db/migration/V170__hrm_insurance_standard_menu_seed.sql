-- V170: HRM 保险域菜单与按钮权限种子（菜单 1 + 按钮 6 = 7 行，挂 9100 /hrm 目录）
-- ============================================================
-- 来源：views/hrm/insurance/scheme（列表/表单/删除）、views/hrm/employee/EmployeeInsuranceSchemeForm、
--       employee/detail/EmployeeInsuranceInfoForm（v-hasPermi='hrm:insurance:employee-info:update'）、
--       标准参保只读接口 type-list/project-list（前端无 v-hasPermi，后端 @PreAuthorize 消费）。
--   * 菜单 9180 参保方案：permission=hrm:insurance:scheme:query，component=hrm/insurance/scheme/index
--   * 按钮：scheme create/update/delete；employee-info:update（员工设方案与参保资料编辑共用）；
--           standard type-list/project-list（只读参考数据）
--   * 员工详情页的参保信息读取归 hrm:employee:query（员工域菜单已种）
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9180-9189 段（避开 9100-9173）。
-- 幂等：菜单按 (permission,type,parent_id) 预检；按钮按 (permission,type) 预检。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9180 AS `id`, '参保方案', 'hrm:insurance:scheme:query' AS `permission`, 2 AS `type`, 8 AS `sort`, 9100 AS `parent_id`, 'insurance/scheme' AS `path`, 'ep:umbrella' AS `icon`, 'hrm/insurance/scheme/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9181, '方案新增', 'hrm:insurance:scheme:create', 3, 1, 9180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9182, '方案修改', 'hrm:insurance:scheme:update', 3, 2, 9180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9183, '方案删除', 'hrm:insurance:scheme:delete', 3, 3, 9180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9184, '员工参保修改', 'hrm:insurance:employee-info:update', 3, 4, 9180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9185, '标准类型查询', 'hrm:insurance:standard:type-list', 3, 5, 9180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9186, '标准项目查询', 'hrm:insurance:standard:project-list', 3, 6, 9180, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

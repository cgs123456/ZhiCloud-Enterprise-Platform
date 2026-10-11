-- V179: HRM 薪资业务域菜单与按钮权限种子
-- ============================================================
-- 来源：views/hrm/salary/{employee-info,month-record,slip/send-record} v-hasPermi 与后端 @PreAuthorize 交集。
--   * 菜单 3 页：员工薪资档案、月度工资表、工资条发放
--   * 按钮：employee-info/{query,update,import}、change-record:delete、month-record/{query,create,update,compute,delete}、
--           slip/{query,create,update,delete}
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）；menu id 9237-9250 段（避开 9219-9236 薪资配置）。
-- 幂等：菜单按 (permission,type,parent_id) 预检。
INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9237 AS `id`, '员工薪资' AS `name`, 'hrm:salary:employee-info:query' AS `permission`, 2 AS `type`, 20 AS `sort`, 9100 AS `parent_id`, 'salary/employee-info' AS `path`, 'ep:money' AS `icon`, 'hrm/salary/employee-info/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9238, '薪资修改', 'hrm:salary:employee-info:update', 3, 1, 9237, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9239, '薪资导入', 'hrm:salary:employee-info:import', 3, 2, 9237, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9240, '调薪记录删除', 'hrm:salary:change-record:delete', 3, 3, 9237, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9241, '月度工资表', 'hrm:salary:month-record:query', 2, 21, 9100, 'salary/month-record', 'ep:calendar', 'hrm/salary/month-record/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9242, '工资表新建', 'hrm:salary:month-record:create', 3, 1, 9241, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9243, '工资表修改', 'hrm:salary:month-record:update', 3, 2, 9241, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9244, '工资核算', 'hrm:salary:month-record:compute', 3, 3, 9241, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9245, '工资表删除', 'hrm:salary:month-record:delete', 3, 4, 9241, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9246, '工资条发放', 'hrm:salary:slip:query', 2, 22, 9100, 'salary/slip/send-record', 'ep:message', 'hrm/salary/slip/send-record/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9247, '工资条发放新增', 'hrm:salary:slip:create', 3, 1, 9246, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9248, '工资条修改', 'hrm:salary:slip:update', 3, 2, 9246, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9249, '工资条删除', 'hrm:salary:slip:delete', 3, 3, 9246, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- V177: HRM 薪资配置域菜单与按钮权限 + 薪资字典种子
-- ============================================================
-- 一、菜单（挂 9100 /hrm 目录下，id 9219-9240 段）
--     来源：views/hrm/salary/config/{group,option,tax-rule,change-template,config} v-hasPermi 与后端 @PreAuthorize 交集。
--     权限口径：group/{query,create,update,delete}、option/{query,create,update,delete}、
--               tax-rule/{query,create,update,delete}、change-template/{query,create,update,delete}、config/{query,update}。
-- 二、字典 4 个（本刀配置域消费：计税类型/计税周期/薪资项类型/社保月份类型）。
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）。
-- 幂等：菜单按 (permission,type,parent_id) 预检；字典按 type 预检；字典数据按 (dict_type,value) 预检。
-- MySQL 8.4。

-- ========== 一、菜单 ==========

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9219 AS `id`, '薪资组' AS `name`, 'hrm:salary:group:query' AS `permission`, 2 AS `type`, 15 AS `sort`, 9100 AS `parent_id`, 'salary/config/group' AS `path`, 'ep:coin' AS `icon`, 'hrm/salary/config/group/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9220, '薪资组新增', 'hrm:salary:group:create', 3, 1, 9219, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9221, '薪资组修改', 'hrm:salary:group:update', 3, 2, 9219, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9222, '薪资组删除', 'hrm:salary:group:delete', 3, 3, 9219, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9223, '薪资项', 'hrm:salary:option:query', 2, 16, 9100, 'salary/config/option', 'ep:list', 'hrm/salary/config/option/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9224, '薪资项新增', 'hrm:salary:option:create', 3, 1, 9223, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9225, '薪资项修改', 'hrm:salary:option:update', 3, 2, 9223, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9226, '薪资项删除', 'hrm:salary:option:delete', 3, 3, 9223, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9227, '计税规则', 'hrm:salary:tax-rule:query', 2, 17, 9100, 'salary/config/tax-rule', 'ep:money', 'hrm/salary/config/tax-rule/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9228, '计税规则新增', 'hrm:salary:tax-rule:create', 3, 1, 9227, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9229, '计税规则修改', 'hrm:salary:tax-rule:update', 3, 2, 9227, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9230, '计税规则删除', 'hrm:salary:tax-rule:delete', 3, 3, 9227, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9231, '调薪模板', 'hrm:salary:change-template:query', 2, 18, 9100, 'salary/config/change-template', 'ep:edit-pen', 'hrm/salary/config/change-template/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9232, '调薪模板新增', 'hrm:salary:change-template:create', 3, 1, 9231, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9233, '调薪模板修改', 'hrm:salary:change-template:update', 3, 2, 9231, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9234, '调薪模板删除', 'hrm:salary:change-template:delete', 3, 3, 9231, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9235, '薪资设置', 'hrm:salary:config:query', 2, 19, 9100, 'salary/config/config', 'ep:setting', 'hrm/salary/config/config/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9236, '薪资设置修改', 'hrm:salary:config:update', 3, 1, 9235, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ========== 二、字典类型（4 个） ==========

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 'HRM 薪资计税类型' AS `name`, 'hrm_salary_tax_type' AS `type`, 0 AS `status`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, NULL AS `deleted_time`
  UNION ALL SELECT 'HRM 薪资项类型', 'hrm_salary_option_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 薪资是否', 'hrm_salary_yes_no', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 薪资调整原因', 'hrm_salary_change_reason', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` e WHERE e.`type` = t.`type` AND e.`deleted` = b'0'
);

-- ========== 三、字典数据（对齐前端 constants.ts 枚举） ==========

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  -- 计税类型（HrmSalaryTaxType：1工资薪金 2劳务报酬 3不计税）
  SELECT 1 AS `sort`, '工资薪金所得税' AS `label`, '1' AS `value`, 'hrm_salary_tax_type' AS `dict_type`, 0 AS `status`, 'primary' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 2, '劳务报酬所得税', '2', 'hrm_salary_tax_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '不计税', '3', 'hrm_salary_tax_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 薪资项类型（HrmSalaryOptionType：0减项 1加项 2计算项）
  UNION ALL SELECT 0, '减项', '0', 'hrm_salary_option_type', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1, '加项', '1', 'hrm_salary_option_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '计算项', '2', 'hrm_salary_option_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 是否
  UNION ALL SELECT 1, '是', 'true', 'hrm_salary_yes_no', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '否', 'false', 'hrm_salary_yes_no', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 薪资调整原因（HrmSalaryChangeReason：1年度调薪 2转正调薪 3晋升调薪 4降级调薪 5岗位调整 6其他）
  UNION ALL SELECT 1, '年度调薪', '1', 'hrm_salary_change_reason', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '转正调薪', '2', 'hrm_salary_change_reason', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '晋升调薪', '3', 'hrm_salary_change_reason', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '降级调薪', '4', 'hrm_salary_change_reason', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '岗位调整', '5', 'hrm_salary_change_reason', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '其他', '6', 'hrm_salary_change_reason', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` e
  WHERE e.`dict_type` = t.`dict_type` AND e.`value` = t.`value` AND e.`deleted` = b'0'
);

-- V175: HRM 绩效域菜单与按钮权限 + 绩效字典种子
-- ============================================================
-- 一、菜单（挂 9100 /hrm 目录下，id 9206-9220 段）
--     来源：views/hrm/performance/{plan,assessment,config/assessment-template,config/result-template}
--           v-hasPermi 与后端 @PreAuthorize 双侧交集。
--     权限口径：plan/{query,create,update,delete}、assessment-template/{query,create,update,delete}、
--               result-template/{query,create,update,delete}、archive:delete。
-- 二、字典 7 个（前端 DICT_TYPE 常量逐项对齐）。
-- 写法沿用 V158（NOT EXISTS 预检、只认 deleted=b'0'）。
-- 幂等：菜单按 (permission,type,parent_id) 预检；字典按 type 预检；字典数据按 (dict_type,value) 预检。
-- MySQL 8.4。

-- ========== 一、菜单 ==========

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9206 AS `id`, '绩效计划' AS `name`, 'hrm:performance:plan:query' AS `permission`, 2 AS `type`, 12 AS `sort`, 9100 AS `parent_id`, 'performance/plan' AS `path`, 'ep:trend-charts' AS `icon`, 'hrm/performance/plan/index' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9207, '计划新增', 'hrm:performance:plan:create', 3, 1, 9206, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9208, '计划修改', 'hrm:performance:plan:update', 3, 2, 9206, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9209, '计划删除', 'hrm:performance:plan:delete', 3, 3, 9206, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9210, '考核模板', 'hrm:performance:assessment-template:query', 2, 13, 9100, 'performance/config/assessment-template', 'ep:document-copy', 'hrm/performance/config/assessment-template/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9211, '考核模板新增', 'hrm:performance:assessment-template:create', 3, 1, 9210, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9212, '考核模板修改', 'hrm:performance:assessment-template:update', 3, 2, 9210, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9213, '考核模板删除', 'hrm:performance:assessment-template:delete', 3, 3, 9210, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9214, '结果模板', 'hrm:performance:result-template:query', 2, 14, 9100, 'performance/config/result-template', 'ep:medal', 'hrm/performance/config/result-template/index', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9215, '结果模板新增', 'hrm:performance:result-template:create', 3, 1, 9214, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9216, '结果模板修改', 'hrm:performance:result-template:update', 3, 2, 9214, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9217, '结果模板删除', 'hrm:performance:result-template:delete', 3, 3, 9214, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9218, '绩效归档删除', 'hrm:performance:archive:delete', 3, 4, 9206, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` IN (2,3) AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = t.`type`
           AND (t.`type` = 3 OR m.`parent_id` = t.`parent_id`)) )
);

-- ========== 二、字典类型（7 个） ==========

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 'HRM 绩效计划状态' AS `name`, 'hrm_performance_plan_status' AS `type`, 0 AS `status`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, NULL AS `deleted_time`
  UNION ALL SELECT 'HRM 绩效阶段状态', 'hrm_performance_stage_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 绩效考核阶段处理状态', 'hrm_performance_assessment_stage_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 绩效申诉状态', 'hrm_performance_appeal_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 绩效计分方式', 'hrm_performance_score_calculation', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 绩效上限类型', 'hrm_performance_upper_limit_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 绩效是否', 'hrm_performance_yes_no', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` e WHERE e.`type` = t.`type` AND e.`deleted` = b'0'
);

-- ========== 三、字典数据（对齐前端 constants.ts 枚举） ==========

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  -- 计划状态（HrmPerformancePlanStatus：1草稿 2未开始 3进行中 4已归档 5已终止）
  SELECT 1 AS `sort`, '草稿' AS `label`, '1' AS `value`, 'hrm_performance_plan_status' AS `dict_type`, 0 AS `status`, 'info' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 2, '未开始', '2', 'hrm_performance_plan_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '进行中', '3', 'hrm_performance_plan_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '已归档', '4', 'hrm_performance_plan_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '已终止', '5', 'hrm_performance_plan_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 阶段状态（HrmPerformanceStageType：0未开始 1员工填写 2目标确认 3自评 4他人评分 5结果审核 6结果确认 7申诉确认 8归档 9执行中 10结束）
  UNION ALL SELECT 0, '未开始', '0', 'hrm_performance_stage_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1, '员工填写', '1', 'hrm_performance_stage_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '目标确认', '2', 'hrm_performance_stage_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '自评', '3', 'hrm_performance_stage_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '他人评分', '4', 'hrm_performance_stage_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '结果审核', '5', 'hrm_performance_stage_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '结果确认', '6', 'hrm_performance_stage_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7, '申诉确认', '7', 'hrm_performance_stage_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 8, '归档', '8', 'hrm_performance_stage_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9, '执行中', '9', 'hrm_performance_stage_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 10, '结束', '10', 'hrm_performance_stage_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 考核阶段处理状态（1待处理 2已处理）
  UNION ALL SELECT 1, '待处理', '1', 'hrm_performance_assessment_stage_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '已处理', '2', 'hrm_performance_assessment_stage_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 申诉状态（HrmPerformanceAppealStatus：0无申诉 1待处理 2已通过 3已驳回 4已取消）
  UNION ALL SELECT 0, '无申诉', '0', 'hrm_performance_appeal_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1, '待处理', '1', 'hrm_performance_appeal_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '已通过', '2', 'hrm_performance_appeal_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '已驳回', '3', 'hrm_performance_appeal_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '已取消', '4', 'hrm_performance_appeal_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 计分方式（1加权求和 2算术平均）
  UNION ALL SELECT 1, '加权求和', '1', 'hrm_performance_score_calculation', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '算术平均', '2', 'hrm_performance_score_calculation', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 上限类型（1不限制 2限制总分 3限制单项）
  UNION ALL SELECT 1, '不限制', '1', 'hrm_performance_upper_limit_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '限制总分', '2', 'hrm_performance_upper_limit_type', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '限制单项', '3', 'hrm_performance_upper_limit_type', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 是否
  UNION ALL SELECT 1, '是', 'true', 'hrm_performance_yes_no', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '否', 'false', 'hrm_performance_yes_no', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` e
  WHERE e.`dict_type` = t.`dict_type` AND e.`value` = t.`value` AND e.`deleted` = b'0'
);

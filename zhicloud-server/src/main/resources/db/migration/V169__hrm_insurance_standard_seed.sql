-- V169: HRM 保险域标准参保种子（深圳 440300）+ 方案/项目类型字典
-- ============================================================
-- 来源：views/hrm/insurance/scheme 页面动态加载；前端契约 areaId=440300（深圳），typeCode=URBAN/FLEXIBLE/RURAL。
--   * hr_insurance_standard_type：深圳 3 种参保类型（城镇职工、灵活就业、城乡居民），种子在 tenant 1
--   * hr_insurance_standard_project：城镇职工 6 项（养老/医疗/失业/工伤/生育/公积金），费率为 2026 深圳现行口径近似演示值
--   * 字典 hrm_insurance_scheme_type / hrm_insurance_project_type（方案表单 radio 与列展示消费）
-- 幂等：类型按 (tenant_id,area_id,code)、项目按 (tenant_id,type_id,name)、字典按 (type,value) 预检。
-- MySQL 8.4。

INSERT INTO `hr_insurance_standard_type` (`area_id`, `code`, `name`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
  SELECT 440300 AS `area_id`, 'URBAN' AS `code`, '城镇职工' AS `name`, '深圳社保局官方方案' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, 1 AS `tenant_id`
  UNION ALL SELECT 440300, 'FLEXIBLE', '灵活就业', '深圳社保局官方方案', 'system', NOW(), 'system', NOW(), b'0', 1
  UNION ALL SELECT 440300, 'RURAL', '城乡居民', '深圳社保局官方方案', 'system', NOW(), 'system', NOW(), b'0', 1
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `hr_insurance_standard_type` e
  WHERE e.`tenant_id` = t.`tenant_id` AND e.`area_id` = t.`area_id` AND e.`code` = t.`code` AND e.`deleted` = b'0'
);

-- 标准参保项目（深圳 城镇职工；base=5000 演示基数）
INSERT INTO `hr_insurance_standard_project` (`type_id`, `type_code`, `area_id`, `type`, `name`, `base_amount`, `corporate_rate`, `personal_rate`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT t.* FROM (
  SELECT (SELECT MAX(id) FROM `hr_insurance_standard_type` WHERE `area_id` = 440300 AND `code` = 'URBAN' AND `deleted` = b'0') AS `type_id`, 'URBAN' AS `type_code`, 440300 AS `area_id`, 1 AS `type`, '养老保险' AS `name`, 5000.00 AS `base_amount`, 0.1600 AS `corporate_rate`, 0.0800 AS `personal_rate`, '单位缴纳16%，个人8%' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, 1 AS `tenant_id`
  UNION ALL SELECT (SELECT MAX(id) FROM `hr_insurance_standard_type` WHERE `area_id` = 440300 AND `code` = 'URBAN' AND `deleted` = b'0'), 'URBAN', 440300, 2, '医疗保险', 5000.00, 0.0600, 0.0200, '单位缴纳6%，个人2%', 'system', NOW(), 'system', NOW(), b'0', 1
  UNION ALL SELECT (SELECT MAX(id) FROM `hr_insurance_standard_type` WHERE `area_id` = 440300 AND `code` = 'URBAN' AND `deleted` = b'0'), 'URBAN', 440300, 3, '失业保险', 5000.00, 0.0050, 0.0020, '单位缴纳0.5%，个人0.2%', 'system', NOW(), 'system', NOW(), b'0', 1
  UNION ALL SELECT (SELECT MAX(id) FROM `hr_insurance_standard_type` WHERE `area_id` = 440300 AND `code` = 'URBAN' AND `deleted` = b'0'), 'URBAN', 440300, 4, '工伤保险', 5000.00, 0.0020, 0.0000, '单位缴纳0.2%，个人不缴', 'system', NOW(), 'system', NOW(), b'0', 1
  UNION ALL SELECT (SELECT MAX(id) FROM `hr_insurance_standard_type` WHERE `area_id` = 440300 AND `code` = 'URBAN' AND `deleted` = b'0'), 'URBAN', 440300, 5, '生育保险', 5000.00, 0.0050, 0.0000, '单位缴纳0.5%，个人不缴', 'system', NOW(), 'system', NOW(), b'0', 1
  UNION ALL SELECT (SELECT MAX(id) FROM `hr_insurance_standard_type` WHERE `area_id` = 440300 AND `code` = 'URBAN' AND `deleted` = b'0'), 'URBAN', 440300, 10, '公积金', 5000.00, 0.1200, 0.0500, '单位缴纳12%，个人5%', 'system', NOW(), 'system', NOW(), b'0', 1
) t
WHERE t.`type_id` IS NOT NULL
  AND NOT EXISTS (
  SELECT 1 FROM `hr_insurance_standard_project` e
  WHERE e.`tenant_id` = t.`tenant_id` AND e.`type_id` = t.`type_id` AND e.`name` = t.`name` AND e.`deleted` = b'0'
);

-- 字典类型：方案类型 / 项目类型（方案表单消费）
INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 'HRM 社保方案类型' AS `name`, 'hrm_insurance_scheme_type' AS `type`, 0 AS `status`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, NULL AS `deleted_time`
  UNION ALL SELECT 'HRM 社保项目类型', 'hrm_insurance_project_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` e WHERE e.`type` = t.`type` AND e.`deleted` = b'0'
);

-- 字典数据：方案类型 2 项 + 项目类型 11 项（对齐前端 HrmInsuranceProjectType 常量）
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 1 AS `sort`, '比例' AS `label`, '1' AS `value`, 'hrm_insurance_scheme_type' AS `dict_type`, 0 AS `status`, 'primary' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 2, '固定金额', '2', 'hrm_insurance_scheme_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1, '养老保险', '1', 'hrm_insurance_project_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '医疗保险', '2', 'hrm_insurance_project_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '失业保险', '3', 'hrm_insurance_project_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 4, '工伤保险', '4', 'hrm_insurance_project_type', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 5, '生育保险', '5', 'hrm_insurance_project_type', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 6, '补充大病医疗', '6', 'hrm_insurance_project_type', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 7, '补充养老', '7', 'hrm_insurance_project_type', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 8, '残保金', '8', 'hrm_insurance_project_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9, '社保自定义', '9', 'hrm_insurance_project_type', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 10, '公积金', '10', 'hrm_insurance_project_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 11, '公积金自定义', '11', 'hrm_insurance_project_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` e
  WHERE e.`dict_type` = t.`dict_type` AND e.`value` = t.`value` AND e.`deleted` = b'0'
);

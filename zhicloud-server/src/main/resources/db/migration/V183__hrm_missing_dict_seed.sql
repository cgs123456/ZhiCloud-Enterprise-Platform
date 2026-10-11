-- V183: HRM 补 4 个缺失字典（前端 HRM 视图引用但库中无 → 「可见但下拉/标签为空」）
-- ============================================================
-- 来源：脚本比对前端 views/hrm/** 引用的 DICT_TYPE.* 与库中 system_dict_type（type LIKE 'hrm_%'），
--       定位 4 个被消费但未种的字典（与 §12.183 FMS 同口径的「可见但选项不可用」缺口）：
--   * hrm_insurance_emp_status：月度社保参保状态（0停止 1正常），dict-tag 消费
--   * hrm_salary_change_type：薪资调整记录类型（1定薪 2调薪），下拉 + dict-tag 消费
--   * hrm_salary_change_record_status：薪资调整记录状态（0待生效 1已生效 2已取消），dict-tag 消费
--   * hrm_salary_slip_read_status：工资条查看状态（0未读 1已读），下拉 + dict-tag 消费
-- 值域对齐前端 constants.ts（HrmInsuranceEmployeeStatus / HrmSalaryRecordType /
-- HrmSalaryChangeRecordStatus / 工资条 readStatus）。
-- 幂等：字典按 type 预检；字典数据按 (dict_type,value) 预检。
-- MySQL 8.4。

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 'HRM 社保参保状态' AS `name`, 'hrm_insurance_emp_status' AS `type`, 0 AS `status`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, NULL AS `deleted_time`
  UNION ALL SELECT 'HRM 薪资调整类型', 'hrm_salary_change_type', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 薪资调整记录状态', 'hrm_salary_change_record_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
  UNION ALL SELECT 'HRM 工资条查看状态', 'hrm_salary_slip_read_status', 0, 'system', NOW(), 'system', NOW(), b'0', NULL
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_type` e WHERE e.`type` = t.`type` AND e.`deleted` = b'0'
);

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  -- 社保参保状态（HrmInsuranceEmployeeStatus：0停止 1正常）
  SELECT 1 AS `sort`, '停止参保' AS `label`, '0' AS `value`, 'hrm_insurance_emp_status' AS `dict_type`, 0 AS `status`, 'info' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 2, '正常参保', '1', 'hrm_insurance_emp_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 薪资调整类型（HrmSalaryRecordType：1定薪 2调薪）
  UNION ALL SELECT 1, '定薪', '1', 'hrm_salary_change_type', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '调薪', '2', 'hrm_salary_change_type', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 薪资调整记录状态（HrmSalaryChangeRecordStatus：0待生效 1已生效 2已取消）
  UNION ALL SELECT 1, '待生效', '0', 'hrm_salary_change_record_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '已生效', '1', 'hrm_salary_change_record_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 3, '已取消', '2', 'hrm_salary_change_record_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  -- 工资条查看状态（0未读 1已读）
  UNION ALL SELECT 1, '未读', '0', 'hrm_salary_slip_read_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 2, '已读', '1', 'hrm_salary_slip_read_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_dict_data` e
  WHERE e.`dict_type` = t.`dict_type` AND e.`value` = t.`value` AND e.`deleted` = b'0'
);

-- V159: FMS 本刀（§12.181 的 8 个 config 菜单）页面用到的数据字典种子
-- 说明：config/subject、initial-balance、closing、report 等页面也引用字典，但那些菜单本刀未种
--       （后端未实现），字典种子跟随各自后端增量一起落库，避免「种了字典却没有页面在用」的无主数据。
--       本条只种两个本刀页面真实用到的字典：
--         fms_account_user_level   —— 账套成员表单（主管/查看者/会计），取值对齐前端 constants.ts
--                                      与后端 FmsAccountUserDO.LEVEL_*（1=主管 2=查看者 3=会计）。
--         fms_finance_indicator_type —— 财务指标「取数报表」下拉，取值对齐 FMS_REPORT_TYPE
--                                       （1=资产负债表 2=利润表；现金流量表无预置指标，留待报表域增量）。
-- 幂等：system_dict_type 按 (type) 预检、system_dict_data 按 (dict_type, value) 预检，只认 deleted = b'0'。
-- id 号段 1062000+（避开既有 1061xxx）。
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 1062001 AS `id`, 'FMS 账套成员权限级别' AS `name`, 'fms_account_user_level' AS `type`, 0 AS `status`, 'FMS 账套成员权限级别' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, '1970-01-01 00:00:00' AS `deleted_time`
  UNION ALL SELECT 1062002, 'FMS 财务指标取数报表类型', 'fms_finance_indicator_type', 0, 'FMS 财务指标取数报表类型', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` d WHERE d.`deleted` = b'0' AND d.`type` = t.`type`);

INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 1062001 AS `id`, 1 AS `sort`, '主管' AS `label`, '1' AS `value`, 'fms_account_user_level' AS `dict_type`, 0 AS `status`, 'primary' AS `color_type`, '' AS `css_class`, '账套主管' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 1062002, 2, '查看者', '2', 'fms_account_user_level', 0, 'info', '', '只读成员', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062003, 3, '会计', '3', 'fms_account_user_level', 0, 'success', '', '记账成员', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062011, 1, '资产负债表', '1', 'fms_finance_indicator_type', 0, 'primary', '', '取数自资产负债表', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1062012, 2, '利润表', '2', 'fms_finance_indicator_type', 0, 'success', '', '取数自利润表', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` d WHERE d.`deleted` = b'0' AND d.`dict_type` = t.`dict_type` AND d.`value` = t.`value`);

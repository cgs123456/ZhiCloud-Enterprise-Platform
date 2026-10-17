-- V204: MES APS/MRP 状态与优先级字典种子（§12.246）
-- ==========================================================
-- 说明：MES APS 排产计划状态/优先级与 MRP 计划状态此前**没有任何 Flyway 字典种子**
--       （库内 72 个 mes_% 字典类型中不含这三类，见 §12.240.5 / §12.243.5 登记），
--       前端两页因此只能用页内映射渲染状态与优先级。本迁移补齐这三类字典。
-- 覆盖：3 个类型 + 11 项数据。
-- 取值：**逐条从后端枚举源码解析生成**（脚本 Temp/zc_gen_v204_dict_sql.py，并断言
--       条数 5/3/3），不手抄，避免注释/字典与实现脱节（§12.234.6 同类教训）。
-- 幂等：system_dict_type 按 (type)、system_dict_data 按 (dict_type,value) 预检，
--       只认 deleted = b'0'；已存在则整条跳过，不覆盖既有 label/color。
-- id 号段：type=1064001+ / data=1064101+（库内现最大 type=1063019、data=1063179；
--       V202 已占 1063001/1063101 段）。

-- 一、字典类型
INSERT INTO `system_dict_type` (`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
SELECT t.* FROM (
  SELECT 1064001 AS `id`, 'MES APS 排产计划状态' AS `name`, 'mes_pro_aps_plan_status' AS `type`, 0 AS `status`, 'MesProApsPlanStatusEnum' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`, '1970-01-01 00:00:00' AS `deleted_time`
  UNION ALL SELECT 1064002, 'MES APS 排产计划优先级', 'mes_pro_aps_plan_priority', 0, 'MesProApsPlanPriorityEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
  UNION ALL SELECT 1064003, 'MES MRP 计划状态', 'mes_pro_mrp_plan_status', 0, 'MesProMrpPlanStatusEnum', 'system', NOW(), 'system', NOW(), b'0', '1970-01-01 00:00:00'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_type` d WHERE d.`deleted` = b'0' AND d.`type` = t.`type`);

-- 二、字典数据
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 1064101 AS `id`, 1 AS `sort`, '草稿' AS `label`, '0' AS `value`, 'mes_pro_aps_plan_status' AS `dict_type`, 0 AS `status`, 'info' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 1064102, 2, '已确认', '1', 'mes_pro_aps_plan_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064103, 3, '执行中', '2', 'mes_pro_aps_plan_status', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064104, 4, '已完成', '3', 'mes_pro_aps_plan_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064105, 5, '已取消', '4', 'mes_pro_aps_plan_status', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064106, 1, '高', '1', 'mes_pro_aps_plan_priority', 0, 'danger', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064107, 2, '中', '2', 'mes_pro_aps_plan_priority', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064108, 3, '低', '3', 'mes_pro_aps_plan_priority', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064109, 1, '草稿', '0', 'mes_pro_mrp_plan_status', 0, 'info', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064110, 2, '已计算', '1', 'mes_pro_mrp_plan_status', 0, 'primary', '', '', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 1064111, 3, '已确认', '2', 'mes_pro_mrp_plan_status', 0, 'success', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` d WHERE d.`deleted` = b'0' AND d.`dict_type` = t.`dict_type` AND d.`value` = t.`value`);

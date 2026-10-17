-- V207: MES MRP「转采购/生产建议」字典增量（§12.262）
-- ==========================================================
-- 说明：C4「MRP 转采购/生产建议」新增两个枚举终值：
--       1. MesProMrpPlanStatusEnum.CONVERTED(3, "已转建议")
--       2. MesProWorkOrderSourceTypeEnum.MRP(3, "MRP 计划")
--       两类型由 V204 建立；前端 MRP 页状态列与工单来源列按字典渲染，
--       需补对应数据行（延续 V204「逐条对齐后端枚举源码」纪律）。
-- 幂等：system_dict_data 按 (dict_type,value) 预检，只认 deleted = b'0'；
--       已存在则整条跳过，不覆盖既有 label/color。
-- id 号段：data=1064112+（V204 已占 1064101..1064111）。
-- 注意：纯字典数据，无表结构变更（H2/MySQL 双栈兼容；H2 侧无本迁移对应表改动）。
-- 回滚预案（如需）：DELETE FROM system_dict_data WHERE id IN (1064112,1064113)；
--       旧代码/旧前端不依赖此数据，无功能回退风险。
-- ==========================================================

-- 一、字典数据（2 行）
INSERT INTO `system_dict_data` (`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT t.* FROM (
  SELECT 1064112 AS `id`, 4 AS `sort`, '已转建议' AS `label`, '3' AS `value`, 'mes_pro_mrp_plan_status' AS `dict_type`, 0 AS `status`, 'success' AS `color_type`, '' AS `css_class`, '' AS `remark`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 1064113, 3, 'MRP 计划', '3', 'mes_pro_work_order_source_type', 0, 'warning', '', '', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (SELECT 1 FROM `system_dict_data` d WHERE d.`deleted` = b'0' AND d.`dict_type` = t.`dict_type` AND d.`value` = t.`value`);

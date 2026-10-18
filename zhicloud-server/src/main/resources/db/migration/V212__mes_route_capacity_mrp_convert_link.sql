-- ============================================================
-- V212：MES 工艺路线产能表 + MRP 结果行→工单回链表（D4，ADR 270；台账 §12.270）
-- ============================================================
-- 依据：§12.263.5「时长估算并入工艺路线产能表」+ §12.262.5「MRP 结果行 → 工单的来源回链表」
-- （重建矩阵 D4，Temp/zc_adr_267.md §0 / Temp/zc_adr_270.md §2）。本迁移为纯增两表、零存量改动：
--   1. mes_pro_route_capacity（工艺路线产能表）：工艺路线维度产能/效率，APS 时长估算产能输入
--      优先级＝路线产能 → 工位平均 → 1.0（capacity 0 / efficiency ≤0 视为未配置回退）。
--   2. mes_pro_mrp_convert_link（MRP 结果行→工单回链表）：转建议逐行落链、行级追溯；
--      历史已转计划无链可回溯、不回填（登记）。
-- 幂等：CREATE TABLE IF NOT EXISTS（重跑安全）。
-- 存量安全：两表均为新增，零存量行改动、无键变更。
-- 回滚预案（如需）：删除两表即可（纯新表、无下游外键；转换留痕仍在 mes_pro_work_order）。
-- ============================================================

-- 1. 工艺路线产能表
CREATE TABLE IF NOT EXISTS `mes_pro_route_capacity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `route_id` bigint NOT NULL COMMENT '工艺路线编号（mes_pro_route.id）',
  `capacity` decimal(12,2) NOT NULL DEFAULT 0 COMMENT '产能（件/小时；0 = 未配置，APS 时长估算回退工位平均产能）',
  `efficiency` decimal(6,3) NOT NULL DEFAULT 1 COMMENT '效率系数（>0；≤0/未配置按 1.0 处理）',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_route_capacity_route` (`tenant_id`, `route_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES 工艺路线产能表（D4：APS 时长估算产能输入）';

-- 2. 自检 1：产能表在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V212_ROUTE_CAPACITY_TABLE_PASS'
            ELSE CONCAT('V212_ROUTE_CAPACITY_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'mes_pro_route_capacity';

-- 3. MRP 结果行→工单回链表
CREATE TABLE IF NOT EXISTS `mes_pro_mrp_convert_link` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `plan_id` bigint NOT NULL COMMENT 'MRP 计划编号（mes_pro_mrp_plan.id）',
  `result_id` bigint NOT NULL COMMENT 'MRP 结果行编号（mes_pro_mrp_result.id）',
  `work_order_id` bigint NOT NULL COMMENT '生产工单编号（mes_pro_work_order.id）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mes_pro_mrp_convert_link_result` (`tenant_id`, `result_id`),
  UNIQUE KEY `uk_mes_pro_mrp_convert_link_order` (`tenant_id`, `work_order_id`),
  KEY `idx_mes_pro_mrp_convert_link_plan` (`tenant_id`, `plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES MRP 结果行→工单回链表（D4：转建议逐行落链）';

-- 4. 自检 2：回链表在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V212_CONVERT_LINK_TABLE_PASS'
            ELSE CONCAT('V212_CONVERT_LINK_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'mes_pro_mrp_convert_link';

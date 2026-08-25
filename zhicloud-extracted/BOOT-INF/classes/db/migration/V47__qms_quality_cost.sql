-- ============================================================
-- V47: QMS 质量成本分析（PAIF 模型）
--
-- 覆盖 PAIF 四类质量成本：
--   PREVENTION         预防成本
--   APPRAISAL          鉴定成本
--   INTERNAL_FAILURE   内部故障
--   EXTERNAL_FAILURE   外部故障
--
-- 支持按年度/月份汇总、趋势分析、累计统计，并可关联 8D/NCR/CAPA 业务记录。
-- ============================================================

CREATE TABLE IF NOT EXISTS `qms_quality_cost` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `cost_type` VARCHAR(32) NOT NULL COMMENT '成本类型（PREVENTION 预防成本 / APPRAISAL 鉴定成本 / INTERNAL_FAILURE 内部故障 / EXTERNAL_FAILURE 外部故障）',
    `cost_category` VARCHAR(64) NOT NULL COMMENT '成本类别（如：培训费/检测设备费/返工费/退货处理费）',
    `cost_item` VARCHAR(200) NOT NULL COMMENT '成本项目',
    `amount` DECIMAL(20,4) NOT NULL COMMENT '金额',
    `period_year` INT NOT NULL COMMENT '年度',
    `period_month` INT NOT NULL COMMENT '月份（1-12）',
    `related_id` BIGINT DEFAULT NULL COMMENT '关联业务 ID（如 8D 报告 ID/NCR ID/CAPA ID）',
    `related_type` VARCHAR(32) DEFAULT NULL COMMENT '关联业务类型（EIGHT_D/NCR/CAPA）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_qc_type` (`cost_type`),
    KEY `idx_qc_period` (`period_year`, `period_month`),
    KEY `idx_qc_related` (`related_type`, `related_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 质量成本表（PAIF 模型）';
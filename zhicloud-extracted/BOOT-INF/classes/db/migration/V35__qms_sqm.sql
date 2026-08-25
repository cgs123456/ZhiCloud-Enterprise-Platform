-- ============================================================
-- V35: QMS 供应商质量管理 SQM
--
-- 包含：供应商评级（基于 PPM/交期/质量）、SCAR（供应商纠正措施请求）、供应商审核
-- ============================================================

-- ----------------------------
-- 供应商评级表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qms_supplier_rating` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `rating_no` VARCHAR(64) NOT NULL COMMENT '评级编号',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商 ID',
    `supplier_name` VARCHAR(255) DEFAULT NULL COMMENT '供应商名称',
    `rating_period` VARCHAR(32) DEFAULT NULL COMMENT '评级周期（如 2024-Q1）',
    `ppm` INT DEFAULT NULL COMMENT 'PPM 缺陷率（百万分之缺陷数）',
    `on_time_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '交期达成率（百分比）',
    `quality_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '质量合格率（百分比）',
    `grade` VARCHAR(2) DEFAULT NULL COMMENT '供应商等级（A 优秀 B 合格 C 待改进 D 不合格）',
    `rating_date` DATE DEFAULT NULL COMMENT '评级日期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_rating_no` (`rating_no`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_grade` (`grade`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 供应商评级表';

-- ----------------------------
-- SCAR 供应商纠正措施请求表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qms_scar` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `scar_no` VARCHAR(64) NOT NULL COMMENT 'SCAR 单号',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商 ID',
    `supplier_name` VARCHAR(255) DEFAULT NULL COMMENT '供应商名称',
    `product_id` BIGINT DEFAULT NULL COMMENT '产品 ID',
    `product_name` VARCHAR(255) DEFAULT NULL COMMENT '产品名称',
    `defect_description` TEXT COMMENT '缺陷描述',
    `root_cause` TEXT COMMENT '根本原因',
    `corrective_action` TEXT COMMENT '纠正措施',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 已发起 20 处理中 30 已关闭）',
    `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_scar_no` (`scar_no`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS SCAR 供应商纠正措施请求表';

-- ----------------------------
-- 供应商审核表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qms_supplier_audit` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `audit_no` VARCHAR(64) NOT NULL COMMENT '审核编号',
    `audit_name` VARCHAR(255) NOT NULL COMMENT '审核名称',
    `supplier_id` BIGINT NOT NULL COMMENT '供应商 ID',
    `supplier_name` VARCHAR(255) DEFAULT NULL COMMENT '供应商名称',
    `audit_type` TINYINT DEFAULT NULL COMMENT '审核类型（10 首次审核 20 年度审核 30 跟踪审核 40 专项审核）',
    `planned_date` DATE DEFAULT NULL COMMENT '计划日期',
    `actual_date` DATE DEFAULT NULL COMMENT '实际日期',
    `auditor` VARCHAR(128) DEFAULT NULL COMMENT '审核员',
    `conclusion` TINYINT DEFAULT NULL COMMENT '审核结论（10 合格 20 有条件合格 30 不合格）',
    `audit_report` TEXT COMMENT '审核报告',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 已计划 20 审核中 30 已完成 40 已取消）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_audit_no` (`audit_no`),
    KEY `idx_supplier_id` (`supplier_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 供应商审核表';
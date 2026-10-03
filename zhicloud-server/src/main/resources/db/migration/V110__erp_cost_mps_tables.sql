-- ============================================================
-- V110: ERP cost accounting and MPS tables
-- Source reviewed from sql/mysql/erp_cost.sql and sql/mysql/erp_mps.sql.
-- The source scripts were not part of the Flyway migration chain.
-- No mock/business rows are inserted by this migration.
-- All ids use AUTO_INCREMENT because MyBatis-Plus IdType=AUTO is configured.
-- Business unique keys are tenant-scoped to prevent cross-tenant collisions.
-- ============================================================

CREATE TABLE IF NOT EXISTS `erp_cost_item` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '成本项目编码',
    name VARCHAR(128) NOT NULL COMMENT '成本项目名称',
    type TINYINT NOT NULL COMMENT '类型（10 材料 20 人工 30 制造费用 40 外协 50 其他）',
    calculation_method VARCHAR(64) COMMENT '计算方法（如：标准成本法/实际成本法/加权平均法）',
    is_standard TINYINT DEFAULT 0 COMMENT '是否标准成本（0 否 1 是）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 成本项目表';

CREATE TABLE IF NOT EXISTS `erp_standard_cost` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    product_code VARCHAR(64) COMMENT '产品编码',
    product_name VARCHAR(128) COMMENT '产品名称',
    cost_item_id BIGINT NOT NULL COMMENT '成本项目 ID',
    standard_cost DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '标准成本',
    effective_date DATE NOT NULL COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已生效 30 已失效）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_product_cost_item` (`tenant_id`, `product_id`, `cost_item_id`, `effective_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 标准成本表';

CREATE TABLE IF NOT EXISTS `erp_actual_cost` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    product_code VARCHAR(64) COMMENT '产品编码',
    cost_period VARCHAR(8) NOT NULL COMMENT '成本期间（yyyymm）',
    cost_item_id BIGINT NOT NULL COMMENT '成本项目 ID',
    actual_cost DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '实际成本总额',
    actual_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '实际产量/数量',
    unit_cost DECIMAL(20,4) DEFAULT 0 COMMENT '单位成本',
    variance_amount DECIMAL(20,4) DEFAULT 0 COMMENT '差异金额',
    variance_rate DECIMAL(10,4) DEFAULT 0 COMMENT '差异率(%)',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_product_period_cost` (`tenant_id`, `product_id`, `cost_period`, `cost_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 实际成本表';

CREATE TABLE IF NOT EXISTS `erp_cost_variance` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    cost_period VARCHAR(8) NOT NULL COMMENT '成本期间（yyyymm）',
    cost_item_id BIGINT NOT NULL COMMENT '成本项目 ID',
    standard_cost DECIMAL(20,4) DEFAULT 0 COMMENT '标准成本',
    actual_cost DECIMAL(20,4) DEFAULT 0 COMMENT '实际成本',
    variance_amount DECIMAL(20,4) DEFAULT 0 COMMENT '差异金额（实际 - 标准）',
    variance_rate DECIMAL(10,4) DEFAULT 0 COMMENT '差异率(%)',
    variance_type TINYINT DEFAULT 10 COMMENT '差异类型（10 有利差异 20 不利差异）',
    analysis_remark VARCHAR(1000) COMMENT '差异分析说明',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_product_period` (`tenant_id`, `product_id`, `cost_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 成本差异表';

CREATE TABLE IF NOT EXISTS `erp_work_order_cost` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT NOT NULL COMMENT '工单 ID',
    work_order_code VARCHAR(64) COMMENT '工单编码',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    cost_period VARCHAR(8) NOT NULL COMMENT '成本期间（yyyymm）',
    material_cost DECIMAL(20,4) DEFAULT 0 COMMENT '材料成本',
    labor_cost DECIMAL(20,4) DEFAULT 0 COMMENT '人工成本',
    overhead_cost DECIMAL(20,4) DEFAULT 0 COMMENT '制造费用',
    outsourcing_cost DECIMAL(20,4) DEFAULT 0 COMMENT '外协成本',
    total_cost DECIMAL(20,4) DEFAULT 0 COMMENT '总成本',
    quantity DECIMAL(20,4) DEFAULT 0 COMMENT '工单产量',
    unit_cost DECIMAL(20,4) DEFAULT 0 COMMENT '单位成本',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_work_order` (`tenant_id`, `work_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 工单成本归集表';

CREATE TABLE IF NOT EXISTS `erp_mps_plan` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_no VARCHAR(64) NOT NULL COMMENT '计划编号',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    product_code VARCHAR(128) COMMENT '产品编码（冗余）',
    product_name VARCHAR(255) COMMENT '产品名称（冗余）',
    plan_period VARCHAR(8) NOT NULL COMMENT '计划周期（yyyyMM）',
    plan_type TINYINT NOT NULL DEFAULT 10 COMMENT '计划类型（10 月度 20 季度 30 年度）',
    demand_date DATE COMMENT '需求日期',
    planned_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '计划数量',
    planned_finish_date DATE COMMENT '计划完工日期',
    source TINYINT NOT NULL DEFAULT 10 COMMENT '来源（10 销售订单 20 预测 30 安全库存）',
    source_order_id BIGINT COMMENT '来源订单 ID',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 已确认 30 已下发 MRP 40 已关闭）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_plan_no` (`tenant_id`, `plan_no`),
    KEY `idx_tenant_product` (`tenant_id`, `product_id`),
    KEY `idx_tenant_plan_period` (`tenant_id`, `plan_period`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_source` (`tenant_id`, `source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 主生产计划主表';

CREATE TABLE IF NOT EXISTS `erp_mps_plan_detail` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '主生产计划 ID',
    period_start DATE COMMENT '时段开始日期',
    period_end DATE COMMENT '时段结束日期',
    gross_requirement DECIMAL(20,4) DEFAULT 0 COMMENT '毛需求',
    scheduled_receipt DECIMAL(20,4) DEFAULT 0 COMMENT '计划接收',
    projected_available_balance DECIMAL(20,4) DEFAULT 0 COMMENT '预计可用库存',
    planned_order_receipt DECIMAL(20,4) DEFAULT 0 COMMENT '计划订单接收',
    planned_order_release DECIMAL(20,4) DEFAULT 0 COMMENT '计划订单下达',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_plan` (`tenant_id`, `plan_id`),
    KEY `idx_tenant_period` (`tenant_id`, `period_start`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 主生产计划明细表';

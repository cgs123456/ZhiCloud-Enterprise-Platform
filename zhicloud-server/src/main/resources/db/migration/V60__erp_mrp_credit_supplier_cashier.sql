-- ============================================================
-- V60: ERP 企业级核心功能补齐 - MRP / 信用控制 / 供应商评估 / 出纳
--
-- 新增 6 张表：
--   erp_mrp_plan                  MRP 物料需求计划主表
--   erp_mrp_result                MRP 物料需求计划结果
--   erp_credit_limit              客户信用额度
--   erp_supplier_evaluation       供应商评估主表
--   erp_supplier_evaluation_item  供应商评估指标项
--   erp_cashier                   出纳单
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. MRP 物料需求计划主表
CREATE TABLE IF NOT EXISTS erp_mrp_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(64) NOT NULL COMMENT '计划编号',
    plan_name VARCHAR(200) NOT NULL COMMENT '计划名称',
    plan_date DATE COMMENT '计划日期',
    mps_plan_id BIGINT COMMENT '关联 MPS 主生产计划编号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态 10草稿/20已计算/30已确认/40已关闭',
    total_demand_count DECIMAL(20,4) DEFAULT 0 COMMENT '总需求量',
    total_purchase_count DECIMAL(20,4) DEFAULT 0 COMMENT '总采购量',
    total_produce_count DECIMAL(20,4) DEFAULT 0 COMMENT '总生产量',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_no (no, tenant_id),
    KEY idx_mps_plan_id (mps_plan_id),
    KEY idx_status (status),
    KEY idx_plan_date (plan_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 物料需求计划主表';

-- 2. MRP 物料需求计划结果
CREATE TABLE IF NOT EXISTS erp_mrp_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    plan_id BIGINT NOT NULL COMMENT 'MRP 计划编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称（冗余）',
    demand_type TINYINT NOT NULL DEFAULT 10 COMMENT '需求类型 10独立需求/20相关需求',
    demand_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '需求量',
    stock_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '库存可用量',
    net_demand DECIMAL(20,4) DEFAULT 0 COMMENT '净需求',
    planned_order_type TINYINT COMMENT '计划订单类型 10采购/20生产',
    planned_order_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '计划订单量',
    planned_delivery_date DATE COMMENT '计划交付日',
    supplier_id BIGINT COMMENT '供应商编号',
    workshop_id BIGINT COMMENT '生产车间编号',
    source_product_id BIGINT COMMENT '上层产品编号（BOM 父件）',
    source_quantity DECIMAL(20,4) COMMENT '上层需求量',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_plan_id (plan_id),
    KEY idx_product_id (product_id),
    KEY idx_demand_type (demand_type),
    KEY idx_planned_order_type (planned_order_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 物料需求计划结果';

-- 3. 客户信用额度
CREATE TABLE IF NOT EXISTS erp_credit_limit (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    customer_id BIGINT NOT NULL COMMENT '客户编号',
    credit_limit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '信用额度',
    used_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '已用额度',
    available_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '可用额度',
    overdue_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '逾期金额',
    warning_ratio DECIMAL(6,2) DEFAULT 80.00 COMMENT '预警比例（默认 80%）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态 10正常/20预警/30冻结',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_id (customer_id, tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 客户信用额度';

-- 4. 供应商评估主表
CREATE TABLE IF NOT EXISTS erp_supplier_evaluation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    supplier_id BIGINT NOT NULL COMMENT '供应商编号',
    evaluation_period VARCHAR(10) NOT NULL COMMENT '评估周期 yyyyMM',
    quality_score DECIMAL(8,2) COMMENT '质量评分',
    delivery_score DECIMAL(8,2) COMMENT '交期评分',
    price_score DECIMAL(8,2) COMMENT '价格评分',
    service_score DECIMAL(8,2) COMMENT '服务评分',
    total_score DECIMAL(8,2) COMMENT '综合评分',
    grade VARCHAR(2) COMMENT '等级 A/B/C/D',
    evaluator VARCHAR(64) COMMENT '评估人',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_supplier_period (supplier_id, evaluation_period, tenant_id),
    KEY idx_evaluation_period (evaluation_period),
    KEY idx_grade (grade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 供应商评估';

-- 5. 供应商评估指标项
CREATE TABLE IF NOT EXISTS erp_supplier_evaluation_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    evaluation_id BIGINT NOT NULL COMMENT '评估编号',
    indicator VARCHAR(200) NOT NULL COMMENT '指标名称',
    score DECIMAL(8,2) COMMENT '得分',
    weight DECIMAL(6,2) COMMENT '权重（百分比）',
    weighted_score DECIMAL(8,2) COMMENT '加权得分',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_evaluation_id (evaluation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 供应商评估指标项';

-- 6. 出纳单
CREATE TABLE IF NOT EXISTS erp_cashier (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(64) NOT NULL COMMENT '出纳单号',
    cashier_type TINYINT NOT NULL COMMENT '出纳类型 10收款/20付款/30内部转账',
    bank_account_id BIGINT NOT NULL COMMENT '银行账户编号',
    counterparty_name VARCHAR(200) COMMENT '对方名称',
    counterparty_account VARCHAR(64) COMMENT '对方账号',
    counterparty_bank VARCHAR(200) COMMENT '对方开户行',
    amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '金额',
    payment_method TINYINT COMMENT '支付方式 10现金/20转账/30支票/40网银',
    payment_date DATE COMMENT '支付日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态 10待处理/20已提交银行/30已到账/40已退回',
    bank_serial_no VARCHAR(64) COMMENT '银行流水号（网银直联返回）',
    business_order_no VARCHAR(64) COMMENT '关联业务单号',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_no (no, tenant_id),
    KEY idx_cashier_type (cashier_type),
    KEY idx_bank_account_id (bank_account_id),
    KEY idx_status (status),
    KEY idx_payment_date (payment_date),
    KEY idx_business_order_no (business_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 出纳单';

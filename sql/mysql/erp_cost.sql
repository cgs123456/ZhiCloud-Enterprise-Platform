-- ======================== ERP 成本核算模块建表脚本 ========================
-- 作者：zhicloud
-- 说明：覆盖成本项目 / 标准成本 / 实际成本 / 成本差异 / 工单成本归集
-- 注意：所有表均包含 IF NOT EXISTS，可重复执行

-- ----------------------------
-- 1. 成本项目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_cost_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 成本项目表';

-- ----------------------------
-- 2. 标准成本表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_standard_cost (
    id BIGINT PRIMARY KEY COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 标准成本表';

-- ----------------------------
-- 3. 实际成本表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_actual_cost (
    id BIGINT PRIMARY KEY COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 实际成本表';

-- ----------------------------
-- 4. 成本差异表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_cost_variance (
    id BIGINT PRIMARY KEY COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 成本差异表';

-- ----------------------------
-- 5. 工单成本归集表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_work_order_cost (
    id BIGINT PRIMARY KEY COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 工单成本归集表';

-- ============================================================
-- 索引
-- ============================================================
CREATE UNIQUE INDEX uk_code ON erp_cost_item(code);
CREATE UNIQUE INDEX uk_product_cost_item ON erp_standard_cost(product_id, cost_item_id, effective_date);
CREATE UNIQUE INDEX uk_product_period_cost ON erp_actual_cost(product_id, cost_period, cost_item_id);
CREATE INDEX idx_work_order_id ON erp_work_order_cost(work_order_id);
CREATE INDEX idx_product_period ON erp_actual_cost(product_id, cost_period);
CREATE INDEX idx_variance_product_period ON erp_cost_variance(product_id, cost_period);

-- ============================================================
-- 字典数据（仅给出 SQL 占位，具体字典管理通过系统字典管理界面维护）
-- 字典类型：erp_cost_item_type / erp_standard_cost_status / erp_variance_type
-- ============================================================
-- erp_cost_item_type: 10 材料 / 20 人工 / 30 制造费用 / 40 外协 / 50 其他
-- erp_standard_cost_status: 10 草稿 / 20 已生效 / 30 已失效
-- erp_variance_type: 10 有利差异 / 20 不利差异

-- ======================== ERP MPS 主生产计划模块建表脚本 ========================
-- 作者：yudao
-- 说明：主生产计划主表 / 主生产计划明细表
-- 注意：所有表均包含 IF NOT EXISTS，可重复执行

-- ----------------------------
-- 1. 主生产计划主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_mps_plan (
    id BIGINT NOT NULL COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id)
) COMMENT='ERP 主生产计划主表';

-- ----------------------------
-- 2. 主生产计划明细表（按需求时界/计划时界分时段）
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_mps_plan_detail (
    id BIGINT NOT NULL COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id)
) COMMENT='ERP 主生产计划明细表';

-- ============================================================
-- 索引
-- ============================================================
CREATE UNIQUE INDEX uk_mps_plan_no ON erp_mps_plan(plan_no);
CREATE INDEX idx_mps_product_id ON erp_mps_plan(product_id);
CREATE INDEX idx_mps_plan_period ON erp_mps_plan(plan_period);
CREATE INDEX idx_mps_plan_status ON erp_mps_plan(status);
CREATE INDEX idx_mps_plan_source ON erp_mps_plan(source);
CREATE INDEX idx_mps_plan_id ON erp_mps_plan_detail(plan_id);
CREATE INDEX idx_mps_plan_detail_period ON erp_mps_plan_detail(period_start, period_end);

-- ============================================================
-- 字典数据（仅给出 SQL 占位，具体字典管理通过系统字典管理界面维护）
-- 字典类型：
--   erp_mps_plan_type:    10 月度 / 20 季度 / 30 年度
--   erp_mps_plan_status:  10 草稿 / 20 已确认 / 30 已下发 MRP / 40 已关闭
--   erp_mps_plan_source:  10 销售订单 / 20 预测 / 30 安全库存
-- ============================================================

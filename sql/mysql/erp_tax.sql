-- ======================== ERP 税务管理模块建表脚本 ========================
-- 作者：zhicloud
-- 说明：覆盖税率 / 发票主表 / 发票明细
-- 注意：所有表均包含 IF NOT EXISTS，可重复执行

-- ----------------------------
-- 1. 税率表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_tax_rate (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '税率编码',
    name VARCHAR(128) NOT NULL COMMENT '税率名称',
    rate_type TINYINT NOT NULL COMMENT '税率类型（10 增值税 20 消费税 30 附加税）',
    rate DECIMAL(10,6) NOT NULL COMMENT '税率（小数，如 0.13 表示 13%）',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认（0 否 1 是）',
    effective_date DATE COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 税率表';

-- ----------------------------
-- 2. 发票主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_tax_invoice (
    id BIGINT PRIMARY KEY COMMENT '主键',
    invoice_no VARCHAR(64) NOT NULL COMMENT '发票号',
    invoice_code VARCHAR(64) NOT NULL COMMENT '发票代码',
    invoice_type TINYINT NOT NULL COMMENT '发票类型（10 销项专票 20 销项普票 30 进项专票 40 进项普票）',
    buyer_name VARCHAR(128) COMMENT '购方名称',
    buyer_tax_no VARCHAR(64) COMMENT '购方税号',
    seller_name VARCHAR(128) COMMENT '销方名称',
    seller_tax_no VARCHAR(64) COMMENT '销方税号',
    invoice_date DATE COMMENT '开票日期',
    amount_without_tax DECIMAL(20,4) DEFAULT 0 COMMENT '不含税金额',
    tax_amount DECIMAL(20,4) DEFAULT 0 COMMENT '税额',
    amount_with_tax DECIMAL(20,4) DEFAULT 0 COMMENT '价税合计',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已开具 30 已作废 40 已红冲）',
    source_order_type VARCHAR(64) COMMENT '来源单据类型',
    source_order_id BIGINT COMMENT '来源单据 ID',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 发票主表';

-- ----------------------------
-- 3. 发票明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_tax_invoice_line (
    id BIGINT PRIMARY KEY COMMENT '主键',
    invoice_id BIGINT NOT NULL COMMENT '发票 ID',
    line_no INT NOT NULL COMMENT '行号',
    product_name VARCHAR(128) NOT NULL COMMENT '商品名称',
    specification VARCHAR(255) COMMENT '规格型号',
    unit VARCHAR(32) COMMENT '单位',
    quantity DECIMAL(20,4) DEFAULT 0 COMMENT '数量',
    unit_price DECIMAL(20,4) DEFAULT 0 COMMENT '单价（不含税）',
    amount_without_tax DECIMAL(20,4) DEFAULT 0 COMMENT '不含税金额',
    tax_rate DECIMAL(10,6) DEFAULT 0 COMMENT '税率（小数）',
    tax_amount DECIMAL(20,4) DEFAULT 0 COMMENT '税额',
    amount_with_tax DECIMAL(20,4) DEFAULT 0 COMMENT '价税合计',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 发票明细表';

-- ============================================================
-- 索引
-- ============================================================
CREATE UNIQUE INDEX uk_invoice_no_code ON erp_tax_invoice(invoice_no, invoice_code);
CREATE INDEX idx_buyer_tax_no ON erp_tax_invoice(buyer_tax_no);
CREATE INDEX idx_seller_tax_no ON erp_tax_invoice(seller_tax_no);
CREATE INDEX idx_invoice_date ON erp_tax_invoice(invoice_date);
CREATE INDEX idx_invoice_id ON erp_tax_invoice_line(invoice_id);
CREATE INDEX idx_tax_rate_code ON erp_tax_rate(code);

-- ============================================================
-- 字典数据（仅给出 SQL 占位，具体字典管理通过系统字典管理界面维护）
-- 字典类型：erp_tax_rate_type / erp_invoice_type / erp_invoice_status
-- ============================================================
-- erp_tax_rate_type: 10 增值税 / 20 消费税 / 30 附加税
-- erp_invoice_type: 10 销项专票 / 20 销项普票 / 30 进项专票 / 40 进项普票
-- erp_invoice_status: 10 草稿 / 20 已开具 / 30 已作废 / 40 已红冲

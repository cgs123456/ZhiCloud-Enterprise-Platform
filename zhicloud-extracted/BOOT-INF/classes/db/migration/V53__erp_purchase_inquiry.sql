-- ============================================================
-- V53: ERP 采购询比价模块（SCM 询比价补齐）
--
-- 新增 6 张表：
--   erp_purchase_inquiry        询价单主表
--   erp_purchase_inquiry_item   询价单明细
--   erp_purchase_quote          报价单主表
--   erp_purchase_quote_item     报价单明细
--   erp_purchase_compare        比价单主表
--   erp_purchase_compare_line   比价单明细行
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. 询价单主表
CREATE TABLE IF NOT EXISTS erp_purchase_inquiry (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '询价单号',
    inquiry_name VARCHAR(200) NOT NULL COMMENT '询价主题',
    supplier_ids VARCHAR(500) NOT NULL COMMENT '供应商编号列表，逗号分隔',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 / 20 已发布 / 30 已比价 / 40 已关闭 / 50 已转采购订单）',
    total_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '合计金额',
    expected_delivery_date DATE COMMENT '期望交货日期',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_status (status),
    KEY idx_expected_delivery_date (expected_delivery_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购询价单';

-- 2. 询价单明细表
CREATE TABLE IF NOT EXISTS erp_purchase_inquiry_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    inquiry_id BIGINT NOT NULL COMMENT '询价单编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称（冗余）',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '数量',
    unit VARCHAR(50) COMMENT '单位',
    unit_price DECIMAL(20,4) COMMENT '期望价',
    delivery_date DATE COMMENT '期望交货日期',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_inquiry_id (inquiry_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购询价单明细';

-- 3. 报价单主表
CREATE TABLE IF NOT EXISTS erp_purchase_quote (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '报价单号',
    inquiry_id BIGINT NOT NULL COMMENT '询价单编号',
    supplier_id BIGINT NOT NULL COMMENT '供应商编号',
    quote_date DATETIME COMMENT '报价时间',
    total_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '合计金额',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 / 20 已报价 / 30 已采纳 / 40 已拒绝）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_inquiry_id (inquiry_id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购报价单';

-- 4. 报价单明细表
CREATE TABLE IF NOT EXISTS erp_purchase_quote_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    quote_id BIGINT NOT NULL COMMENT '报价单编号',
    inquiry_item_id BIGINT NOT NULL COMMENT '询价单明细编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '数量',
    unit_price DECIMAL(20,4) COMMENT '报价单价',
    amount DECIMAL(20,4) COMMENT '报价金额',
    delivery_date DATE COMMENT '报价交货日期',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_quote_id (quote_id),
    KEY idx_inquiry_item_id (inquiry_item_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购报价单明细';

-- 5. 比价单主表
CREATE TABLE IF NOT EXISTS erp_purchase_compare (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '比价单号',
    inquiry_id BIGINT NOT NULL COMMENT '询价单编号',
    recommend_supplier_id BIGINT COMMENT '推荐供应商编号',
    recommend_reason VARCHAR(500) COMMENT '推荐理由',
    total_quote_count INT COMMENT '报价总数',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 / 20 已完成）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    UNIQUE KEY uk_inquiry_id (inquiry_id, deleted),
    KEY idx_recommend_supplier_id (recommend_supplier_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购比价单';

-- 6. 比价单明细行表
CREATE TABLE IF NOT EXISTS erp_purchase_compare_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    compare_id BIGINT NOT NULL COMMENT '比价单编号',
    inquiry_item_id BIGINT NOT NULL COMMENT '询价单明细编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    supplier_id BIGINT NOT NULL COMMENT '供应商编号',
    quote_item_id BIGINT COMMENT '报价单明细编号',
    unit_price DECIMAL(20,4) COMMENT '报价单价',
    amount DECIMAL(20,4) COMMENT '报价金额',
    delivery_date DATE COMMENT '报价交货日期',
    is_recommended TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_compare_id (compare_id),
    KEY idx_inquiry_item_id (inquiry_item_id),
    KEY idx_supplier_id (supplier_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购比价单明细行';

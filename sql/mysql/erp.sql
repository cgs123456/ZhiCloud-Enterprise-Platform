-- ======================== ERP 企业资源计划建表脚本 ========================
-- 作者：zhicloud
-- 说明：覆盖产品主数据/库存/采购/销售/财务总账等 ERP 核心业务表

-- ============================================================
-- 1. 基础主数据（7 张）
-- ============================================================

-- ----------------------------
-- 产品分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_product_category (
    id BIGINT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类 ID',
    code VARCHAR(64) NOT NULL COMMENT '分类编码',
    name VARCHAR(128) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 产品分类表';

-- ----------------------------
-- 产品单位表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_product_unit (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(64) NOT NULL COMMENT '单位名称',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 产品单位表';

-- ----------------------------
-- 产品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_product (
    id BIGINT PRIMARY KEY COMMENT '主键',
    category_id BIGINT NOT NULL COMMENT '分类 ID',
    unit_id BIGINT NOT NULL COMMENT '单位 ID',
    code VARCHAR(64) NOT NULL COMMENT '产品编码',
    name VARCHAR(128) NOT NULL COMMENT '产品名称',
    barcode VARCHAR(64) COMMENT '条码',
    spec VARCHAR(255) COMMENT '规格',
    sale_price DECIMAL(20,4) COMMENT '销售单价',
    purchase_price DECIMAL(20,4) COMMENT '采购单价',
    cost_price DECIMAL(20,4) COMMENT '成本单价',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 产品表';

-- ----------------------------
-- 供应商表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_supplier (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '供应商编码',
    name VARCHAR(128) NOT NULL COMMENT '供应商名称',
    contact VARCHAR(64) COMMENT '联系人',
    mobile VARCHAR(32) COMMENT '手机号',
    phone VARCHAR(32) COMMENT '电话',
    email VARCHAR(128) COMMENT '邮箱',
    address VARCHAR(255) COMMENT '地址',
    bank_name VARCHAR(128) COMMENT '开户银行',
    bank_account VARCHAR(64) COMMENT '银行账号',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 供应商表';

-- ----------------------------
-- 客户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_customer (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '客户编码',
    name VARCHAR(128) NOT NULL COMMENT '客户名称',
    contact VARCHAR(64) COMMENT '联系人',
    mobile VARCHAR(32) COMMENT '手机号',
    phone VARCHAR(32) COMMENT '电话',
    email VARCHAR(128) COMMENT '邮箱',
    address VARCHAR(255) COMMENT '地址',
    bank_name VARCHAR(128) COMMENT '开户银行',
    bank_account VARCHAR(64) COMMENT '银行账号',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 客户表';

-- ----------------------------
-- 仓库表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_warehouse (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    name VARCHAR(128) NOT NULL COMMENT '仓库名称',
    address VARCHAR(255) COMMENT '仓库地址',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 仓库表';

-- ----------------------------
-- 账户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_account (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '账户编码',
    name VARCHAR(128) NOT NULL COMMENT '账户名称',
    type TINYINT NOT NULL COMMENT '账户类型（10 现金 20 银行 30 支付宝 40 微信）',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    default_flag BIT(1) DEFAULT 0 COMMENT '是否默认',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 账户表';

-- ============================================================
-- 2. 库存核心（2 张）
-- ============================================================

-- ----------------------------
-- 库存余额表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock (
    id BIGINT PRIMARY KEY COMMENT '主键',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    count DECIMAL(20,4) DEFAULT 0 COMMENT '库存数量',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 库存余额表';

-- ----------------------------
-- 库存流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_record (
    id BIGINT PRIMARY KEY COMMENT '主键',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    biz_type TINYINT NOT NULL COMMENT '业务类型（10 其它入库 20 其它出库 30 调拨入 40 调拨出 50 盘盈 60 盘亏 70 采购入库 80 销售出库）',
    bill_id BIGINT COMMENT '业务单 ID',
    bill_item_id BIGINT COMMENT '业务单明细 ID',
    count DECIMAL(20,4) DEFAULT 0 COMMENT '变动数量',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '变动后库存',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 库存流水表';

-- ============================================================
-- 3. 库存单据（8 张，主表+明细成对）
-- ============================================================

-- ----------------------------
-- 其它入库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_in (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '入库单号',
    supplier_id BIGINT COMMENT '供应商 ID',
    in_time DATETIME COMMENT '入库时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已入库）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 其它入库单表';

-- ----------------------------
-- 其它入库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_in_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '入库单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    count DECIMAL(20,4) DEFAULT 0 COMMENT '入库数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 其它入库单明细表';

-- ----------------------------
-- 其它出库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_out (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '出库单号',
    customer_id BIGINT COMMENT '客户 ID',
    out_time DATETIME COMMENT '出库时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已出库）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 其它出库单表';

-- ----------------------------
-- 其它出库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_out_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '出库单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    count DECIMAL(20,4) DEFAULT 0 COMMENT '出库数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 其它出库单明细表';

-- ----------------------------
-- 调拨单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_move (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '调拨单号',
    move_time DATETIME COMMENT '调拨时间',
    from_warehouse_id BIGINT NOT NULL COMMENT '调出仓库 ID',
    to_warehouse_id BIGINT NOT NULL COMMENT '调入仓库 ID',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已调拨）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 调拨单表';

-- ----------------------------
-- 调拨单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_move_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '调拨单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    count DECIMAL(20,4) DEFAULT 0 COMMENT '调拨数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 调拨单明细表';

-- ----------------------------
-- 盘点单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_check (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '盘点单号',
    check_time DATETIME COMMENT '盘点时间',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已盘点）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 盘点单表';

-- ----------------------------
-- 盘点单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_check_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '盘点单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    stock_count DECIMAL(20,4) DEFAULT 0 COMMENT '账面数量',
    actual_count DECIMAL(20,4) DEFAULT 0 COMMENT '实际数量',
    diff_count DECIMAL(20,4) DEFAULT 0 COMMENT '差异数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 盘点单明细表';

-- ============================================================
-- 4. 采购单据（6 张）
-- ============================================================

-- ----------------------------
-- 采购订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '采购订单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商 ID',
    in_time DATETIME COMMENT '预计入库时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已审批 30 已入库 40 已关闭）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 采购订单表';

-- ----------------------------
-- 采购订单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_order_items (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '采购订单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    purchase_count DECIMAL(20,4) DEFAULT 0 COMMENT '采购数量',
    purchase_price DECIMAL(20,4) DEFAULT 0 COMMENT '采购单价',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 采购订单明细表';

-- ----------------------------
-- 采购入库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_in (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '采购入库单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商 ID',
    in_time DATETIME COMMENT '入库时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已入库）',
    payment_status TINYINT DEFAULT 10 COMMENT '付款状态（10 未付款 20 部分付款 30 已付款）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 采购入库单表';

-- ----------------------------
-- 采购入库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_in_items (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '采购入库单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    purchase_count DECIMAL(20,4) DEFAULT 0 COMMENT '采购数量',
    purchase_price DECIMAL(20,4) DEFAULT 0 COMMENT '采购单价',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 采购入库单明细表';

-- ----------------------------
-- 采购退货单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_return (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '采购退货单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商 ID',
    in_time DATETIME COMMENT '退货时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已退货）',
    refund_status TINYINT DEFAULT 10 COMMENT '退款状态（10 未退款 20 部分退款 30 已退款）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 采购退货单表';

-- ----------------------------
-- 采购退货单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_purchase_return_items (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '采购退货单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    purchase_count DECIMAL(20,4) DEFAULT 0 COMMENT '退货数量',
    purchase_price DECIMAL(20,4) DEFAULT 0 COMMENT '退货单价',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 采购退货单明细表';

-- ============================================================
-- 5. 销售单据（6 张）
-- ============================================================

-- ----------------------------
-- 销售订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_sale_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '销售订单号',
    customer_id BIGINT NOT NULL COMMENT '客户 ID',
    out_time DATETIME COMMENT '预计出库时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已审批 30 已出库 40 已关闭）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 销售订单表';

-- ----------------------------
-- 销售订单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_sale_order_items (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '销售订单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    sale_count DECIMAL(20,4) DEFAULT 0 COMMENT '销售数量',
    sale_price DECIMAL(20,4) DEFAULT 0 COMMENT '销售单价',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 销售订单明细表';

-- ----------------------------
-- 销售出库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_sale_out (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '销售出库单号',
    customer_id BIGINT NOT NULL COMMENT '客户 ID',
    out_time DATETIME COMMENT '出库时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已出库）',
    receipt_status TINYINT DEFAULT 10 COMMENT '收款状态（10 未收款 20 部分收款 30 已收款）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 销售出库单表';

-- ----------------------------
-- 销售出库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_sale_out_items (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '销售出库单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    sale_count DECIMAL(20,4) DEFAULT 0 COMMENT '销售数量',
    sale_price DECIMAL(20,4) DEFAULT 0 COMMENT '销售单价',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 销售出库单明细表';

-- ----------------------------
-- 销售退货单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_sale_return (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '销售退货单号',
    customer_id BIGINT NOT NULL COMMENT '客户 ID',
    out_time DATETIME COMMENT '退货时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已退货）',
    refund_status TINYINT DEFAULT 10 COMMENT '退款状态（10 未退款 20 部分退款 30 已退款）',
    total_count DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 销售退货单表';

-- ----------------------------
-- 销售退货单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_sale_return_items (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '销售退货单 ID',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    sale_count DECIMAL(20,4) DEFAULT 0 COMMENT '退货数量',
    sale_price DECIMAL(20,4) DEFAULT 0 COMMENT '退货单价',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总价',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 销售退货单明细表';

-- ============================================================
-- 6. 财务/总账（9 张）
-- ============================================================

-- ----------------------------
-- 付款单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_finance_payment (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '付款单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商 ID',
    finance_time DATETIME COMMENT '付款时间',
    account_id BIGINT NOT NULL COMMENT '账户 ID',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已付款）',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '应付金额',
    payment_price DECIMAL(20,4) DEFAULT 0 COMMENT '实付金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 付款单表';

-- ----------------------------
-- 付款单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_finance_payment_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '付款单 ID',
    bill_id BIGINT NOT NULL COMMENT '业务单 ID',
    bill_type TINYINT NOT NULL COMMENT '业务单类型（10 采购入库 20 采购退货）',
    payment_price DECIMAL(20,4) DEFAULT 0 COMMENT '付款金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 付款单明细表';

-- ----------------------------
-- 收款单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_finance_receipt (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '收款单号',
    customer_id BIGINT NOT NULL COMMENT '客户 ID',
    finance_time DATETIME COMMENT '收款时间',
    account_id BIGINT NOT NULL COMMENT '账户 ID',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已收款）',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '应收金额',
    payment_price DECIMAL(20,4) DEFAULT 0 COMMENT '实收金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 收款单表';

-- ----------------------------
-- 收款单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_finance_receipt_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '收款单 ID',
    bill_id BIGINT NOT NULL COMMENT '业务单 ID',
    bill_type TINYINT NOT NULL COMMENT '业务单类型（10 销售出库 20 销售退货）',
    payment_price DECIMAL(20,4) DEFAULT 0 COMMENT '收款金额',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 收款单明细表';

-- ----------------------------
-- 会计期间表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_period (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '期间编码',
    name VARCHAR(128) NOT NULL COMMENT '期间名称',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 未开启 20 已开启 30 已关闭）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 会计期间表';

-- ----------------------------
-- 期间关闭记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_period_close (
    id BIGINT PRIMARY KEY COMMENT '主键',
    period_id BIGINT NOT NULL COMMENT '会计期间 ID',
    close_time DATETIME COMMENT '关闭时间',
    close_user VARCHAR(64) COMMENT '关闭人',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 期间关闭记录表';

-- ----------------------------
-- 总账科目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_gl_account (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '科目编码',
    name VARCHAR(128) NOT NULL COMMENT '科目名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父科目 ID',
    level INT DEFAULT 1 COMMENT '科目层级',
    balance_direction TINYINT NOT NULL COMMENT '余额方向（10 借 20 贷）',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 总账科目表';

-- ----------------------------
-- 凭证表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_gl_voucher (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '凭证号',
    period_id BIGINT NOT NULL COMMENT '会计期间 ID',
    voucher_time DATETIME COMMENT '凭证日期',
    summary VARCHAR(500) COMMENT '摘要',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已审核 30 已过账）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 凭证表';

-- ----------------------------
-- 凭证分录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_gl_voucher_entry (
    id BIGINT PRIMARY KEY COMMENT '主键',
    master_id BIGINT NOT NULL COMMENT '凭证 ID',
    account_id BIGINT NOT NULL COMMENT '科目 ID',
    debit_amount DECIMAL(20,4) DEFAULT 0 COMMENT '借方金额',
    credit_amount DECIMAL(20,4) DEFAULT 0 COMMENT '贷方金额',
    summary VARCHAR(500) COMMENT '摘要',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 凭证分录表';

-- ============================================================
-- 9. 库存批次/序列号管理（2 张）
-- ============================================================

-- ----------------------------
-- 库存批次表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_batch (
    id BIGINT PRIMARY KEY COMMENT '主键',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',
    quantity DECIMAL(20,4) DEFAULT 0 COMMENT '批次数量',
    status TINYINT DEFAULT 10 COMMENT '批次状态（10 可用 20 冻结 30 过期）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 库存批次表';

-- ----------------------------
-- 库存序列号表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_stock_serial (
    id BIGINT PRIMARY KEY COMMENT '主键',
    serial_no VARCHAR(64) NOT NULL COMMENT '序列号',
    product_id BIGINT NOT NULL COMMENT '产品 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    batch_id BIGINT COMMENT '批次 ID',
    status TINYINT DEFAULT 10 COMMENT '序列号状态（10 在库 20 出库）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 库存序列号表';

-- ============================================================
-- 10. 多币种管理（2 张）
-- ============================================================

-- ----------------------------
-- 币种表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_currency (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(16) NOT NULL COMMENT '币种编码（如 CNY/USD/EUR）',
    name VARCHAR(64) NOT NULL COMMENT '币种名称',
    symbol VARCHAR(16) COMMENT '币种符号（如 ¥/$/€）',
    is_base BIT(1) DEFAULT 0 COMMENT '是否本位币',
    enabled TINYINT DEFAULT 0 COMMENT '是否启用（0 启用 1 禁用）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 币种表';

-- ----------------------------
-- 汇率表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_exchange_rate (
    id BIGINT PRIMARY KEY COMMENT '主键',
    from_currency_id BIGINT NOT NULL COMMENT '源币种 ID',
    to_currency_id BIGINT NOT NULL COMMENT '目标币种 ID',
    rate DECIMAL(20,8) NOT NULL COMMENT '汇率',
    effective_date DATE NOT NULL COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 汇率表';

-- ============================================================
-- 11. 管理会计 CO（4 张：成本中心 / 利润中心 / 成本分摊 / 获利分析）
-- ============================================================

-- ----------------------------
-- 成本中心表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_cost_center (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '成本中心编码',
    name VARCHAR(128) NOT NULL COMMENT '成本中心名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父级成本中心 ID（顶级为 0）',
    manager_id BIGINT COMMENT '负责人 ID',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 成本中心表';

-- ----------------------------
-- 利润中心表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_profit_center (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '利润中心编码',
    name VARCHAR(128) NOT NULL COMMENT '利润中心名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父级利润中心 ID（顶级为 0）',
    manager_id BIGINT COMMENT '负责人 ID',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 利润中心表';

-- ----------------------------
-- 成本分摊表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_cost_allocation (
    id BIGINT PRIMARY KEY COMMENT '主键',
    cost_center_id BIGINT NOT NULL COMMENT '源成本中心 ID',
    allocation_type TINYINT NOT NULL COMMENT '分摊类型（10 手工分摊 20 规则分摊）',
    amount DECIMAL(20,4) NOT NULL COMMENT '分摊金额',
    allocation_date DATE NOT NULL COMMENT '分摊日期',
    target_cost_center_id BIGINT NOT NULL COMMENT '目标成本中心 ID',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 成本分摊表';

-- ----------------------------
-- 获利能力分析表
-- ----------------------------
CREATE TABLE IF NOT EXISTS erp_profitability_analysis (
    id BIGINT PRIMARY KEY COMMENT '主键',
    profit_center_id BIGINT NOT NULL COMMENT '利润中心 ID',
    period_id BIGINT NOT NULL COMMENT '会计期间 ID',
    revenue DECIMAL(20,4) DEFAULT 0 COMMENT '收入',
    cost DECIMAL(20,4) DEFAULT 0 COMMENT '成本',
    profit DECIMAL(20,4) DEFAULT 0 COMMENT '利润',
    profit_margin DECIMAL(20,4) DEFAULT 0 COMMENT '利润率',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='ERP 获利能力分析表';

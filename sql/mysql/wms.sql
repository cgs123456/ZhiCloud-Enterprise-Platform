-- ======================== 仓储管理系统（WMS）建表脚本 ========================
-- 作者：yudao
-- 说明：覆盖仓库/往来企业/商品主数据/库存/入库/出库/移库/盘库等 WMS 核心业务表
-- 注意：波次单相关表（wms_wave_order / wms_wave_order_detail）已在 wms_wave.sql 中维护，
--      本脚本末尾附同名定义以保持完整性，与 wms_wave.sql 保持一致。
-- 规范：InnoDB / utf8mb4 / utf8mb4_unicode_ci；主键 BIGINT（应用层雪花ID）；
--      统一含 tenant_id/creator/create_time/updater/update_time/deleted 审计字段。

-- ============================================================
-- 1. 基础主数据（5 张）
-- ============================================================

-- ----------------------------
-- 仓库表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_warehouse (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '仓库编码',
    name VARCHAR(128) NOT NULL COMMENT '仓库名称',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_code (code),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 仓库表';

-- ----------------------------
-- 往来企业表（供应商/客户/承运商等）
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_merchant (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '往来企业编码',
    name VARCHAR(128) NOT NULL COMMENT '往来企业名称',
    type TINYINT NOT NULL COMMENT '往来企业类型（1 供应商 2 客户 3 承运商）',
    level VARCHAR(20) COMMENT '级别',
    bank_name VARCHAR(128) COMMENT '开户银行',
    bank_account VARCHAR(64) COMMENT '银行账号',
    address VARCHAR(255) COMMENT '地址',
    mobile VARCHAR(32) COMMENT '手机号',
    telephone VARCHAR(32) COMMENT '座机号',
    contact VARCHAR(64) COMMENT '联系人',
    email VARCHAR(128) COMMENT '邮箱',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_code (code),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 往来企业表';

-- ----------------------------
-- 商品品牌表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_item_brand (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '品牌编码',
    name VARCHAR(64) NOT NULL COMMENT '品牌名称',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_code (code),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 商品品牌表';

-- ----------------------------
-- 商品分类表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_item_category (
    id BIGINT PRIMARY KEY COMMENT '主键',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类 ID',
    code VARCHAR(64) NOT NULL COMMENT '分类编码',
    name VARCHAR(64) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_code (code),
    KEY idx_parent_id (parent_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 商品分类表';

-- ----------------------------
-- 商品表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) COMMENT '商品编码',
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    unit VARCHAR(32) COMMENT '单位',
    category_id BIGINT NOT NULL COMMENT '商品分类 ID',
    brand_id BIGINT COMMENT '商品品牌 ID',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_category_id (category_id),
    KEY idx_brand_id (brand_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 商品表';

-- ----------------------------
-- 商品 SKU 表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_item_sku (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(255) NOT NULL COMMENT '规格名称',
    item_id BIGINT NOT NULL COMMENT '商品 ID',
    bar_code VARCHAR(64) COMMENT '条码',
    code VARCHAR(64) COMMENT '规格编码',
    length DECIMAL(16,4) COMMENT '长（cm）',
    width DECIMAL(16,4) COMMENT '宽（cm）',
    height DECIMAL(16,4) COMMENT '高（cm）',
    gross_weight DECIMAL(16,4) COMMENT '毛重（kg）',
    net_weight DECIMAL(16,4) COMMENT '净重（kg）',
    cost_price DECIMAL(20,4) COMMENT '成本价（元）',
    selling_price DECIMAL(20,4) COMMENT '销售价（元）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_item_id (item_id),
    KEY idx_bar_code (bar_code),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 商品 SKU 表';

-- ============================================================
-- 2. 库存核心（2 张）
-- ============================================================

-- ----------------------------
-- 库存余额表
-- 说明：available_quantity / locked_quantity / frozen_quantity 为新增字段，
--       用于支持波次拣货预占与质检冻结场景。
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_inventory (
    id BIGINT PRIMARY KEY COMMENT '主键',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '库存数量（物理库存总量）',
    available_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '可用数量（= quantity - locked_quantity - frozen_quantity）',
    locked_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '锁定数量（已被订单/波次预占但未出库）',
    frozen_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '冻结数量（因质检、盘点等冻结）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_sku_warehouse (sku_id, warehouse_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 库存余额表';

-- ----------------------------
-- 库存流水表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_inventory_history (
    id BIGINT PRIMARY KEY COMMENT '主键',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU ID',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '库存变化数量',
    before_quantity DECIMAL(20,4) COMMENT '变化前库存数量',
    after_quantity DECIMAL(20,4) COMMENT '变化后库存数量',
    price DECIMAL(20,4) COMMENT '单价',
    total_price DECIMAL(20,4) COMMENT '库存变化金额',
    remark VARCHAR(500) COMMENT '备注',
    order_id BIGINT COMMENT '来源单据 ID',
    order_no VARCHAR(64) COMMENT '来源单据号',
    order_type TINYINT COMMENT '单据类型（10 入库 20 出库 30 移库 40 盘库 50 波次）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_warehouse_sku (warehouse_id, sku_id),
    KEY idx_order (order_id, order_type),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 库存流水表';

-- ============================================================
-- 3. 库存单据（8 张，主表+明细成对）
-- ============================================================

-- ----------------------------
-- 入库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_receipt_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '入库单号',
    type TINYINT NOT NULL COMMENT '入库类型（1 采购入库 2 退货入库 3 调拨入库 4 其他入库）',
    order_time DATETIME NOT NULL COMMENT '单据日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 草稿 1 待审核 2 已审核 3 已完成 4 已作废）',
    biz_order_no VARCHAR(64) COMMENT '业务订单号',
    merchant_id BIGINT COMMENT '供应商 ID',
    remark VARCHAR(500) COMMENT '备注',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    total_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '总数量',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_no (no),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 入库单表';

-- ----------------------------
-- 入库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_receipt_order_detail (
    id BIGINT PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '入库单 ID',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '入库数量',
    price DECIMAL(20,4) COMMENT '单价',
    total_price DECIMAL(20,4) COMMENT '行金额（数量 * 单价）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_order_id (order_id),
    KEY idx_sku_id (sku_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 入库单明细表';

-- ----------------------------
-- 出库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_shipment_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '出库单号',
    type TINYINT NOT NULL COMMENT '出库类型（1 销售出库 2 退货出库 3 调拨出库 4 其他出库）',
    order_time DATETIME NOT NULL COMMENT '单据日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 草稿 1 待审核 2 已审核 3 已完成 4 已作废）',
    biz_order_no VARCHAR(64) COMMENT '业务订单号',
    merchant_id BIGINT COMMENT '客户 ID',
    remark VARCHAR(500) COMMENT '备注',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    total_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '总数量',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_no (no),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_merchant_id (merchant_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 出库单表';

-- ----------------------------
-- 出库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_shipment_order_detail (
    id BIGINT PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '出库单 ID',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '出库数量',
    price DECIMAL(20,4) COMMENT '单价',
    total_price DECIMAL(20,4) COMMENT '行金额（数量 * 单价）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_order_id (order_id),
    KEY idx_sku_id (sku_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 出库单明细表';

-- ----------------------------
-- 移库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_movement_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '移库单号',
    order_time DATETIME NOT NULL COMMENT '单据日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 草稿 1 待审核 2 已审核 3 已完成 4 已作废）',
    remark VARCHAR(500) COMMENT '备注',
    source_warehouse_id BIGINT NOT NULL COMMENT '来源仓库 ID',
    target_warehouse_id BIGINT NOT NULL COMMENT '目标仓库 ID',
    total_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '总数量',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_no (no),
    KEY idx_source_warehouse_id (source_warehouse_id),
    KEY idx_target_warehouse_id (target_warehouse_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 移库单表';

-- ----------------------------
-- 移库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_movement_order_detail (
    id BIGINT PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '移库单 ID',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU ID',
    source_warehouse_id BIGINT NOT NULL COMMENT '来源仓库 ID',
    target_warehouse_id BIGINT NOT NULL COMMENT '目标仓库 ID',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '移库数量',
    price DECIMAL(20,4) COMMENT '单价',
    total_price DECIMAL(20,4) COMMENT '行金额（数量 * 单价）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_order_id (order_id),
    KEY idx_sku_id (sku_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 移库单明细表';

-- ----------------------------
-- 盘库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_check_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '盘库单号',
    order_time DATETIME NOT NULL COMMENT '单据日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 草稿 1 待审核 2 已审核 3 已完成 4 已作废）',
    remark VARCHAR(500) COMMENT '备注',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    total_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '盈亏数量（实盘 - 账面）',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总金额（账面数量 * 单价）',
    actual_price DECIMAL(20,4) DEFAULT 0 COMMENT '实际金额（实盘数量 * 单价）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_no (no),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 盘库单表';

-- ----------------------------
-- 盘库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_check_order_detail (
    id BIGINT PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '盘库单 ID',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    inventory_id BIGINT COMMENT '库存 ID',
    receipt_time DATETIME COMMENT '入库时间',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '账面数量',
    check_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '实盘数量',
    price DECIMAL(20,4) COMMENT '单价',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_order_id (order_id),
    KEY idx_sku_id (sku_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 盘库单明细表';

-- ============================================================
-- 4. 波次单据（2 张）
-- ============================================================
-- 以下两张表与 wms_wave.sql 完全一致，此处重复定义以保持 wms.sql 完整性。
-- 部署时按需选择其中一处执行即可，避免重复建表。

-- ----------------------------
-- 波次单主表（与 wms_wave.sql 一致）
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_wave_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    no VARCHAR(64) NOT NULL COMMENT '波次单号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    strategy TINYINT NOT NULL COMMENT '波次策略（1 按仓库合并 2 按客户合并 3 按商品合并 4 按承运商合并）',
    order_time DATETIME NOT NULL COMMENT '单据日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 草稿 4 已完成 5 已作废）',
    picker VARCHAR(64) COMMENT '拣货员',
    remark VARCHAR(500) COMMENT '备注',
    shipment_count INT DEFAULT 0 COMMENT '出库单数',
    sku_count INT DEFAULT 0 COMMENT 'SKU 数',
    total_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_price DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_no (no)
) COMMENT='WMS 波次单主表';

-- ----------------------------
-- 波次单明细表（与 wms_wave.sql 一致）
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_wave_order_detail (
    id BIGINT PRIMARY KEY COMMENT '主键',
    wave_order_id BIGINT NOT NULL COMMENT '波次单 ID',
    shipment_order_id BIGINT NOT NULL COMMENT '出库单 ID',
    sku_id BIGINT NOT NULL COMMENT '商品规格 ID',
    pick_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '拣货数量',
    picked_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '已拣数量',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_wave_order_id (wave_order_id),
    KEY idx_shipment_order_id (shipment_order_id)
) COMMENT='WMS 波次单明细表';

-- ============================================================
-- 5. 库存批次（1 张，批次/效期管理）
-- ============================================================

-- ----------------------------
-- 库存批次表
-- 说明：记录同一库存行下不同批次/效期的库存明细，用于批次追溯与 FIFO/FEFO 出库策略。
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_inventory_batch (
    id BIGINT PRIMARY KEY COMMENT '主键',
    inventory_id BIGINT NOT NULL COMMENT '库存 ID',
    batch_no VARCHAR(64) NOT NULL COMMENT '批次号',
    production_date DATE COMMENT '生产日期',
    expiry_date DATE COMMENT '过期日期',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '批次数量',
    locked_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '锁定数量（已被订单/波次预占但未出库）',
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '批次状态（AVAILABLE 可用 / FROZEN 冻结 / EXPIRED 已过期）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_inventory_batch (inventory_id, batch_no),
    KEY idx_inventory_id (inventory_id),
    KEY idx_batch_no (batch_no),
    KEY idx_expiry_date (expiry_date),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 库存批次表';

-- ----------------------------
-- 商品表补充 ABC 分类字段
-- ----------------------------
ALTER TABLE wms_item ADD COLUMN IF NOT EXISTS abc_classification VARCHAR(10) COMMENT 'ABC 分类（A 高频 / B 中频 / C 低频）' AFTER remark;

-- ============================================================
-- V56: WMS 3PL 补齐（越库作业 + 3PL 计费）
--
-- 修改 1 张表：
--   wms_inventory  增加 owner_id 字段（货主ID）+ 联合索引 idx_warehouse_owner_sku
--
-- 新增 6 张表：
--   wms_cross_dock_order         越库单（收货直发，跳过上架）
--   wms_cross_dock_order_detail  越库单明细
--   wms_billing_contract         3PL 计费合同
--   wms_billing_contract_item    计费合同条款
--   wms_billing_bill             3PL 计费账单
--   wms_billing_bill_line        计费账单明细
--
-- 兼容性：新增字段允许 NULL；新增表均使用 CREATE TABLE IF NOT EXISTS
-- 幂等性：使用 IF NOT EXISTS
-- ============================================================

-- ----------------------------
-- 1. 为 wms_inventory 增加货主字段
-- ----------------------------
ALTER TABLE wms_inventory
    ADD COLUMN IF NOT EXISTS owner_id BIGINT DEFAULT NULL COMMENT '货主ID（3PL 场景区分不同货主库存归属）' AFTER warehouse_id;

ALTER TABLE wms_inventory
    ADD INDEX IF NOT EXISTS idx_warehouse_owner_sku (warehouse_id, owner_id, sku_id);

-- ----------------------------
-- 2. 越库单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_cross_dock_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(64) NOT NULL COMMENT '越库单号',
    source_supplier_id BIGINT DEFAULT NULL COMMENT '源头供应商编号',
    target_customer_id BIGINT DEFAULT NULL COMMENT '目标客户编号',
    receipt_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联入库单号',
    shipment_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联出库单号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '越库状态（10待收货/20已收货/30已分配/40已完成/50已取消）',
    total_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_no (no, tenant_id),
    KEY idx_source_supplier (source_supplier_id),
    KEY idx_target_customer (target_customer_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 越库单';

-- ----------------------------
-- 3. 越库单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_cross_dock_order_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    order_id BIGINT NOT NULL COMMENT '越库单编号',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU 编号',
    product_name VARCHAR(255) DEFAULT NULL COMMENT '商品名称',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '数量',
    unit_price DECIMAL(20,4) DEFAULT NULL COMMENT '单价',
    amount DECIMAL(20,4) DEFAULT NULL COMMENT '行金额',
    receipt_detail_id BIGINT DEFAULT NULL COMMENT '关联入库明细编号',
    shipment_detail_id BIGINT DEFAULT NULL COMMENT '关联出库明细编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_order (order_id),
    KEY idx_sku (sku_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 越库单明细';

-- ----------------------------
-- 4. 3PL 计费合同表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_billing_contract (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    contract_no VARCHAR(64) NOT NULL COMMENT '合同号',
    owner_id BIGINT NOT NULL COMMENT '货主编号',
    contract_name VARCHAR(255) NOT NULL COMMENT '合同名称',
    start_date DATE NOT NULL COMMENT '生效日期',
    end_date DATE NOT NULL COMMENT '失效日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '合同状态（10生效/20失效/30已终止）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_no (contract_no, tenant_id),
    KEY idx_owner (owner_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 3PL 计费合同';

-- ----------------------------
-- 5. 3PL 计费合同条款表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_billing_contract_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    contract_id BIGINT NOT NULL COMMENT '计费合同编号',
    fee_type TINYINT NOT NULL COMMENT '费用类型（10仓储费/20操作费/30装卸费/40越库费/50其他）',
    fee_mode TINYINT NOT NULL COMMENT '计费方式（10按天/20按次/30按件）',
    unit_price DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '单价',
    min_charge DECIMAL(20,4) DEFAULT NULL COMMENT '最低收费',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_contract (contract_id),
    KEY idx_fee_type (fee_type),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 3PL 计费合同条款';

-- ----------------------------
-- 6. 3PL 计费账单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_billing_bill (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    bill_no VARCHAR(64) NOT NULL COMMENT '账单号',
    owner_id BIGINT NOT NULL COMMENT '货主编号',
    billing_period_start DATETIME NOT NULL COMMENT '计费周期开始时间',
    billing_period_end DATETIME NOT NULL COMMENT '计费周期结束时间',
    total_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '总金额',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '账单状态（10草稿/20已确认/30已结算/40已付款）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_no (bill_no, tenant_id),
    KEY idx_owner (owner_id),
    KEY idx_status (status),
    KEY idx_billing_period (billing_period_start, billing_period_end),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 3PL 计费账单';

-- ----------------------------
-- 7. 3PL 计费账单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_billing_bill_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    bill_id BIGINT NOT NULL COMMENT '账单编号',
    contract_item_id BIGINT DEFAULT NULL COMMENT '计费合同条款编号',
    fee_type TINYINT NOT NULL COMMENT '费用类型（10仓储费/20操作费/30装卸费/40越库费/50其他）',
    fee_mode TINYINT NOT NULL COMMENT '计费方式（10按天/20按次/30按件）',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '数量（天数/次数/件数）',
    unit_price DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '单价',
    amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '金额',
    reference_order_no VARCHAR(64) DEFAULT NULL COMMENT '关联单据号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_bill (bill_id),
    KEY idx_contract_item (contract_item_id),
    KEY idx_fee_type (fee_type),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 3PL 计费账单明细';

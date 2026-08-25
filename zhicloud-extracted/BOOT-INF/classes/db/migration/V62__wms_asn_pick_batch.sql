-- ============================================================
-- V62: WMS ASN 到货通知 + 月台管理 + 拣货任务 + 批次效期扩展
--
-- 修改 1 张表：
--   wms_inventory_batch  增加 shelf_life_days（保质期天数）、supplier_batch_no（供应商批次号）
--
-- 新增 4 张表：
--   wms_dock                月台管理（收发货月台，关联 ASN）
--   wms_asn_order           ASN 到货通知单（3PL 标配）
--   wms_asn_order_detail    ASN 到货通知单明细
--   wms_pick_task           拣货任务（拣选策略引擎生成）
--
-- 兼容性：新增字段允许 NULL；新增表均使用 CREATE TABLE IF NOT EXISTS
-- 幂等性：使用 IF NOT EXISTS
-- ============================================================

-- ----------------------------
-- 1. 为 wms_inventory_batch 增加保质期天数、供应商批次号字段
-- ----------------------------
-- 幂等新增列：wms_inventory_batch.shelf_life_days
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_batch' AND COLUMN_NAME = 'shelf_life_days'),
                  'DO 0',
                  'ALTER TABLE `wms_inventory_batch` ADD COLUMN `shelf_life_days` INT DEFAULT NULL COMMENT ''保质期天数（生产日期 + 保质期天数 = 过期日期）'' AFTER expiry_date');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;

-- 幂等新增列：wms_inventory_batch.supplier_batch_no
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_inventory_batch' AND COLUMN_NAME = 'supplier_batch_no'),
                  'DO 0',
                  'ALTER TABLE `wms_inventory_batch` ADD COLUMN `supplier_batch_no` VARCHAR(64) DEFAULT NULL COMMENT ''供应商批次号（供应链批次追溯）'' AFTER shelf_life_days');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;

-- 更新批次状态注释以包含 NEAR_EXPIRY 临期预警状态
ALTER TABLE wms_inventory_batch
    MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '批次状态（AVAILABLE 正常 / NEAR_EXPIRY 临期预警 / EXPIRED 已过期 / FROZEN 已冻结）';

-- ----------------------------
-- 2. 月台管理表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_dock (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    dock_code VARCHAR(64) NOT NULL COMMENT '月台编号',
    dock_name VARCHAR(128) NOT NULL COMMENT '月台名称',
    dock_type TINYINT DEFAULT 10 COMMENT '月台类型（10 收货月台 / 20 发货月台 / 30 越库月台）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 空闲 / 20 占用 / 30 维修中）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dock_code (dock_code, tenant_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 月台管理表';

-- ----------------------------
-- 3. ASN 到货通知单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_asn_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(64) NOT NULL COMMENT 'ASN 编号',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商编号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    dock_id BIGINT DEFAULT NULL COMMENT '月台编号',
    expected_arrival_time DATETIME DEFAULT NULL COMMENT '预计到货时间',
    actual_arrival_time DATETIME DEFAULT NULL COMMENT '实际到货时间',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待到货 / 20 已到货 / 30 已收货 / 40 已上架 / 50 已关闭）',
    total_quantity DECIMAL(20,4) DEFAULT 0 COMMENT '总数量',
    total_amount DECIMAL(20,4) DEFAULT 0 COMMENT '总金额',
    transport_mode TINYINT DEFAULT NULL COMMENT '运输方式（10 卡车 / 20 铁路 / 30 空运 / 40 海运）',
    carrier_name VARCHAR(128) DEFAULT NULL COMMENT '承运商',
    vehicle_no VARCHAR(32) DEFAULT NULL COMMENT '车牌号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_no (no, tenant_id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_dock_id (dock_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS ASN 到货通知单表';

-- ----------------------------
-- 4. ASN 到货通知单明细表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_asn_order_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    asn_order_id BIGINT NOT NULL COMMENT 'ASN 单编号',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU 编号',
    product_name VARCHAR(256) DEFAULT NULL COMMENT '商品名称',
    expected_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '预计数量',
    received_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '已收数量',
    unit VARCHAR(32) DEFAULT NULL COMMENT '单位',
    lot_number VARCHAR(64) DEFAULT NULL COMMENT '批次号',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    expiry_date DATE DEFAULT NULL COMMENT '过期日期',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_asn_order_id (asn_order_id),
    KEY idx_sku_id (sku_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS ASN 到货通知单明细表';

-- ----------------------------
-- 5. 拣货任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_pick_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    shipment_order_id BIGINT DEFAULT NULL COMMENT '出库单编号',
    wave_order_id BIGINT DEFAULT NULL COMMENT '波次单编号',
    sku_id BIGINT NOT NULL COMMENT '商品 SKU 编号',
    product_name VARCHAR(256) DEFAULT NULL COMMENT '商品名称',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '应拣数量',
    picked_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '已拣数量',
    location_id BIGINT DEFAULT NULL COMMENT '库位编号',
    pick_sequence INT DEFAULT NULL COMMENT '拣货顺序',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待拣 / 20 已拣 / 30 已确认）',
    picker_user_id BIGINT DEFAULT NULL COMMENT '拣货员用户编号',
    pick_time DATETIME DEFAULT NULL COMMENT '拣货时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no, tenant_id),
    KEY idx_shipment_order_id (shipment_order_id),
    KEY idx_wave_order_id (wave_order_id),
    KEY idx_sku_id (sku_id),
    KEY idx_picker_user_id (picker_user_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 拣货任务表';

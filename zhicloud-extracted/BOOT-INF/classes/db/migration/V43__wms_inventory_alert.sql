-- ============================================================
-- V43: WMS 库存预警（P0-1）
--
-- 新增 2 张表：
--   wms_safety_stock_config  安全库存配置（仓库 + SKU 维度）
--   wms_inventory_alert      库存预警记录（低库存/高库存/临期/过期/呆滞）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- ----------------------------
-- 安全库存配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_safety_stock_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    product_id BIGINT NOT NULL COMMENT '商品 SKU 编号',
    safety_stock DECIMAL(20,4) NOT NULL COMMENT '安全库存',
    max_stock DECIMAL(20,4) COMMENT '最高库存',
    min_stock DECIMAL(20,4) COMMENT '最低库存',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_product (warehouse_id, product_id, tenant_id),
    KEY idx_warehouse (warehouse_id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 安全库存配置';

-- ----------------------------
-- 库存预警记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_inventory_alert (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    alert_type VARCHAR(32) NOT NULL COMMENT '预警类型（LOW_STOCK/HIGH_STOCK/NEAR_EXPIRY/EXPIRED/DEAD_STOCK）',
    warehouse_id BIGINT COMMENT '仓库编号',
    product_id BIGINT NOT NULL COMMENT '商品 SKU 编号',
    batch_no VARCHAR(64) COMMENT '批次号（保质期预警用）',
    current_quantity DECIMAL(20,4) COMMENT '当前库存',
    threshold_value DECIMAL(20,4) COMMENT '阈值',
    alert_time DATETIME NOT NULL COMMENT '预警时间',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 未处理 1 已确认 2 已处理）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_alert_product (product_id),
    KEY idx_alert_status (status),
    KEY idx_alert_type (alert_type),
    KEY idx_alert_warehouse (warehouse_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 库存预警记录';

-- ============================================================
-- V37: WMS 序列号 SN 管理（P1）
--
-- 新增 1 张表：
--   wms_sn  序列号主数据（全生命周期 + 正反向追溯）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

CREATE TABLE IF NOT EXISTS wms_sn (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    sn VARCHAR(64) NOT NULL COMMENT '序列号',
    product_id BIGINT NOT NULL COMMENT '商品编号（关联 wms_item.id）',
    batch_id BIGINT COMMENT '库存批次编号（关联 wms_inventory_batch.id）',
    inventory_id BIGINT COMMENT '库存编号（关联 wms_inventory.id）',
    status VARCHAR(32) NOT NULL DEFAULT 'GENERATED' COMMENT '状态（GENERATED 已生成 / BOUND 已绑定 / IN_STOCK 在库 / SHIPPED 已出库 / RETURNED 已退货）',
    warehouse_id BIGINT COMMENT '仓库编号',
    zone_id BIGINT COMMENT '库区编号',
    location_id BIGINT COMMENT '库位编号',
    inbound_order_id BIGINT COMMENT '入库单编号（正向追溯）',
    outbound_order_id BIGINT COMMENT '出库单编号（反向追溯）',
    bound_time DATETIME COMMENT '绑定时间',
    shipped_time DATETIME COMMENT '出库时间',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_sn (sn, tenant_id),
    KEY idx_product (product_id),
    KEY idx_batch (batch_id),
    KEY idx_inbound (inbound_order_id),
    KEY idx_outbound (outbound_order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 序列号';
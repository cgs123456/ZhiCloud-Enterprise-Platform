-- ======================== 仓储管理系统（WMS）波次单建表脚本 ========================
-- 作者：zhicloud
-- 说明：波次单是出库单的聚合容器，将相同仓库/相同波次策略的出库单合并为一个波次，
--      用于批量拣货、批量复核、批量出库。

-- ----------------------------
-- 波次单主表
-- ----------------------------
DROP TABLE IF EXISTS wms_wave_order;
CREATE TABLE wms_wave_order (
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
-- 波次单明细表
-- ----------------------------
DROP TABLE IF EXISTS wms_wave_order_detail;
CREATE TABLE wms_wave_order_detail (
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

-- ----------------------------
-- 权限初始化
-- ----------------------------
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
('波次单管理', '', 2, 30, 0, '/wms/wave-order', '', 'wms/waveOrder/index', 0, 'admin', NOW(), 'admin', NOW(), 0, 0),
('波次单查询', 'wms:wave-order:query', 3, 1, 0, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0, 0),
('波次单创建', 'wms:wave-order:create', 3, 2, 0, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0, 0),
('波次单更新', 'wms:wave-order:update', 3, 3, 0, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0, 0),
('波次单删除', 'wms:wave-order:delete', 3, 4, 0, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0, 0);

-- ----------------------------
-- 字典初始化
-- ----------------------------
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('WMS 波次策略', 'wms_wave_strategy', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'WMS 波次策略字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '按仓库合并', '1', 'wms_wave_strategy', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '按客户合并', '2', 'wms_wave_strategy', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '按商品合并', '3', 'wms_wave_strategy', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '按承运商合并', '4', 'wms_wave_strategy', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

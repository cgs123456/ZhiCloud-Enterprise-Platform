-- ======================== 仓储管理系统（WMS）库区/库位建表脚本 ========================
-- 作者：zhicloud
-- 说明：WMS 库位三级建模（仓库 Warehouse → 库区 Zone → 库位 Location）
-- 规范：InnoDB / utf8mb4 / utf8mb4_unicode_ci；主键 BIGINT（应用层雪花ID）；
--      统一含 tenant_id/creator/create_time/updater/update_time/deleted 审计字段。

-- ----------------------------
-- 库区表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_zone (
    id BIGINT PRIMARY KEY COMMENT '主键',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    code VARCHAR(64) NOT NULL COMMENT '库区编码',
    name VARCHAR(128) NOT NULL COMMENT '库区名称',
    type TINYINT NOT NULL DEFAULT 10 COMMENT '库区类型（10 存储区 20 拣货区 30 退货区 40 不合格品区）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 库区表';

-- ----------------------------
-- 库位表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_location (
    id BIGINT PRIMARY KEY COMMENT '主键',
    zone_id BIGINT NOT NULL COMMENT '库区 ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID（冗余，便于查询）',
    code VARCHAR(64) NOT NULL COMMENT '库位编码',
    name VARCHAR(128) NOT NULL COMMENT '库位名称',
    barcode VARCHAR(64) COMMENT '库位条码',
    type TINYINT NOT NULL DEFAULT 10 COMMENT '库位类型（10 储位 20 拣货位 30 收货位 40 发货位）',
    capacity_weight DECIMAL(20,4) COMMENT '承重容量（kg）',
    capacity_volume DECIMAL(20,4) COMMENT '容积容量（m³）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 空闲 20 占用 30 锁定 40 禁用）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_zone_code (zone_id, code),
    KEY idx_zone_id (zone_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_barcode (barcode),
    KEY idx_tenant_id (tenant_id)
) COMMENT='WMS 库位表';

-- ----------------------------
-- 字典初始化
-- ----------------------------

-- 库区类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('WMS 库区类型', 'wms_zone_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'WMS 库区类型字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '存储区', '10', 'wms_zone_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '拣货区', '20', 'wms_zone_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '退货区', '30', 'wms_zone_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '不合格品区', '40', 'wms_zone_type', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 库位类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('WMS 库位类型', 'wms_location_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'WMS 库位类型字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '储位', '10', 'wms_location_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '拣货位', '20', 'wms_location_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '收货位', '30', 'wms_location_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '发货位', '40', 'wms_location_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 库位状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('WMS 库位状态', 'wms_location_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'WMS 库位状态字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '空闲', '10', 'wms_location_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '占用', '20', 'wms_location_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '锁定', '30', 'wms_location_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '禁用', '40', 'wms_location_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

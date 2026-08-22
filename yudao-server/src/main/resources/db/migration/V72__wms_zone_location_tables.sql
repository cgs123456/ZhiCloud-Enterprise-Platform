-- ============================================================
-- V72: WMS 库区/库位表补齐（Stage 3 架构一致性修复）
--
-- 问题：wms_zone / wms_location 两张表仅存在于参考脚本 sql/mysql/wms_zone_location.sql，
--       却从未被任何 Flyway 迁移创建。运行时若 zone/location 相关 Mapper/Service 被调用，
--       将因 "Table 'wms_zone' doesn't exist" 直接 500。
--
-- 修复：补齐两张表 + 对应字典初始化（幂等，全新库与存量库均可安全执行）。
--   1) CREATE TABLE IF NOT EXISTS wms_zone
--   2) CREATE TABLE IF NOT EXISTS wms_location
--   3) 字典类型/数据使用 INSERT IGNORE，避免存量库重复主键冲突
-- ============================================================

-- ----------------------------
-- 1. 库区表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_zone (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    warehouse_id BIGINT NOT NULL COMMENT '仓库 ID',
    code VARCHAR(64) NOT NULL COMMENT '库区编码',
    name VARCHAR(128) NOT NULL COMMENT '库区名称',
    type TINYINT NOT NULL DEFAULT 10 COMMENT '库区类型（10 存储区 20 拣货区 30 退货区 40 不合格品区）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_code (warehouse_id, code),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 库区表';

-- ----------------------------
-- 2. 库位表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_location (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
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
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_zone_code (zone_id, code),
    KEY idx_zone_id (zone_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_barcode (barcode),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 库位表';

-- ----------------------------
-- 3. 字典初始化
--
-- 注意两处易错点：
--   1) system_dict_type / system_dict_data 在芋道中是「全局字典」，
--      表结构里**没有 tenant_id 列**，写入时带上会 ERROR 1054 Unknown column。
--   2) system_dict_type.type 上没有唯一索引（仅 PRIMARY KEY id），
--      因此 INSERT IGNORE 并不会按 type 去重，重复执行会插出脏数据。
--      改用 INSERT ... SELECT ... WHERE NOT EXISTS 实现真正幂等。
-- ----------------------------

-- 库区类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 'WMS 库区类型', 'wms_zone_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'WMS 库区类型字典'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_type t WHERE t.type = 'wms_zone_type');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT * FROM (
    SELECT 1 AS sort, '存储区' AS label, '10' AS value, 'wms_zone_type' AS dict_type, 0 AS status, 'primary' AS color_type, '' AS css_class, '' AS remark, 'admin' AS creator, NOW() AS create_time, 'admin' AS updater, NOW() AS update_time, 0 AS deleted
    UNION ALL SELECT 2, '拣货区', '20', 'wms_zone_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0
    UNION ALL SELECT 3, '退货区', '30', 'wms_zone_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0
    UNION ALL SELECT 4, '不合格品区', '40', 'wms_zone_type', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), 0
) src
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data d WHERE d.dict_type = 'wms_zone_type' AND d.value = src.value
);

-- 库位类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 'WMS 库位类型', 'wms_location_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'WMS 库位类型字典'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_type t WHERE t.type = 'wms_location_type');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT * FROM (
    SELECT 1 AS sort, '储位' AS label, '10' AS value, 'wms_location_type' AS dict_type, 0 AS status, 'primary' AS color_type, '' AS css_class, '' AS remark, 'admin' AS creator, NOW() AS create_time, 'admin' AS updater, NOW() AS update_time, 0 AS deleted
    UNION ALL SELECT 2, '拣货位', '20', 'wms_location_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0
    UNION ALL SELECT 3, '收货位', '30', 'wms_location_type', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), 0
    UNION ALL SELECT 4, '发货位', '40', 'wms_location_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0
) src
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data d WHERE d.dict_type = 'wms_location_type' AND d.value = src.value
);

-- 库位状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 'WMS 库位状态', 'wms_location_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'WMS 库位状态字典'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_type t WHERE t.type = 'wms_location_status');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
SELECT * FROM (
    SELECT 1 AS sort, '空闲' AS label, '10' AS value, 'wms_location_status' AS dict_type, 0 AS status, 'success' AS color_type, '' AS css_class, '' AS remark, 'admin' AS creator, NOW() AS create_time, 'admin' AS updater, NOW() AS update_time, 0 AS deleted
    UNION ALL SELECT 2, '占用', '20', 'wms_location_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0
    UNION ALL SELECT 3, '锁定', '30', 'wms_location_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0
    UNION ALL SELECT 4, '禁用', '40', 'wms_location_status', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), 0
) src
WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_data d WHERE d.dict_type = 'wms_location_status' AND d.value = src.value
);

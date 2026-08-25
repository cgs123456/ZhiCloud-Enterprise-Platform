-- ============================================================
-- V68: TMS GPS 定位模块
--
-- 新增 1 张表：
--   tms_gps_position   GPS 定位记录
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

CREATE TABLE IF NOT EXISTS tms_gps_position (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    vehicle_id BIGINT NOT NULL COMMENT '车辆编号',
    shipment_id BIGINT COMMENT '运单编号',
    longitude DECIMAL(12,6) NOT NULL COMMENT '经度',
    latitude DECIMAL(12,6) NOT NULL COMMENT '纬度',
    speed DECIMAL(10,2) COMMENT '速度（km/h）',
    direction DECIMAL(10,2) COMMENT '方向（0-360度，0=正北）',
    report_time DATETIME NOT NULL COMMENT '上报时间',
    location_desc VARCHAR(500) COMMENT '位置描述',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_vehicle_id (vehicle_id),
    KEY idx_shipment_id (shipment_id),
    KEY idx_report_time (report_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS GPS 定位记录';

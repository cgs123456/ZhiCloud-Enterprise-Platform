-- ============================================================
-- V58: TMS 运输管理模块初始化
--
-- 新增 6 张表：
--   tms_carrier          承运商
--   tms_vehicle          车辆
--   tms_driver           司机
--   tms_shipment         运单
--   tms_shipment_stop    运单站点
--   tms_tracking_event   跟踪事件
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. 承运商
CREATE TABLE IF NOT EXISTS tms_carrier (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(200) NOT NULL COMMENT '承运商名称',
    code VARCHAR(50) NOT NULL COMMENT '承运商编码',
    service_type TINYINT COMMENT '服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）',
    contact_person VARCHAR(100) COMMENT '联系人',
    contact_phone VARCHAR(50) COMMENT '联系电话',
    qualification_no VARCHAR(100) COMMENT '资质编号',
    rating TINYINT COMMENT '评分（1-5）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 启用 / 20 停用）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 承运商';

-- 2. 车辆
CREATE TABLE IF NOT EXISTS tms_vehicle (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    plate_no VARCHAR(50) NOT NULL COMMENT '车牌号',
    vehicle_type VARCHAR(100) COMMENT '车型',
    carrier_id BIGINT COMMENT '承运商编号',
    load_capacity DECIMAL(20,4) COMMENT '载重',
    volume DECIMAL(20,4) COMMENT '容积',
    driver_user_id BIGINT COMMENT '司机编号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 可用 / 20 运输中 / 30 维修中）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_plate_no (tenant_id, plate_no, deleted),
    KEY idx_carrier_id (carrier_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 车辆';

-- 3. 司机
CREATE TABLE IF NOT EXISTS tms_driver (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(100) NOT NULL COMMENT '司机姓名',
    phone VARCHAR(50) COMMENT '手机号',
    license_no VARCHAR(100) COMMENT '驾照号',
    license_type VARCHAR(50) COMMENT '准驾车型',
    carrier_id BIGINT COMMENT '承运商编号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 可用 / 20 运输中 / 30 休假）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_license_no (tenant_id, license_no, deleted),
    KEY idx_carrier_id (carrier_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 司机';

-- 4. 运单
CREATE TABLE IF NOT EXISTS tms_shipment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '运单号',
    carrier_id BIGINT COMMENT '承运商编号',
    vehicle_id BIGINT COMMENT '车辆编号',
    driver_id BIGINT COMMENT '司机编号',
    origin_address VARCHAR(500) COMMENT '起点地址',
    destination_address VARCHAR(500) COMMENT '终点地址',
    shipment_type TINYINT COMMENT '运单类型（10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货）',
    source_order_no VARCHAR(50) COMMENT '来源单据号',
    total_quantity DECIMAL(20,4) COMMENT '合计数量',
    total_weight DECIMAL(20,4) COMMENT '合计重量',
    total_volume DECIMAL(20,4) COMMENT '合计体积',
    freight_amount DECIMAL(20,4) COMMENT '运费金额',
    departure_time DATETIME COMMENT '发车时间',
    estimated_arrival_time DATETIME COMMENT '预计到达时间',
    actual_arrival_time DATETIME COMMENT '实际到达时间',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待发车 / 20 运输中 / 30 已到达 / 40 已签收 / 50 已取消）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_carrier_id (carrier_id),
    KEY idx_vehicle_id (vehicle_id),
    KEY idx_driver_id (driver_id),
    KEY idx_status (status),
    KEY idx_source_order_no (source_order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运单';

-- 5. 运单站点
CREATE TABLE IF NOT EXISTS tms_shipment_stop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    shipment_id BIGINT NOT NULL COMMENT '运单编号',
    sequence_no INT COMMENT '站点顺序',
    address VARCHAR(500) COMMENT '站点地址',
    arrival_time DATETIME COMMENT '到达时间',
    departure_time DATETIME COMMENT '离开时间',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_shipment_id (shipment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 运单站点';

-- 6. 跟踪事件
CREATE TABLE IF NOT EXISTS tms_tracking_event (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    shipment_id BIGINT NOT NULL COMMENT '运单编号',
    event_type TINYINT COMMENT '事件类型（10 发车 / 20 到达站点 / 30 签收 / 40 异常报告）',
    event_time DATETIME COMMENT '事件时间',
    location VARCHAR(500) COMMENT '当前位置',
    longitude DECIMAL(12,6) COMMENT '经度',
    latitude DECIMAL(12,6) COMMENT '纬度',
    description VARCHAR(500) COMMENT '描述',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_shipment_id (shipment_id),
    KEY idx_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TMS 跟踪事件';

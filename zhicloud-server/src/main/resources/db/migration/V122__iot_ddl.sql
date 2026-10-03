-- ======================== IoT 物联网建表脚本 ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- IoT 告警配置 DO (IotAlertConfigDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_alert_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '配置名称',
    description VARCHAR(512) COMMENT '配置描述',
    level INT COMMENT '配置状态',
    status INT COMMENT '配置状态',
    scene_rule_ids TEXT COMMENT '关联的场景联动规则编号数组',
    receive_user_ids TEXT COMMENT '接收的用户编号数组',
    receive_types TEXT COMMENT '接收的类型数组',
    sms_template_code VARCHAR(64) COMMENT '短信模板编号',
    mail_template_code VARCHAR(64) COMMENT '邮件模板编号',
    notify_template_code VARCHAR(64) COMMENT '站内信模板编号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 告警配置 DO';

-- ----------------------------
-- IoT 告警记录 DO (IotAlertRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_alert_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录编号',
    PRIMARY KEY (id),
    config_id BIGINT COMMENT '告警名称',
    config_name VARCHAR(255) COMMENT '告警名称',
    config_level INT COMMENT '告警级别',
    scene_rule_id BIGINT COMMENT '场景规则编号',
    product_id BIGINT COMMENT '产品编号',
    device_id BIGINT COMMENT '设备编号',
    device_message TEXT COMMENT '触发的设备消息',
    process_status BIT(1) COMMENT '是否处理',
    process_remark VARCHAR(512) COMMENT '处理结果（备注）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 告警记录 DO';

-- ----------------------------
-- IoT 设备 DO (IotDeviceDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_device (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备 ID，主键，自增',
    PRIMARY KEY (id),
    device_name VARCHAR(255) COMMENT '设备名称，在产品内唯一，用于标识设备',
    nickname VARCHAR(128) COMMENT '设备备注名称',
    serial_number VARCHAR(255) COMMENT '设备序列号',
    pic_url VARCHAR(512) COMMENT '设备图片',
    group_ids TEXT COMMENT '设备分组编号集合',
    product_id BIGINT COMMENT '产品编号',
    product_key VARCHAR(64) COMMENT '产品标识',
    device_type INT COMMENT '设备类型',
    gateway_id BIGINT COMMENT '网关设备编号',
    state INT COMMENT '设备状态',
    online_time DATETIME COMMENT '最后上线时间',
    offline_time DATETIME COMMENT '最后离线时间',
    active_time DATETIME COMMENT '设备激活时间',
    firmware_id BIGINT COMMENT '固件编号',
    device_secret VARCHAR(64) COMMENT '设备密钥，用于设备认证',
    latitude DECIMAL(20,4) COMMENT '设备位置的纬度',
    longitude DECIMAL(20,4) COMMENT '设备位置的经度',
    config TEXT COMMENT '设备配置',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 设备 DO';

-- ----------------------------
-- IoT 设备分组 DO (IotDeviceGroupDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_device_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组 ID',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '分组名字',
    status INT COMMENT '分组状态',
    description VARCHAR(512) COMMENT '分组描述',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 设备分组 DO';

-- ----------------------------
-- IoT 设备 Modbus 连接配置 DO (IotDeviceModbusConfigDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_device_modbus_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    PRIMARY KEY (id),
    product_id BIGINT COMMENT '产品编号',
    device_id BIGINT COMMENT '设备编号',
    ip VARCHAR(255) COMMENT 'Modbus 服务器 IP 地址',
    port INT COMMENT 'Modbus 服务器端口',
    slave_id INT COMMENT '从站地址',
    timeout INT COMMENT '连接超时时间，单位：毫秒',
    retry_interval INT COMMENT '重试间隔，单位：毫秒',
    mode INT COMMENT '模式',
    frame_format INT COMMENT '数据帧格式',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 设备 Modbus 连接配置 DO';

-- ----------------------------
-- IoT 设备 Modbus 点位配置 DO (IotDeviceModbusPointDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_device_modbus_point (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    PRIMARY KEY (id),
    device_id BIGINT COMMENT '设备编号',
    thing_model_id BIGINT COMMENT '物模型属性编号',
    identifier VARCHAR(255) COMMENT '属性标识符',
    name VARCHAR(128) COMMENT '属性名称',
    function_code INT COMMENT 'Modbus 功能码',
    register_address INT COMMENT '寄存器起始地址',
    register_count INT COMMENT '寄存器数量',
    byte_order VARCHAR(255) COMMENT '字节序',
    raw_data_type VARCHAR(255) COMMENT '原始数据类型',
    scale DECIMAL(20,4) COMMENT '缩放因子',
    poll_interval INT COMMENT '轮询间隔（毫秒）',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 设备 Modbus 点位配置 DO';

-- ----------------------------
-- IoT OTA 固件 DO (IotOtaFirmwareDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_ota_firmware (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '固件编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '固件名称',
    description VARCHAR(512) COMMENT '固件描述',
    version VARCHAR(64) COMMENT '版本号',
    product_id BIGINT COMMENT '产品编号',
    file_url VARCHAR(512) COMMENT '固件文件 URL',
    file_size BIGINT COMMENT '固件文件大小',
    file_digest_algorithm VARCHAR(255) COMMENT '固件文件签名算法',
    file_digest_value VARCHAR(255) COMMENT '固件文件签名结果',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT OTA 固件 DO';

-- ----------------------------
-- IoT OTA 升级任务 DO (IotOtaTaskDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_ota_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '任务名称',
    description VARCHAR(512) COMMENT '任务描述',
    firmware_id BIGINT COMMENT '固件编号',
    status INT COMMENT '任务状态',
    device_scope INT COMMENT '设备升级范围',
    device_total_count INT COMMENT '设备总数数量',
    device_success_count INT COMMENT '设备成功数量',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT OTA 升级任务 DO';

-- ----------------------------
-- IoT OTA 升级任务记录 DO (IotOtaTaskRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_ota_task_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '升级记录编号',
    PRIMARY KEY (id),
    firmware_id BIGINT COMMENT '固件编号',
    task_id BIGINT COMMENT '任务编号',
    device_id BIGINT COMMENT '设备编号',
    from_firmware_id BIGINT COMMENT '来源的固件编号',
    status INT COMMENT '升级状态',
    progress INT COMMENT '升级进度，百分比',
    description VARCHAR(512) COMMENT '升级进度描述',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT OTA 升级任务记录 DO';

-- ----------------------------
-- IoT 产品分类 DO (IotProductCategoryDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_product_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类 ID',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '分类名字',
    sort INT COMMENT '分类排序',
    status INT COMMENT '分类状态',
    description VARCHAR(512) COMMENT '分类描述',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 产品分类 DO';

-- ----------------------------
-- IoT 产品 DO (IotProductDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品 ID',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '产品名称',
    product_key VARCHAR(64) COMMENT '产品标识',
    product_secret VARCHAR(64) COMMENT '产品密钥，用于一型一密动态注册',
    register_enabled BIT(1) COMMENT '是否开启动态注册',
    category_id BIGINT COMMENT '产品分类编号',
    icon VARCHAR(512) COMMENT '产品图标',
    pic_url VARCHAR(512) COMMENT '产品图片',
    description VARCHAR(512) COMMENT '产品描述',
    status INT COMMENT '产品状态',
    device_type INT COMMENT '设备类型',
    net_type INT COMMENT '联网方式',
    protocol_type VARCHAR(255) COMMENT '协议类型',
    serialize_type VARCHAR(255) COMMENT '序列化类型',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 产品 DO';

-- ----------------------------
-- IoT 数据流转规则 DO (IotDataRuleDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_data_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据流转规格编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '数据流转规格名称',
    description VARCHAR(512) COMMENT '数据流转规格描述',
    status INT COMMENT '数据流转规格状态',
    source_configs TEXT COMMENT '数据源配置数组',
    sink_ids TEXT COMMENT '数据目的编号数组',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 数据流转规则 DO';

-- ----------------------------
-- IoT 数据流转目的 DO (IotDataSinkDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_data_sink (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据流转目的编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '数据流转目的名称',
    description VARCHAR(512) COMMENT '数据流转目的描述',
    status INT COMMENT '数据流转目的状态',
    type INT COMMENT '数据流转目的类型',
    config TEXT COMMENT '数据流转目的配置',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 数据流转目的 DO';

-- ----------------------------
-- IoT 场景联动规则 DO (IotSceneRuleDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_scene_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '场景联动编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '场景联动名称',
    description VARCHAR(512) COMMENT '场景联动描述',
    status INT COMMENT '场景联动状态',
    last_trigger_time DATETIME COMMENT '最后触发时间',
    triggers TEXT COMMENT '场景定义配置',
    actions TEXT COMMENT '场景动作配置',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 场景联动规则 DO';

-- ----------------------------
-- IoT 产品物模型功能 DO (IotThingModelDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS iot_thing_model (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '物模型功能编号',
    PRIMARY KEY (id),
    identifier VARCHAR(255) COMMENT '功能标识',
    name VARCHAR(128) COMMENT '功能名称',
    description VARCHAR(512) COMMENT '功能描述',
    product_id BIGINT COMMENT '产品标识',
    product_key VARCHAR(64) COMMENT '产品标识',
    type INT COMMENT '功能类型',
    property TEXT COMMENT '属性',
    event TEXT COMMENT '事件',
    service TEXT COMMENT '服务',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='IoT 产品物模型功能 DO';

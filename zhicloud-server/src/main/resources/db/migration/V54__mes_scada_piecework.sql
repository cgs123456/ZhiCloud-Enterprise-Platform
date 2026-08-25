-- ============================================================
-- V54: MES SCADA 集成 + 计件工资
--
-- 新增 3 张表：
--   mes_dv_scada_config        SCADA 设备配置（MES 设备 ↔ IoT 设备映射）
--   mes_pro_piecework_rule     计件工资规则
--   mes_pro_piecework_record   计件工资明细
--
-- 变更 1 张表：
--   mes_dv_machinery           新增 iot_device_pk / protocol_type 字段
--
-- 兼容性：完全新增 + ADD COLUMN（IF NOT EXISTS），不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. SCADA 设备配置表
CREATE TABLE IF NOT EXISTS mes_dv_scada_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    machinery_id BIGINT NOT NULL COMMENT 'MES 设备编号（关联 mes_dv_machinery.id）',
    iot_device_pk VARCHAR(100) NOT NULL COMMENT 'IoT 平台设备 PK（唯一编码）',
    iot_product_id BIGINT COMMENT 'IoT 平台产品编号',
    protocol_type VARCHAR(20) NOT NULL COMMENT 'SCADA 协议类型（MQTT / MODBUS_TCP / OPC-UA）',
    point_config TEXT COMMENT '点位映射配置（JSON）',
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用（0 启用 1 停用）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_machinery (tenant_id, machinery_id, deleted),
    KEY idx_iot_device_pk (iot_device_pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES SCADA 设备配置';

-- 2. 计件工资规则表
CREATE TABLE IF NOT EXISTS mes_pro_piecework_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    process_id BIGINT COMMENT '工序编号（为空表示不限工序）',
    route_id BIGINT COMMENT '工艺路线编号（为空表示不限路线）',
    item_id BIGINT COMMENT '产品物料编号（为空表示不限产品）',
    workstation_id BIGINT COMMENT '工作站编号（为空表示不限工作站）',
    qualified_unit_price DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '合格品单价（元/件）',
    scrap_unit_price DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '废品单价（元/件）',
    step_config TEXT COMMENT '阶梯单价配置（JSON）',
    effective_date DATE NOT NULL COMMENT '生效日期',
    expire_date DATE NOT NULL COMMENT '失效日期',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 开启 1 关闭）',
    enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用（0 启用 1 停用）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_process_id (process_id),
    KEY idx_item_id (item_id),
    KEY idx_workstation_id (workstation_id),
    KEY idx_enabled_effective (enabled, effective_date, expire_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES 计件工资规则';

-- 3. 计件工资明细表
CREATE TABLE IF NOT EXISTS mes_pro_piecework_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    feedback_id BIGINT NOT NULL COMMENT '报工单编号（关联 mes_pro_feedback.id）',
    feedback_user_id BIGINT COMMENT '报工用户编号',
    work_order_id BIGINT COMMENT '生产工单编号',
    process_id BIGINT COMMENT '工序编号',
    item_id BIGINT COMMENT '产品物料编号',
    workstation_id BIGINT COMMENT '工作站编号',
    qualified_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '合格品数量',
    scrap_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '废品数量',
    labor_scrap_qty DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '工废数量',
    unit_price DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '合格品单价（元/件）',
    scrap_unit_price DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '废品单价（元/件）',
    total_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '工资金额合计',
    period_month VARCHAR(6) NOT NULL COMMENT '所属月份（yyyyMM）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 正常 1 作废）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_feedback (tenant_id, feedback_id, deleted),
    KEY idx_feedback_user_id (feedback_user_id),
    KEY idx_period_month (period_month),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES 计件工资明细';

-- 4. MES 设备台账新增 SCADA 集成字段
--    iot_device_pk：IoT 平台设备 PK，用于 MES 设备与 IoT 设备的映射
--    protocol_type：SCADA 协议类型（MQTT / MODBUS_TCP / OPC-UA）
-- 幂等新增列：mes_dv_machinery.iot_device_pk
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_dv_machinery' AND COLUMN_NAME = 'iot_device_pk'),
                  'DO 0',
                  'ALTER TABLE `mes_dv_machinery` ADD COLUMN `iot_device_pk` VARCHAR(100) COMMENT ''IoT 平台设备 PK'' AFTER last_check_time');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
-- 幂等新增列：mes_dv_machinery.protocol_type
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mes_dv_machinery' AND COLUMN_NAME = 'protocol_type'),
                  'DO 0',
                  'ALTER TABLE `mes_dv_machinery` ADD COLUMN `protocol_type` VARCHAR(20) COMMENT ''SCADA 协议类型（MQTT / MODBUS_TCP / OPC-UA）'' AFTER iot_device_pk');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;

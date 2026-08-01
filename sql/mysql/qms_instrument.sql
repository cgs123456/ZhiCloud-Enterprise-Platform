-- ======================== QMS 计量器具管理建表脚本 ========================
-- 作者：yudao
-- 说明：覆盖计量器具台账（qms_instrument）+ 校准记录（qms_instrument_calibration）
-- 依赖：qms.sql 基础表
-- 规范：InnoDB / utf8mb4 / utf8mb4_unicode_ci；主键 BIGINT（应用层雪花ID）；
--      统一含 tenant_id/creator/create_time/updater/update_time/deleted 审计字段。

-- ----------------------------
-- 计量器具台账表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_instrument (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '器具编号',
    name VARCHAR(255) NOT NULL COMMENT '器具名称',
    model VARCHAR(128) COMMENT '型号规格',
    manufacturer VARCHAR(255) COMMENT '生产厂家',
    serial_no VARCHAR(64) COMMENT '出厂编号',
    category TINYINT NOT NULL COMMENT '类别（10 长度类 20 温度类 30 力学类 40 电学类 50 光学类 60 其他）',
    accuracy VARCHAR(64) COMMENT '精度等级',
    measure_range VARCHAR(128) COMMENT '测量范围',
    unit VARCHAR(32) COMMENT '计量单位',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 在用 20 停用 30 报废 40 封存）',
    location VARCHAR(255) COMMENT '使用地点',
    responsible_person VARCHAR(64) COMMENT '负责人',
    calibration_cycle_days INT COMMENT '校准周期天数',
    last_calibration_date DATE COMMENT '上次校准日期',
    next_calibration_date DATE COMMENT '下次校准日期',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_code (code),
    KEY idx_next_calibration (next_calibration_date),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 计量器具台账表';

-- ----------------------------
-- 计量器具校准记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_instrument_calibration (
    id BIGINT PRIMARY KEY COMMENT '主键',
    instrument_id BIGINT NOT NULL COMMENT '器具 ID',
    calibration_no VARCHAR(64) NOT NULL COMMENT '校准证书编号',
    calibration_date DATE NOT NULL COMMENT '校准日期',
    calibration_organization VARCHAR(255) COMMENT '校准机构',
    calibration_result TINYINT NOT NULL COMMENT '校准结果（10 合格 20 不合格）',
    calibration_certificate_url VARCHAR(500) COMMENT '校准证书附件 URL',
    deviation DECIMAL(20,4) COMMENT '偏差值',
    uncertainty VARCHAR(64) COMMENT '不确定度',
    next_calibration_date DATE COMMENT '下次校准日期',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_calibration_no (calibration_no),
    KEY idx_instrument_id (instrument_id),
    KEY idx_calibration_date (calibration_date),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 计量器具校准记录表';

-- ----------------------------
-- 字典初始化
-- ----------------------------

-- 计量器具类别字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 计量器具类别', 'qms_instrument_category', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 计量器具类别字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '长度类', '10', 'qms_instrument_category', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '温度类', '20', 'qms_instrument_category', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '力学类', '30', 'qms_instrument_category', 0, 'info', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '电学类', '40', 'qms_instrument_category', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '光学类', '50', 'qms_instrument_category', 0, 'danger', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(6, '其他',   '60', 'qms_instrument_category', 0, '',        '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 计量器具状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 计量器具状态', 'qms_instrument_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 计量器具状态字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '在用', '10', 'qms_instrument_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '停用', '20', 'qms_instrument_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '报废', '30', 'qms_instrument_status', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '封存', '40', 'qms_instrument_status', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 校准结果字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 校准结果', 'qms_calibration_result', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 校准结果字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '合格',   '10', 'qms_calibration_result', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '不合格', '20', 'qms_calibration_result', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

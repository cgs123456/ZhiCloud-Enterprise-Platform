-- ============================================================
-- MES 返工工单建表脚本
-- 数据库：MySQL 8.0
-- 引擎：InnoDB / 字符集：utf8mb4 / 排序：utf8mb4_unicode_ci
-- 说明：覆盖 mes_pro_rework_order（返工工单主表）与 mes_pro_rework_order_detail（返工明细）
-- ============================================================

-- ----------------------------
-- MES 返工工单主表（mes_pro_rework_order）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_rework_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '返工工单号',
    original_work_order_id BIGINT DEFAULT NULL COMMENT '原工单 ID',
    original_work_order_code VARCHAR(64) DEFAULT NULL COMMENT '原工单号',
    product_id BIGINT DEFAULT NULL COMMENT '产品 ID',
    product_code VARCHAR(64) DEFAULT NULL COMMENT '产品编码',
    product_name VARCHAR(255) DEFAULT NULL COMMENT '产品名称',
    rework_quantity DECIMAL(20,4) DEFAULT NULL COMMENT '返工数量',
    rework_reason VARCHAR(500) DEFAULT NULL COMMENT '返工原因',
    rework_type INT DEFAULT NULL COMMENT '返工类型（10 生产返工 / 20 来料不良 / 30 客户退货返工）',
    rework_process_id BIGINT DEFAULT NULL COMMENT '返工工序 ID',
    rework_process_name VARCHAR(255) DEFAULT NULL COMMENT '返工工序名称',
    status INT DEFAULT 10 COMMENT '状态（10 待返工 / 20 返工中 / 30 已完成 / 40 已取消）',
    responsible_person_id BIGINT DEFAULT NULL COMMENT '责任人 ID',
    responsible_dept_id BIGINT DEFAULT NULL COMMENT '责任部门 ID',
    planned_start_time DATETIME DEFAULT NULL COMMENT '计划开始时间',
    planned_end_time DATETIME DEFAULT NULL COMMENT '计划结束时间',
    actual_start_time DATETIME DEFAULT NULL COMMENT '实际开始时间',
    actual_end_time DATETIME DEFAULT NULL COMMENT '实际结束时间',
    sort INT DEFAULT 0 COMMENT '显示顺序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_original_work_order_id (original_work_order_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 返工工单主表';

-- ----------------------------
-- MES 返工工单明细表（mes_pro_rework_order_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_rework_order_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rework_order_id BIGINT NOT NULL COMMENT '返工工单 ID',
    defect_description VARCHAR(500) DEFAULT NULL COMMENT '缺陷描述',
    defect_quantity DECIMAL(20,4) DEFAULT NULL COMMENT '缺陷数量',
    defect_type INT DEFAULT NULL COMMENT '缺陷类型（10 尺寸不良 / 20 外观不良 / 30 功能不良 / 40 性能不良 / 50 其他）',
    repair_method INT DEFAULT NULL COMMENT '处理方式（10 返修 / 20 降级 / 30 报废 / 40 重新加工）',
    repair_description VARCHAR(500) DEFAULT NULL COMMENT '处理描述',
    repaired_quantity DECIMAL(20,4) DEFAULT NULL COMMENT '已处理数量',
    sort INT DEFAULT 0 COMMENT '显示顺序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_rework_order_id (rework_order_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 返工工单明细表';

-- ============================================================
-- 字典数据（mes_rework_type / mes_rework_status / mes_defect_type / mes_repair_method）
-- 说明：以下为字典类型与字典数据插入脚本，需配合 system_dict_type 与 system_dict_data 表使用
-- ============================================================

-- ----------------------------
-- 字典类型：mes_rework_type（返工类型）
-- ----------------------------
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES ('MES 返工类型', 'mes_rework_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

INSERT IGNORE INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES
    (1, '生产返工', '10', 'mes_rework_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (2, '来料不良', '20', 'mes_rework_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (3, '客户退货返工', '30', 'mes_rework_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

-- ----------------------------
-- 字典类型：mes_rework_status（返工状态）
-- ----------------------------
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES ('MES 返工状态', 'mes_rework_status', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

INSERT IGNORE INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES
    (1, '待返工', '10', 'mes_rework_status', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (2, '返工中', '20', 'mes_rework_status', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (3, '已完成', '30', 'mes_rework_status', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (4, '已取消', '40', 'mes_rework_status', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

-- ----------------------------
-- 字典类型：mes_defect_type（缺陷类型）
-- ----------------------------
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES ('MES 缺陷类型', 'mes_defect_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

INSERT IGNORE INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES
    (1, '尺寸不良', '10', 'mes_defect_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (2, '外观不良', '20', 'mes_defect_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (3, '功能不良', '30', 'mes_defect_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (4, '性能不良', '40', 'mes_defect_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (5, '其他', '50', 'mes_defect_type', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

-- ----------------------------
-- 字典类型：mes_repair_method（处理方式）
-- ----------------------------
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES ('MES 处理方式', 'mes_repair_method', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

INSERT IGNORE INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES
    (1, '返修', '10', 'mes_repair_method', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (2, '降级', '20', 'mes_repair_method', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (3, '报废', '30', 'mes_repair_method', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00'),
    (4, '重新加工', '40', 'mes_repair_method', 0, '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00');

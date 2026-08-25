-- ============================================================
-- V61: MES SCADA 数据采集 + PDA 报工支持 + ECN 工程变更
--
-- 新增 3 张表：
--   mes_dv_device_data_record  设备数据采集记录（SCADA 实际采集结果）
--   mes_md_ecn_order            ECN 工程变更单主表
--   mes_md_ecn_order_item       ECN 工程变更明细
--
-- 兼容性：完全新增，使用 CREATE TABLE IF NOT EXISTS
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. 设备数据采集记录表（SCADA 实际采集结果）
CREATE TABLE IF NOT EXISTS mes_dv_device_data_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    machinery_id BIGINT NOT NULL COMMENT 'MES 设备编号（关联 mes_dv_machinery.id）',
    scada_config_id BIGINT NOT NULL COMMENT 'SCADA 配置编号（关联 mes_dv_scada_config.id）',
    property_name VARCHAR(100) NOT NULL COMMENT '属性名称（如 temperature / pressure / runStatus）',
    property_value VARCHAR(255) COMMENT '属性值（字符串形式）',
    data_type TINYINT NOT NULL DEFAULT 10 COMMENT '数据类型（10 数字 20 布尔 30 字符串）',
    collect_time DATETIME NOT NULL COMMENT '采集时间',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 正常 20 异常）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_machinery_collect (machinery_id, collect_time),
    KEY idx_scada_config_id (scada_config_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES 设备数据采集记录';

-- 2. ECN 工程变更单主表
CREATE TABLE IF NOT EXISTS mes_md_ecn_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(64) NOT NULL COMMENT 'ECN 单号',
    ecn_name VARCHAR(200) NOT NULL COMMENT '变更名称',
    change_type TINYINT NOT NULL COMMENT '变更类型（10 新增 BOM 20 修改 BOM 30 删除 BOM 40 替换物料）',
    bom_id BIGINT COMMENT '原 BOM 编号',
    new_bom_id BIGINT COMMENT '新 BOM 编号',
    change_reason VARCHAR(500) COMMENT '变更原因',
    change_description TEXT COMMENT '变更说明',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 审核中 30 已批准 40 已驳回 50 已执行）',
    applicant_user_id BIGINT COMMENT '申请人',
    approve_user_id BIGINT COMMENT '审批人',
    approve_date DATETIME COMMENT '审批日期',
    effective_date DATE COMMENT '生效日期',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_bom_id (bom_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES ECN 工程变更单';

-- 3. ECN 工程变更明细表
CREATE TABLE IF NOT EXISTS mes_md_ecn_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    ecn_order_id BIGINT NOT NULL COMMENT 'ECN 单编号（关联 mes_md_ecn_order.id）',
    change_item TINYINT NOT NULL COMMENT '变更项（10 物料 20 数量 30 工序 40 备注）',
    old_value VARCHAR(500) COMMENT '原值',
    new_value VARCHAR(500) COMMENT '新值',
    bom_detail_id BIGINT COMMENT '原 BOM 明细编号',
    new_bom_detail_id BIGINT COMMENT '新 BOM 明细编号',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_ecn_order_id (ecn_order_id),
    KEY idx_bom_detail_id (bom_detail_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES ECN 工程变更明细';

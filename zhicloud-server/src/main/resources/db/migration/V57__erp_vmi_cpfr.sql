-- ============================================================
-- V57: ERP VMI 供应商管理库存 + CPFR 联合计划预测补货
--
-- 新增 5 张表：
--   erp_vmi_inventory          VMI 供应商管理库存
--   erp_vmi_replenishment      VMI 补货建议主表
--   erp_vmi_replenishment_item VMI 补货建议明细
--   erp_cpfr_forecast          CPFR 联合计划预测
--   erp_cpfr_exception         CPFR 协同异常
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. VMI 供应商管理库存
CREATE TABLE IF NOT EXISTS erp_vmi_inventory (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    supplier_id BIGINT NOT NULL COMMENT '供应商编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称（冗余）',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '当前库存数量',
    available_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '可用库存数量',
    locked_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '锁定库存数量',
    min_quantity DECIMAL(20,4) COMMENT '最低库存',
    max_quantity DECIMAL(20,4) COMMENT '最高库存',
    replenishment_point DECIMAL(20,4) COMMENT '补货点',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_supplier_id (supplier_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP VMI 供应商管理库存';

-- 2. VMI 补货建议主表
CREATE TABLE IF NOT EXISTS erp_vmi_replenishment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '补货建议单号',
    supplier_id BIGINT NOT NULL COMMENT '供应商编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待处理 / 20 已生成采购订单 / 30 已完成）',
    total_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '合计数量',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_supplier_id (supplier_id),
    KEY idx_warehouse_id (warehouse_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP VMI 补货建议';

-- 3. VMI 补货建议明细
CREATE TABLE IF NOT EXISTS erp_vmi_replenishment_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    replenishment_id BIGINT NOT NULL COMMENT '补货建议编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称（冗余）',
    quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '建议补货数量',
    current_quantity DECIMAL(20,4) COMMENT '当前库存数量',
    suggested_quantity DECIMAL(20,4) COMMENT '系统建议补货数量',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_replenishment_id (replenishment_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP VMI 补货建议明细';

-- 4. CPFR 联合计划预测补货
CREATE TABLE IF NOT EXISTS erp_cpfr_forecast (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    no VARCHAR(50) NOT NULL COMMENT '预测单号',
    partner_type TINYINT NOT NULL COMMENT '合作伙伴类型（10 供应商 / 20 客户）',
    partner_id BIGINT NOT NULL COMMENT '合作伙伴编号',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    product_name VARCHAR(200) COMMENT '产品名称（冗余）',
    forecast_period VARCHAR(10) NOT NULL COMMENT '预测周期（yyyyMM）',
    forecast_quantity DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '预测数量',
    actual_quantity DECIMAL(20,4) COMMENT '实际数量',
    deviation_rate DECIMAL(10,4) COMMENT '偏差率',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, no, deleted),
    KEY idx_partner (partner_type, partner_id),
    KEY idx_product_id (product_id),
    KEY idx_forecast_period (forecast_period)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP CPFR 联合计划预测补货';

-- 5. CPFR 协同异常
CREATE TABLE IF NOT EXISTS erp_cpfr_exception (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    forecast_id BIGINT NOT NULL COMMENT '预测编号',
    exception_type TINYINT NOT NULL COMMENT '异常类型（10 预测偏差超限 / 20 库存异常 / 30 补货异常）',
    exception_description VARCHAR(500) COMMENT '异常描述',
    handling_status TINYINT NOT NULL DEFAULT 10 COMMENT '处理状态（10 待处理 / 20 处理中 / 30 已解决）',
    handler_user_id BIGINT COMMENT '处理人编号',
    handling_time DATETIME COMMENT '处理时间',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_forecast_id (forecast_id),
    KEY idx_exception_type (exception_type),
    KEY idx_handling_status (handling_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP CPFR 协同异常';

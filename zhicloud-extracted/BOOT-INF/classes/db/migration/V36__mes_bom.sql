-- ============================================================
-- V36: MES 独立 BOM 模块（P1）
--
-- 新增 2 张表：
--   mes_md_bom         BOM 主数据（工程BOM/制造BOM/虚拟件，多版本）
--   mes_md_bom_detail  BOM 明细（子件清单）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS
-- ============================================================

-- 1. BOM 主数据表
CREATE TABLE IF NOT EXISTS mes_md_bom (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    bom_no VARCHAR(64) NOT NULL COMMENT 'BOM 编号',
    product_id BIGINT NOT NULL COMMENT '产品编号（关联 mes_md_item.id）',
    bom_type VARCHAR(32) NOT NULL DEFAULT 'MANUFACTURING' COMMENT 'BOM 类型（ENGINEERING 工程BOM / MANUFACTURING 制造BOM / PHANTOM 虚拟件）',
    version VARCHAR(32) NOT NULL DEFAULT 'V1.0' COMMENT '版本号',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 启用 / 1 停用，CommonStatusEnum）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_bom_no (bom_no, tenant_id),
    KEY idx_product (product_id),
    KEY idx_bom_type (bom_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES 独立 BOM 主数据';

-- 2. BOM 明细表
CREATE TABLE IF NOT EXISTS mes_md_bom_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    bom_id BIGINT NOT NULL COMMENT 'BOM 主数据编号（关联 mes_md_bom.id）',
    product_id BIGINT NOT NULL COMMENT '子件产品编号（关联 mes_md_item.id）',
    quantity DECIMAL(20,6) NOT NULL COMMENT '用量',
    unit VARCHAR(20) COMMENT '单位',
    scrap_rate DECIMAL(10,4) DEFAULT 0 COMMENT '损耗率（百分比，0-100）',
    unit_cost DECIMAL(20,4) DEFAULT 0 COMMENT '标准单位成本（叶子件成本取数来源，供成本卷积）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_bom_id (bom_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MES BOM 明细（子件清单）';
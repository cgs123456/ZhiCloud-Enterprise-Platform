-- ============================================================
-- V31: ERP 合并报表自动抵消引擎（P1）
--
-- 新增 2 张表：
--   erp_consolidation_worksheet  合并工作底稿表（自动抵消分录结果）
--   erp_consolidation_scope      合并范围表（母子公司持股关系）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- ============================================================

-- 1. 合并工作底稿表
CREATE TABLE IF NOT EXISTS erp_consolidation_worksheet (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    consolidation_period VARCHAR(20) NOT NULL COMMENT '合并周期（yyyyMM，如 202607）',
    parent_company_id BIGINT NOT NULL COMMENT '母公司编号',
    subsidiary_company_id BIGINT NOT NULL COMMENT '子公司编号',
    elimination_type TINYINT NOT NULL COMMENT '抵消类型（10 投资权益 / 20 内部应收应付 / 30 内部销售成本 / 40 内部固定资产）',
    elimination_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '抵消金额',
    description VARCHAR(500) COMMENT '抵消描述',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待审核 / 20 已审核 / 30 已驳回）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_period (consolidation_period),
    KEY idx_parent_company (parent_company_id),
    KEY idx_subsidiary_company (subsidiary_company_id),
    KEY idx_elimination_type (elimination_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 合并工作底稿表';

-- 2. 合并范围表
CREATE TABLE IF NOT EXISTS erp_consolidation_scope (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    parent_company_id BIGINT NOT NULL COMMENT '母公司编号',
    subsidiary_company_id BIGINT NOT NULL COMMENT '子公司编号',
    holding_ratio DECIMAL(10,4) NOT NULL DEFAULT 1.0000 COMMENT '持股比例（0~1，例如 0.65 表示 65%）',
    consolidation_method TINYINT NOT NULL DEFAULT 10 COMMENT '合并方法（10 完全合并 / 20 比例合并 / 30 权益法）',
    effective_date DATE COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 启用 / 20 禁用）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_parent_subsidiary (tenant_id, parent_company_id, subsidiary_company_id, deleted),
    KEY idx_parent_company (parent_company_id),
    KEY idx_subsidiary_company (subsidiary_company_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 合并范围表';

-- ============================================================
-- 3. 字典初始化
-- ============================================================

-- 合并方法
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 合并方法', 'erp_consolidation_method', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 合并报表合并方法');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '完全合并', '10', 'erp_consolidation_method', 0, 'admin', NOW(), 'admin', NOW(), 0, '完全合并'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_method' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '比例合并', '20', 'erp_consolidation_method', 0, 'admin', NOW(), 'admin', NOW(), 0, '比例合并'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_method' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '权益法', '30', 'erp_consolidation_method', 0, 'admin', NOW(), 'admin', NOW(), 0, '权益法'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_method' AND value='30');

-- 工作底稿状态
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 合并工作底稿状态', 'erp_worksheet_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 合并工作底稿状态');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '待审核', '10', 'erp_worksheet_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '待审核'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_worksheet_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '已审核', '20', 'erp_worksheet_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已审核'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_worksheet_status' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '已驳回', '30', 'erp_worksheet_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已驳回'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_worksheet_status' AND value='30');

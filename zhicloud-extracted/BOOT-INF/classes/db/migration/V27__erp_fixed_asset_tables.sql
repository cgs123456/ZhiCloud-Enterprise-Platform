-- ============================================================
-- V27: ERP 固定资产管理模块（P0-14）
--
-- 新增 2 张表：
--   erp_fixed_asset                固定资产主数据表
--   erp_fixed_asset_depreciation    固定资产折旧记录表
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- ============================================================

-- 1. 固定资产主数据表
CREATE TABLE IF NOT EXISTS erp_fixed_asset (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    code VARCHAR(50) NOT NULL COMMENT '资产编码（如 FA-001）',
    name VARCHAR(100) NOT NULL COMMENT '资产名称',
    category VARCHAR(50) COMMENT '资产类别（如：机器设备/办公设备/车辆/房屋建筑物）',
    specification VARCHAR(200) COMMENT '规格型号',
    department_id BIGINT COMMENT '使用部门编号',
    location VARCHAR(200) COMMENT '存放地点',
    responsible_person VARCHAR(50) COMMENT '责任人',
    original_value DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '资产原值',
    salvage_value DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '预计残值',
    useful_life_months INT NOT NULL COMMENT '预计使用年限（月数）',
    depreciation_method TINYINT NOT NULL DEFAULT 10 COMMENT '折旧方法（10 直线法 / 20 DDB / 30 SYD）',
    capitalization_date DATE NOT NULL COMMENT '入账日期（开始折旧日期）',
    asset_account_id BIGINT COMMENT '对应资产科目编号',
    accumulated_depreciation_account_id BIGINT COMMENT '对应累计折旧科目编号',
    depreciation_expense_account_id BIGINT COMMENT '对应折旧费用科目编号',
    depreciated_months INT NOT NULL DEFAULT 0 COMMENT '已折旧月数',
    accumulated_depreciation DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '累计折旧金额',
    net_book_value DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '账面净值（原值 - 累计折旧）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '资产状态（10 在用 / 20 闲置 / 30 已处置 / 40 已报废）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted),
    KEY idx_category (category),
    KEY idx_status (status),
    KEY idx_department_id (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 固定资产主数据表';

-- 2. 固定资产折旧记录表
CREATE TABLE IF NOT EXISTS erp_fixed_asset_depreciation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    fixed_asset_id BIGINT NOT NULL COMMENT '固定资产编号',
    asset_code VARCHAR(50) COMMENT '资产编码（冗余）',
    asset_name VARCHAR(100) COMMENT '资产名称（冗余）',
    period_id BIGINT COMMENT '会计期间编号',
    period_code VARCHAR(20) COMMENT '期间编码（如 202607）',
    depreciation_date DATE COMMENT '折旧日期',
    depreciation_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '本月折旧额',
    accumulated_depreciation DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '累计折旧额（截至本月）',
    net_book_value DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '账面净值（本月末）',
    depreciated_months INT NOT NULL DEFAULT 0 COMMENT '已折旧月数（截至本月）',
    depreciation_method TINYINT NOT NULL DEFAULT 10 COMMENT '折旧方法（10 直线法）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待审核 / 20 已审核）',
    voucher_id BIGINT COMMENT '生成的凭证编号（审核时生成，关联 erp_gl_voucher.id）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_asset_period (tenant_id, fixed_asset_id, period_id, deleted),
    KEY idx_period_id (period_id),
    KEY idx_status (status),
    KEY idx_voucher_id (voucher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 固定资产折旧记录表';

-- 3. 字典数据：折旧方法
INSERT INTO system_dict_type (name, type, status, remark) SELECT
    'ERP 折旧方法', 'erp_depreciation_method', 0, 'ERP 固定资产折旧方法'
    FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type WHERE type = 'erp_depreciation_method' AND deleted = 0
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 1, '直线法', '10', 'erp_depreciation_method', 0, 'primary', '', '年限平均法'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_depreciation_method' AND value = '10' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 2, '双倍余额递减法', '20', 'erp_depreciation_method', 0, 'warning', '', 'DDB（预留）'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_depreciation_method' AND value = '20' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 3, '年数总和法', '30', 'erp_depreciation_method', 0, 'info', '', 'SYD（预留）'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_depreciation_method' AND value = '30' AND deleted = 0);

-- 4. 字典数据：固定资产状态
INSERT INTO system_dict_type (name, type, status, remark) SELECT
    'ERP 固定资产状态', 'erp_fixed_asset_status', 0, 'ERP 固定资产状态'
    FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type WHERE type = 'erp_fixed_asset_status' AND deleted = 0
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 1, '在用', '10', 'erp_fixed_asset_status', 0, 'success', '', '正常使用中'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fixed_asset_status' AND value = '10' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 2, '闲置', '20', 'erp_fixed_asset_status', 0, 'info', '', '暂停使用'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fixed_asset_status' AND value = '20' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 3, '已处置', '30', 'erp_fixed_asset_status', 0, 'warning', '', '已出售/转让'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fixed_asset_status' AND value = '30' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 4, '已报废', '40', 'erp_fixed_asset_status', 0, 'danger', '', '已报废清理'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fixed_asset_status' AND value = '40' AND deleted = 0);

-- 5. 字典数据：折旧记录状态
INSERT INTO system_dict_type (name, type, status, remark) SELECT
    'ERP 折旧记录状态', 'erp_fixed_asset_depreciation_status', 0, 'ERP 固定资产折旧记录状态'
    FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type WHERE type = 'erp_fixed_asset_depreciation_status' AND deleted = 0
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 1, '待审核', '10', 'erp_fixed_asset_depreciation_status', 0, 'warning', '', '草稿状态'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fixed_asset_depreciation_status' AND value = '10' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 2, '已审核', '20', 'erp_fixed_asset_depreciation_status', 0, 'success', '', '已审核，不可修改'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fixed_asset_depreciation_status' AND value = '20' AND deleted = 0);

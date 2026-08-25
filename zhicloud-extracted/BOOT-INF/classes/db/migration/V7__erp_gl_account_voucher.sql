-- ============================================================
-- V7: ERP 财务总账模块（P0-7）
--
-- 新增 3 张表：
--   erp_gl_account        会计科目表（树形结构）
--   erp_gl_voucher        会计凭证主表
--   erp_gl_voucher_entry  会计凭证分录表
--
-- 兼容性：完全新增，不影响历史数据
-- ============================================================

-- 1. 会计科目表（树形结构，支持资产/负债/权益/收入/费用/共同六大类）
CREATE TABLE IF NOT EXISTS erp_gl_account (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级编号（顶级为 0）',
    code VARCHAR(50) NOT NULL COMMENT '科目编码（如 1001）',
    name VARCHAR(100) NOT NULL COMMENT '科目名称（如 库存现金）',
    type TINYINT NOT NULL COMMENT '科目类型（10 资产 / 20 负债 / 30 权益 / 40 收入 / 50 费用 / 60 共同）',
    balance_direction TINYINT NOT NULL COMMENT '余额方向（10 借方 / 20 贷方 / 30 双向）',
    level INT NOT NULL DEFAULT 1 COMMENT '层级（顶级为 1）',
    is_leaf TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否末级科目（1 是 / 0 否）',
    opening_debit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '期初借方余额',
    opening_credit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '期初贷方余额',
    current_debit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '当前借方累计发生额',
    current_credit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '当前贷方累计发生额',
    closing_debit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '期末借方余额',
    closing_credit DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '期末贷方余额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 启用 / 1 禁用）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted),
    KEY idx_parent_id (parent_id),
    KEY idx_type (type),
    KEY idx_is_leaf (is_leaf)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 会计科目表';

-- 2. 会计凭证主表
CREATE TABLE IF NOT EXISTS erp_gl_voucher (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    voucher_no VARCHAR(50) NOT NULL COMMENT '凭证字号（如 记-202607-001）',
    voucher_date DATE NOT NULL COMMENT '凭证日期',
    period_id BIGINT COMMENT '会计期间编号',
    period_code VARCHAR(20) COMMENT '期间编码（冗余）',
    voucher_type TINYINT NOT NULL DEFAULT 40 COMMENT '凭证类型（10 收款 / 20 付款 / 30 转账 / 40 记账）',
    attachment_count INT DEFAULT 0 COMMENT '附件张数',
    summary VARCHAR(500) COMMENT '凭证摘要',
    debit_total DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '借方合计',
    credit_total DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '贷方合计',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 / 20 已审核 / 30 已反审核）',
    prepared_by VARCHAR(64) COMMENT '制单人',
    approved_by VARCHAR(64) COMMENT '审核人',
    approved_time DATETIME COMMENT '审核时间',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, voucher_no, deleted),
    KEY idx_voucher_date (voucher_date),
    KEY idx_period_id (period_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 会计凭证表';

-- 3. 会计凭证分录表
CREATE TABLE IF NOT EXISTS erp_gl_voucher_entry (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    voucher_id BIGINT NOT NULL COMMENT '凭证编号',
    account_id BIGINT NOT NULL COMMENT '科目编号',
    account_code VARCHAR(50) NOT NULL COMMENT '科目编码（冗余）',
    account_name VARCHAR(100) NOT NULL COMMENT '科目名称（冗余）',
    summary VARCHAR(500) COMMENT '摘要',
    debit_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '借方金额',
    credit_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '贷方金额',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序号',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_voucher_id (voucher_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 会计凭证分录表';

-- ============================================================
-- 4. 字典初始化
-- ============================================================

-- 会计科目类型
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 会计科目类型', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 会计科目六大类型');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '资产', '10', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '资产'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_type' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '负债', '20', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '负债'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_type' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '所有者权益', '30', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '所有者权益'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_type' AND value='30');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 4, '收入', '40', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '收入'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_type' AND value='40');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 5, '费用', '50', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '费用'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_type' AND value='50');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 6, '共同', '60', 'erp_gl_account_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '共同'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_type' AND value='60');

-- 余额方向
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 会计科目余额方向', 'erp_gl_account_direction', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 会计科目余额方向');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '借方', '10', 'erp_gl_account_direction', 0, 'admin', NOW(), 'admin', NOW(), 0, '借方'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_direction' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '贷方', '20', 'erp_gl_account_direction', 0, 'admin', NOW(), 'admin', NOW(), 0, '贷方'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_direction' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '双向', '30', 'erp_gl_account_direction', 0, 'admin', NOW(), 'admin', NOW(), 0, '双向'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_account_direction' AND value='30');

-- 凭证类型
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 会计凭证类型', 'erp_gl_voucher_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 会计凭证类型');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '收款凭证', '10', 'erp_gl_voucher_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '收款凭证'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_type' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '付款凭证', '20', 'erp_gl_voucher_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '付款凭证'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_type' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '转账凭证', '30', 'erp_gl_voucher_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '转账凭证'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_type' AND value='30');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 4, '记账凭证', '40', 'erp_gl_voucher_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '记账凭证'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_type' AND value='40');

-- 凭证状态
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 会计凭证状态', 'erp_gl_voucher_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 会计凭证状态');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '草稿', '10', 'erp_gl_voucher_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '草稿'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '已审核', '20', 'erp_gl_voucher_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已审核'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_status' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '已反审核', '30', 'erp_gl_voucher_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已反审核'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_gl_voucher_status' AND value='30');

-- ============================================================
-- 5. 菜单初始化（会计科目 + 会计凭证 两个二级菜单，挂在 ERP 财务模块下）
-- ============================================================

-- 5.1 会计科目菜单
INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('会计科目', '', 2, 60, 0, '/erp/gl-account', 'ep:notebook', 'erp/glAccount/index', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '科目查询', 'erp:gl-account:query', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计科目' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-account:query'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '科目创建', 'erp:gl-account:create', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计科目' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-account:create'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '科目更新', 'erp:gl-account:update', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计科目' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-account:update'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '科目删除', 'erp:gl-account:delete', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计科目' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-account:delete'
);

-- 5.2 会计凭证菜单
INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('会计凭证', '', 2, 70, 0, '/erp/gl-voucher', 'ep:document', 'erp/glVoucher/index', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '凭证查询', 'erp:gl-voucher:query', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计凭证' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-voucher:query'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '凭证创建', 'erp:gl-voucher:create', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计凭证' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-voucher:create'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '凭证更新', 'erp:gl-voucher:update', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计凭证' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-voucher:update'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '凭证删除', 'erp:gl-voucher:delete', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计凭证' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-voucher:delete'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '凭证审核', 'erp:gl-voucher:approve', 3, 5, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='会计凭证' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:gl-voucher:approve'
);

-- ============================================================
-- 6. 初始化常用会计科目（中国会计准则基础科目，遵循 1xxx 资产、2xxx 负债、4xxx 权益、6xxx 损益 编码规则）
-- ============================================================
INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1001', '库存现金', 10, 10, 1, 1, 0, 1, '资产类-库存现金', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='1001' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1002', '银行存款', 10, 10, 1, 1, 0, 2, '资产类-银行存款', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='1002' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1122', '应收账款', 10, 10, 1, 1, 0, 3, '资产类-应收账款', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='1122' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1401', '原材料', 10, 10, 1, 1, 0, 4, '资产类-原材料', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='1401' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '1405', '库存商品', 10, 10, 1, 1, 0, 5, '资产类-库存商品', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='1405' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '2202', '应付账款', 20, 20, 1, 1, 0, 6, '负债类-应付账款', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='2202' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '4001', '实收资本', 30, 20, 1, 1, 0, 7, '权益类-实收资本', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='4001' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '4103', '本年利润', 30, 20, 1, 1, 0, 8, '权益类-本年利润（损益结转入此科目）', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='4103' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '6001', '主营业务收入', 40, 20, 1, 1, 0, 9, '收入类-主营业务收入', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='6001' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '6401', '主营业务成本', 50, 10, 1, 1, 0, 10, '费用类-主营业务成本', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='6401' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '6601', '销售费用', 50, 10, 1, 1, 0, 11, '费用类-销售费用', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='6601' AND tenant_id=0);

INSERT INTO erp_gl_account (parent_id, code, name, type, balance_direction, level, is_leaf, status, sort, remark, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT 0, '6602', '管理费用', 50, 10, 1, 1, 0, 12, '费用类-管理费用', 'admin', NOW(), 'admin', NOW(), 0, 0
WHERE NOT EXISTS (SELECT 1 FROM erp_gl_account WHERE code='6602' AND tenant_id=0);

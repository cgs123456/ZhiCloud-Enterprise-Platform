-- ============================================================
-- V30: ERP 多账簿支持（P1）
--
-- 新增 1 张表：
--   erp_account_book  账簿主数据表（支持多会计准则并行账簿）
--
-- 修改 1 张表：
--   erp_gl_voucher   新增 account_book_id 字段（多账簿支持）
--
-- 兼容性：account_book_id 默认 NULL，空表示默认主账簿，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS + information_schema 列存在性检查
-- ============================================================

-- 1. 账簿主数据表
CREATE TABLE IF NOT EXISTS erp_account_book (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    code VARCHAR(50) NOT NULL COMMENT '账簿编码（如 BOOK-CAS、BOOK-IFRS）',
    name VARCHAR(100) NOT NULL COMMENT '账簿名称（如 中国会计准则账簿）',
    accounting_standard TINYINT NOT NULL DEFAULT 10 COMMENT '会计准则（10 CAS 中国会计准则 / 20 IFRS 国际财务报告准则 / 30 US_GAAP 美国会计准则 / 40 其他）',
    currency_id BIGINT COMMENT '本位币编号（关联 erp_currency.id）',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主账簿（同一会计准则下最多一个主账簿）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 启用 / 20 禁用）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_code (tenant_id, code, deleted),
    KEY idx_status (status),
    KEY idx_accounting_standard (accounting_standard)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 账簿主数据表（多账簿支持）';

-- ============================================================
-- 2. erp_gl_voucher 新增 account_book_id 字段（幂等性检查）
-- ============================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'erp_gl_voucher' AND column_name = 'account_book_id');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE erp_gl_voucher ADD COLUMN account_book_id BIGINT DEFAULT NULL COMMENT ''账簿 ID（多账簿支持，空表示默认主账簿）''',
    'SELECT ''Column account_book_id already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 3. 字典初始化
-- ============================================================

-- 会计准则
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 会计准则', 'erp_accounting_standard', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 会计准则类型');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '中国会计准则 (CAS)', '10', 'erp_accounting_standard', 0, 'admin', NOW(), 'admin', NOW(), 0, '中国会计准则'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_accounting_standard' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '国际财务报告准则 (IFRS)', '20', 'erp_accounting_standard', 0, 'admin', NOW(), 'admin', NOW(), 0, '国际财务报告准则'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_accounting_standard' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '美国会计准则 (US_GAAP)', '30', 'erp_accounting_standard', 0, 'admin', NOW(), 'admin', NOW(), 0, '美国会计准则'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_accounting_standard' AND value='30');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 4, '其他', '40', 'erp_accounting_standard', 0, 'admin', NOW(), 'admin', NOW(), 0, '其他会计准则'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_accounting_standard' AND value='40');

-- 账簿状态
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 账簿状态', 'erp_account_book_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 账簿启用状态');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '启用', '10', 'erp_account_book_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '启用'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_account_book_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '禁用', '20', 'erp_account_book_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '禁用'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_account_book_status' AND value='20');

-- ============================================================
-- 4. 菜单初始化（账簿管理，挂在 ERP 财务模块下）
-- ============================================================

INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('账簿管理', '', 2, 90, 0, '/erp/account-book', 'ep:notebook', 'erp/accountBook/index', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '账簿查询', 'erp:account-book:query', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='账簿管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:account-book:query'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '账簿创建', 'erp:account-book:create', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='账簿管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:account-book:create'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '账簿更新', 'erp:account-book:update', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='账簿管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:account-book:update'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '账簿删除', 'erp:account-book:delete', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='账簿管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:account-book:delete'
);

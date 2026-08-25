-- ============================================================
-- V25: ERP 预算管理模块（P0-14）
--
-- 新增 2 张表：
--   erp_budget          预算主表（按年度/期间/部门/类型制定预算）
--   erp_budget_detail   预算明细表（按会计科目拆分预算金额）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- ============================================================

-- 1. 预算主表
CREATE TABLE IF NOT EXISTS erp_budget (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    budget_no VARCHAR(50) NOT NULL COMMENT '预算编号（如 BUD-2026-001）',
    budget_year INT NOT NULL COMMENT '预算年度（如 2026）',
    period_id BIGINT COMMENT '会计期间编号（空表示年度预算）',
    period_code VARCHAR(20) COMMENT '期间编码（冗余，如 202607 或 2026）',
    department_id BIGINT COMMENT '部门编号',
    budget_type TINYINT NOT NULL DEFAULT 10 COMMENT '预算类型（10 运营预算 / 20 资本预算 / 30 现金流预算）',
    total_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '预算总额（所有明细金额之和）',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 / 20 已审批 / 30 执行中 / 40 已关闭）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_no (tenant_id, budget_no, deleted),
    KEY idx_budget_year (budget_year),
    KEY idx_period_id (period_id),
    KEY idx_department_id (department_id),
    KEY idx_budget_type (budget_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预算主表';

-- 2. 预算明细表
CREATE TABLE IF NOT EXISTS erp_budget_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    budget_id BIGINT NOT NULL COMMENT '预算主表编号',
    account_id BIGINT NOT NULL COMMENT '会计科目编号',
    account_code VARCHAR(50) COMMENT '科目编码（冗余）',
    account_name VARCHAR(100) COMMENT '科目名称（冗余）',
    budget_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '预算金额（借方方向）',
    actual_amount DECIMAL(20,4) COMMENT '实际金额（由系统汇总 GL 凭证填入）',
    variance_amount DECIMAL(20,4) COMMENT '差异金额（actualAmount - budgetAmount）',
    variance_rate DECIMAL(10,4) COMMENT '差异率（varianceAmount / budgetAmount）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_budget_id (budget_id),
    KEY idx_account_id (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预算明细表';

-- ============================================================
-- 3. 字典初始化
-- ============================================================

-- 预算类型
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 预算类型', 'erp_budget_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 预算类型');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '运营预算', '10', 'erp_budget_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '运营预算'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_type' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '资本预算', '20', 'erp_budget_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '资本预算'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_type' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '现金流预算', '30', 'erp_budget_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '现金流预算'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_type' AND value='30');

-- 预算状态
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 预算状态', 'erp_budget_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 预算状态');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '草稿', '10', 'erp_budget_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '草稿'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '已审批', '20', 'erp_budget_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已审批'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_status' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '执行中', '30', 'erp_budget_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '执行中'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_status' AND value='30');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 4, '已关闭', '40', 'erp_budget_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已关闭'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_budget_status' AND value='40');

-- ============================================================
-- 4. 菜单初始化（预算管理二级菜单，挂在 ERP 财务模块下）
-- ============================================================

INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('预算管理', '', 2, 80, 0, '/erp/budget', 'ep:money', 'erp/budget/index', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '预算查询', 'erp:budget:query', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='预算管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:budget:query'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '预算创建', 'erp:budget:create', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='预算管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:budget:create'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '预算更新', 'erp:budget:update', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='预算管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:budget:update'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '预算删除', 'erp:budget:delete', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='预算管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:budget:delete'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '预算审批', 'erp:budget:approve', 3, 5, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='预算管理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:budget:approve'
);

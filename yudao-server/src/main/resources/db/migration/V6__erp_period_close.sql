-- ============================================================
-- V6: ERP 期末处理链（P0-6）
--
-- 新增 2 张表：
--   erp_period       会计期间表
--   erp_period_close 期末处理记录表
--
-- 兼容性：完全新增，不影响历史数据
-- ============================================================

-- 1. 会计期间表
CREATE TABLE IF NOT EXISTS erp_period (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    year INT NOT NULL COMMENT '年度',
    month INT NOT NULL COMMENT '月份（1-12）',
    code VARCHAR(20) NOT NULL COMMENT '期间编码（如 202607）',
    start_date DATE NOT NULL COMMENT '起始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 开放 / 20 结账中 / 30 已关账）',
    closed_by VARCHAR(64) COMMENT '关账人',
    closed_time DATETIME COMMENT '关账时间',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted),
    KEY idx_year_month (year, month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 会计期间表';

-- 2. 期末处理记录表
CREATE TABLE IF NOT EXISTS erp_period_close (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    period_id BIGINT NOT NULL COMMENT '期间编号',
    period_code VARCHAR(20) NOT NULL COMMENT '期间编码（冗余）',
    type TINYINT NOT NULL COMMENT '处理类型（10 月末检查 / 20 调汇 / 30 损益结转）',
    executed_by VARCHAR(64) COMMENT '执行人',
    executed_time DATETIME COMMENT '执行时间',
    process_status TINYINT NOT NULL DEFAULT 10 COMMENT '处理状态（10 成功 / 20 跳过 / 30 失败）',
    summary TEXT COMMENT '关键数据摘要（JSON）',
    adjustment_amount DECIMAL(20,4) DEFAULT 0 COMMENT '调整金额（仅调汇）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_period_type (tenant_id, period_id, type, deleted),
    KEY idx_period_code (period_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 期末处理记录表';

-- 3. 字典初始化
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 期间状态', 'erp_period_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 会计期间状态');

INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 期末处理类型', 'erp_period_close_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 期末处理类型');

INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 期末处理状态', 'erp_period_close_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 期末处理执行状态');

-- 期间状态字典数据
INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '开放', '10', 'erp_period_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '开放'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '结账中', '20', 'erp_period_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '结账中'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_status' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '已关账', '30', 'erp_period_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已关账'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_status' AND value='30');

-- 期末处理类型字典数据
INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '月末检查', '10', 'erp_period_close_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '月末检查'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_close_type' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '调汇', '20', 'erp_period_close_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '调汇'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_close_type' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '损益结转', '30', 'erp_period_close_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '损益结转'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_close_type' AND value='30');

-- 期末处理状态字典数据
INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '成功', '10', 'erp_period_close_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '成功'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_close_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '跳过', '20', 'erp_period_close_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '跳过'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_close_status' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '失败', '30', 'erp_period_close_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '失败'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_period_close_status' AND value='30');

-- 4. 菜单初始化（ERP 期末处理菜单 + 4 个按钮权限）
INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('期末处理', '', 2, 50, 0, '/erp/period-close', 'ep:calendar', 'erp/periodClose/index', 0, 'admin', NOW(), 'admin', NOW(), 0);

-- 按钮权限（假设期末处理菜单 parent_id 通过子查询获取）
INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '期间查询', 'erp:period:query', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='期末处理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:period:query'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '创建期间', 'erp:period:create', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='期末处理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:period:create'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '执行月末检查', 'erp:period:month-check', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='期末处理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:period:month-check'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '执行调汇', 'erp:period:revalue', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='期末处理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:period:revalue'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '执行损益结转', 'erp:period:transfer', 3, 5, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='期末处理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:period:transfer'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '关账', 'erp:period:close', 3, 6, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='期末处理' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:period:close'
);

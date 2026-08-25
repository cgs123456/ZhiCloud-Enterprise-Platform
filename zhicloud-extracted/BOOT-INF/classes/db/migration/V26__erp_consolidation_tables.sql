-- ============================================================
-- V26: ERP 合并报表抵消分录模块（P0-14）
--
-- 新增 1 张表：
--   erp_consolidation_entry  合并报表抵消分录表
--
-- 典型场景：
--   1) 母公司对子公司投资 ↔ 子公司权益
--   2) 集团内部应收/应付
--   3) 集团内部销售/采购收入成本抵消
--   4) 集团内部固定资产交易未实现利润
--
-- 兼容性：完全新增，不影响历史数据
-- ============================================================

-- 1. 合并报表抵消分录表
CREATE TABLE IF NOT EXISTS erp_consolidation_entry (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    consolidation_no VARCHAR(50) NOT NULL COMMENT '合并任务编号（如 CONS-202607）',
    period_id BIGINT COMMENT '会计期间编号',
    period_code VARCHAR(20) COMMENT '期间编码（冗余）',
    elimination_type TINYINT NOT NULL COMMENT '抵消类型（10 投资权益 / 20 内部应收应付 / 30 内部销售成本 / 40 内部固定资产）',
    debit_account_id BIGINT COMMENT '借方科目编号',
    debit_account_code VARCHAR(50) COMMENT '借方科目编码（冗余）',
    debit_account_name VARCHAR(100) COMMENT '借方科目名称（冗余）',
    credit_account_id BIGINT COMMENT '贷方科目编号',
    credit_account_code VARCHAR(50) COMMENT '贷方科目编码（冗余）',
    credit_account_name VARCHAR(100) COMMENT '贷方科目名称（冗余）',
    elimination_amount DECIMAL(20,4) NOT NULL DEFAULT 0 COMMENT '抵消金额',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 / 20 已审核）',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_consolidation_no (consolidation_no),
    KEY idx_period_id (period_id),
    KEY idx_elimination_type (elimination_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 合并报表抵消分录表';

-- ============================================================
-- 2. 字典初始化
-- ============================================================

-- 抵消类型
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 合并抵消类型', 'erp_consolidation_elimination_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 合并报表抵消分录类型');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '投资权益抵消', '10', 'erp_consolidation_elimination_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '投资权益抵消'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_elimination_type' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '内部应收应付抵消', '20', 'erp_consolidation_elimination_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '内部应收应付抵消'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_elimination_type' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '内部销售成本抵消', '30', 'erp_consolidation_elimination_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '内部销售成本抵消'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_elimination_type' AND value='30');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 4, '内部固定资产抵消', '40', 'erp_consolidation_elimination_type', 0, 'admin', NOW(), 'admin', NOW(), 0, '内部固定资产抵消'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_elimination_type' AND value='40');

-- 抵消分录状态
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('ERP 合并抵消分录状态', 'erp_consolidation_entry_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 'ERP 合并报表抵消分录状态');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '草稿', '10', 'erp_consolidation_entry_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '草稿'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_entry_status' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '已审核', '20', 'erp_consolidation_entry_status', 0, 'admin', NOW(), 'admin', NOW(), 0, '已审核'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='erp_consolidation_entry_status' AND value='20');

-- ============================================================
-- 3. 菜单初始化（合并报表抵消分录，挂在 ERP 财务模块下）
-- ============================================================

INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('合并抵消分录', '', 2, 80, 0, '/erp/consolidation-entry', 'ep:connection', 'erp/consolidationEntry/index', 0, 'admin', NOW(), 'admin', NOW(), 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '抵消分录查询', 'erp:consolidation-entry:query', 3, 1, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='合并抵消分录' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:consolidation-entry:query'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '抵消分录创建', 'erp:consolidation-entry:create', 3, 2, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='合并抵消分录' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:consolidation-entry:create'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '抵消分录更新', 'erp:consolidation-entry:update', 3, 3, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='合并抵消分录' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:consolidation-entry:update'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '抵消分录删除', 'erp:consolidation-entry:delete', 3, 4, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='合并抵消分录' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:consolidation-entry:delete'
);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
SELECT '抵消分录审核', 'erp:consolidation-entry:approve', 3, 5, m.id, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0
FROM system_menu m WHERE m.name='合并抵消分录' AND m.permission='' AND NOT EXISTS (
    SELECT 1 FROM system_menu WHERE permission='erp:consolidation-entry:approve'
);

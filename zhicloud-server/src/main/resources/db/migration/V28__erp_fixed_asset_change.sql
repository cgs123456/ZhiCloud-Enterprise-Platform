-- ============================================================
-- V28: ERP 固定资产变动记录 + 折旧凭证编号字段
--
-- 新增内容：
--   1. erp_fa_change                  固定资产变动记录表
--   2. erp_fixed_asset_depreciation   新增 voucher_no 字段（折旧审核时生成的凭证编号）
--   3. 字典数据：erp_fa_change_type     变动类型（10-60）
--   4. 字典数据：erp_fa_change_status   变动状态（10/20/30）
--
-- 兼容性：完全新增，不影响历史数据
-- 幂等性：使用 CREATE TABLE IF NOT EXISTS + INSERT ... WHERE NOT EXISTS
-- ============================================================

-- 1. 固定资产变动记录表
CREATE TABLE IF NOT EXISTS erp_fa_change (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    asset_id BIGINT NOT NULL COMMENT '固定资产编号（关联 erp_fixed_asset.id）',
    change_type TINYINT NOT NULL COMMENT '变动类型（10 部门转移 / 20 状态变动 / 30 原值调整 / 40 使用年限调整 / 50 残值调整 / 60 折旧方法变更）',
    before_value VARCHAR(500) COMMENT '变更前值',
    after_value VARCHAR(500) COMMENT '变更后值',
    change_date DATE COMMENT '变更日期',
    change_reason VARCHAR(500) COMMENT '变更原因',
    operator_id BIGINT COMMENT '操作员编号',
    approver_id BIGINT COMMENT '审批人编号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待审核 / 20 已审核 / 30 已驳回）',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    remark VARCHAR(500) COMMENT '备注',

    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_asset_id (asset_id),
    KEY idx_change_type (change_type),
    KEY idx_status (status),
    KEY idx_tenant_asset (tenant_id, asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 固定资产变动记录表';

-- 2. 为 erp_fixed_asset_depreciation 表添加 voucher_no 字段
--    使用存储过程实现幂等添加列
DROP PROCEDURE IF EXISTS p_erp_add_voucher_no_column;
DELIMITER $$
CREATE PROCEDURE p_erp_add_voucher_no_column()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = 'erp_fixed_asset_depreciation') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = 'erp_fixed_asset_depreciation'
                         AND column_name = 'voucher_no') THEN
            ALTER TABLE `erp_fixed_asset_depreciation`
                ADD COLUMN `voucher_no` VARCHAR(64) NULL COMMENT '凭证编号字符串（折旧审核时生成，占位实现）' AFTER `voucher_id`;
        END IF;
    END IF;
END$$
DELIMITER ;

CALL p_erp_add_voucher_no_column();
DROP PROCEDURE IF EXISTS p_erp_add_voucher_no_column;

-- 3. 字典数据：固定资产变动类型
INSERT INTO system_dict_type (name, type, status, remark) SELECT
    'ERP 固定资产变动类型', 'erp_fa_change_type', 0, 'ERP 固定资产变动类型'
    FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type WHERE type = 'erp_fa_change_type' AND deleted = 0
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 1, '部门转移', '10', 'erp_fa_change_type', 0, 'primary', '', '使用部门变更'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_type' AND value = '10' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 2, '状态变动', '20', 'erp_fa_change_type', 0, 'warning', '', '资产状态变更（在用/闲置/已处置/已报废）'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_type' AND value = '20' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 3, '原值调整', '30', 'erp_fa_change_type', 0, 'danger', '', '资产原值增减调整'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_type' AND value = '30' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 4, '使用年限调整', '40', 'erp_fa_change_type', 0, 'info', '', '预计使用年限月数调整'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_type' AND value = '40' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 5, '残值调整', '50', 'erp_fa_change_type', 0, 'info', '', '预计残值调整'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_type' AND value = '50' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 6, '折旧方法变更', '60', 'erp_fa_change_type', 0, 'success', '', '折旧方法变更（直线法/DDB/SYD）'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_type' AND value = '60' AND deleted = 0);

-- 4. 字典数据：固定资产变动状态
INSERT INTO system_dict_type (name, type, status, remark) SELECT
    'ERP 固定资产变动状态', 'erp_fa_change_status', 0, 'ERP 固定资产变动审批状态'
    FROM DUAL WHERE NOT EXISTS (
    SELECT 1 FROM system_dict_type WHERE type = 'erp_fa_change_status' AND deleted = 0
);

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 1, '待审核', '10', 'erp_fa_change_status', 0, 'warning', '', '草稿状态，待审批'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_status' AND value = '10' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 2, '已审核', '20', 'erp_fa_change_status', 0, 'success', '', '已审核，已实际更新资产字段'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_status' AND value = '20' AND deleted = 0);
INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark)
SELECT 3, '已驳回', '30', 'erp_fa_change_status', 0, 'danger', '', '已驳回，记录驳回原因'
    FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'erp_fa_change_status' AND value = '30' AND deleted = 0);

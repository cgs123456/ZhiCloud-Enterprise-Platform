-- ============================================================
-- V44: WMS 盘点类型扩展（P0-2）
--
-- 修改 1 张表：
--   wms_check_order  增加 check_type / cycle_days 字段
--
-- 新增 1 张表：
--   wms_check_cycle_plan  循环盘点计划（按 ABC 分类配置 cycle_days）
--
-- 兼容性：新增字段允许 NULL，历史数据视为明盘（OPEN=2）
-- 幂等性：使用 IF NOT EXISTS
-- ============================================================

-- ----------------------------
-- 1. 为 wms_check_order 增加字段
-- ----------------------------
-- 幂等新增列：wms_check_order.check_type
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_check_order' AND COLUMN_NAME = 'check_type'),
                  'DO 0',
                  'ALTER TABLE `wms_check_order` ADD COLUMN `check_type` TINYINT DEFAULT 2 COMMENT ''盘点类型（1 暗盘 BLIND / 2 明盘 OPEN / 3 循环盘点 CYCLE）'' AFTER status');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;

-- 幂等新增列：wms_check_order.cycle_days
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'wms_check_order' AND COLUMN_NAME = 'cycle_days'),
                  'DO 0',
                  'ALTER TABLE `wms_check_order` ADD COLUMN `cycle_days` INT COMMENT ''循环盘点周期天数（仅 CYCLE 类型使用）'' AFTER check_type');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;

-- ----------------------------
-- 2. 循环盘点计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS wms_check_cycle_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    abc_classification VARCHAR(8) NOT NULL COMMENT 'ABC 分类（A/B/C）',
    cycle_days INT NOT NULL COMMENT '循环周期天数（A 类默认 30 / B 类 60 / C 类 90）',
    next_check_date DATE COMMENT '下次盘点日期（Job 计算后回写）',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0 停用 1 启用）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_warehouse_abc (warehouse_id, abc_classification, tenant_id),
    KEY idx_abc (abc_classification),
    KEY idx_next_check (next_check_date),
    KEY idx_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='WMS 循环盘点计划';
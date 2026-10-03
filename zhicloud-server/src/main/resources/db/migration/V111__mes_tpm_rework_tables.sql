-- ============================================================
-- V111: MES TPM and rework tables
-- Source reviewed from sql/mysql/mes_tpm.sql and sql/mysql/mes_rework.sql.
-- The source scripts were not part of the Flyway migration chain.
-- No mock/business rows are inserted by this migration.
-- All ids use AUTO_INCREMENT because MyBatis-Plus IdType=AUTO is configured.
-- Business unique keys are tenant-scoped to prevent cross-tenant collisions.
-- ============================================================

CREATE TABLE IF NOT EXISTS `mes_dv_tp_plan` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID',
    plan_no VARCHAR(64) NOT NULL COMMENT '计划编号',
    plan_type TINYINT NOT NULL DEFAULT 10 COMMENT '计划类型（10 自主维护 AM 20 计划维护 PM 30 预测性维护 PdM）',
    cycle_type TINYINT NOT NULL DEFAULT 30 COMMENT '周期类型（10 日 20 周 30 月 40 季 50 年）',
    cycle_value INT DEFAULT 1 COMMENT '周期值',
    next_execute_date DATE COMMENT '下次执行日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 启用 20 禁用）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_plan_no` (`tenant_id`, `plan_no`),
    KEY `idx_tenant_equipment` (`tenant_id`, `equipment_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_next_execute` (`tenant_id`, `next_execute_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES TPM 计划表';

CREATE TABLE IF NOT EXISTS `mes_dv_tp_plan_item` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT 'TPM 计划 ID',
    item_name VARCHAR(128) NOT NULL COMMENT '项目名称',
    item_content VARCHAR(500) COMMENT '项目内容',
    standard VARCHAR(255) COMMENT '标准',
    method TINYINT DEFAULT 10 COMMENT '方法（10 目视 20 听觉 30 测量 40 操作）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_plan` (`tenant_id`, `plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES TPM 计划项目表';

CREATE TABLE IF NOT EXISTS `mes_dv_tp_record` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT 'TPM 计划 ID',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID',
    execute_date DATE COMMENT '执行日期',
    executor_id BIGINT COMMENT '执行人 ID',
    result TINYINT DEFAULT 10 COMMENT '结果（10 合格 20 异常 30 未执行）',
    issues_found VARCHAR(1000) COMMENT '发现问题',
    action_taken VARCHAR(1000) COMMENT '已采取措施',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_plan` (`tenant_id`, `plan_id`),
    KEY `idx_tenant_equipment` (`tenant_id`, `equipment_id`),
    KEY `idx_tenant_execute_date` (`tenant_id`, `execute_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES TPM 执行记录表';

CREATE TABLE IF NOT EXISTS `mes_dv_tp_kpi` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    equipment_id BIGINT NOT NULL COMMENT '设备 ID',
    period VARCHAR(8) NOT NULL COMMENT '周期（yyyyMM）',
    mtbf DECIMAL(20,4) DEFAULT 0 COMMENT '平均故障间隔时间（MTBF）',
    mttr DECIMAL(20,4) DEFAULT 0 COMMENT '平均修复时间（MTTR）',
    oee_improvement DECIMAL(10,4) DEFAULT 0 COMMENT 'OEE 改善值',
    planned_downtime DECIMAL(20,4) DEFAULT 0 COMMENT '计划停机时间',
    unplanned_downtime DECIMAL(20,4) DEFAULT 0 COMMENT '非计划停机时间',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_equipment_period` (`tenant_id`, `equipment_id`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES TPM KPI 指标表';

CREATE TABLE IF NOT EXISTS `mes_pro_rework_order` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '返工工单号',
    original_work_order_id BIGINT DEFAULT NULL COMMENT '原工单 ID',
    original_work_order_code VARCHAR(64) DEFAULT NULL COMMENT '原工单号',
    product_id BIGINT DEFAULT NULL COMMENT '产品 ID',
    product_code VARCHAR(64) DEFAULT NULL COMMENT '产品编码',
    product_name VARCHAR(255) DEFAULT NULL COMMENT '产品名称',
    rework_quantity DECIMAL(20,4) DEFAULT NULL COMMENT '返工数量',
    rework_reason VARCHAR(500) DEFAULT NULL COMMENT '返工原因',
    rework_type INT DEFAULT NULL COMMENT '返工类型（10 生产返工 / 20 来料不良 / 30 客户退货返工）',
    rework_process_id BIGINT DEFAULT NULL COMMENT '返工工序 ID',
    rework_process_name VARCHAR(255) DEFAULT NULL COMMENT '返工工序名称',
    status INT DEFAULT 10 COMMENT '状态（10 待返工 / 20 返工中 / 30 已完成 / 40 已取消）',
    responsible_person_id BIGINT DEFAULT NULL COMMENT '责任人 ID',
    responsible_dept_id BIGINT DEFAULT NULL COMMENT '责任部门 ID',
    planned_start_time DATETIME DEFAULT NULL COMMENT '计划开始时间',
    planned_end_time DATETIME DEFAULT NULL COMMENT '计划结束时间',
    actual_start_time DATETIME DEFAULT NULL COMMENT '实际开始时间',
    actual_end_time DATETIME DEFAULT NULL COMMENT '实际结束时间',
    sort INT DEFAULT 0 COMMENT '显示顺序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_code` (`tenant_id`, `code`),
    KEY `idx_tenant_original_work_order` (`tenant_id`, `original_work_order_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 返工工单主表';

CREATE TABLE IF NOT EXISTS `mes_pro_rework_order_detail` (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rework_order_id BIGINT NOT NULL COMMENT '返工工单 ID',
    defect_description VARCHAR(500) DEFAULT NULL COMMENT '缺陷描述',
    defect_quantity DECIMAL(20,4) DEFAULT NULL COMMENT '缺陷数量',
    defect_type INT DEFAULT NULL COMMENT '缺陷类型（10 尺寸不良 / 20 外观不良 / 30 功能不良 / 40 性能不良 / 50 其他）',
    repair_method INT DEFAULT NULL COMMENT '处理方式（10 返修 / 20 降级 / 30 报废 / 40 重新加工）',
    repair_description VARCHAR(500) DEFAULT NULL COMMENT '处理描述',
    repaired_quantity DECIMAL(20,4) DEFAULT NULL COMMENT '已处理数量',
    sort INT DEFAULT 0 COMMENT '显示顺序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_rework_order` (`tenant_id`, `rework_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 返工工单明细表';

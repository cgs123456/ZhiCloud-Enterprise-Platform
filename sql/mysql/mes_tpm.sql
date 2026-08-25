-- ======================== MES TPM 全员生产维护模块建表脚本 ========================
-- 作者：zhicloud
-- 说明：TPM 计划 / TPM 计划项目 / TPM 执行记录 / TPM KPI 指标
-- 注意：所有表均包含 IF NOT EXISTS，可重复执行

-- ----------------------------
-- 1. TPM 计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_tp_plan (
    id BIGINT NOT NULL COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id)
) COMMENT='MES TPM 计划表';

-- ----------------------------
-- 2. TPM 计划项目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_tp_plan_item (
    id BIGINT NOT NULL COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id)
) COMMENT='MES TPM 计划项目表';

-- ----------------------------
-- 3. TPM 执行记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_tp_record (
    id BIGINT NOT NULL COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id)
) COMMENT='MES TPM 执行记录表';

-- ----------------------------
-- 4. TPM KPI 指标表
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_tp_kpi (
    id BIGINT NOT NULL COMMENT '主键',
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
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id)
) COMMENT='MES TPM KPI 指标表';

-- ============================================================
-- 索引
-- ============================================================
CREATE UNIQUE INDEX uk_tp_plan_no ON mes_dv_tp_plan(plan_no);
CREATE INDEX idx_tp_equipment_id ON mes_dv_tp_plan(equipment_id);
CREATE INDEX idx_tp_plan_status ON mes_dv_tp_plan(status);
CREATE INDEX idx_tp_next_execute_date ON mes_dv_tp_plan(next_execute_date);
CREATE INDEX idx_tp_plan_id ON mes_dv_tp_plan_item(plan_id);
CREATE INDEX idx_tp_record_plan_id ON mes_dv_tp_record(plan_id);
CREATE INDEX idx_tp_record_equipment_id ON mes_dv_tp_record(equipment_id);
CREATE INDEX idx_tp_record_execute_date ON mes_dv_tp_record(execute_date);
CREATE INDEX idx_tp_kpi_equipment_id ON mes_dv_tp_kpi(equipment_id);
CREATE INDEX idx_tp_kpi_period ON mes_dv_tp_kpi(period);

-- ============================================================
-- 字典数据（仅给出 SQL 占位，具体字典管理通过系统字典管理界面维护）
-- 字典类型：
--   mes_tp_plan_type:     10 自主维护 AM / 20 计划维护 PM / 30 预测性维护 PdM
--   mes_tp_cycle_type:    10 日 / 20 周 / 30 月 / 40 季 / 50 年
--   mes_tp_plan_status:   10 启用 / 20 禁用
--   mes_tp_item_method:   10 目视 / 20 听觉 / 30 测量 / 40 操作
--   mes_tp_record_result: 10 合格 / 20 异常 / 30 未执行
-- ============================================================
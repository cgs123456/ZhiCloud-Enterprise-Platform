-- ======================== 质量管理系统（QMS）建表脚本 ========================
-- 作者：yudao
-- 说明：覆盖 IQC（来料检验）/IPQC（过程检验）/OQC（出货检验）/CAPA（纠正预防措施）

-- ----------------------------
-- 检验项目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_inspection_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '检验项目编码',
    name VARCHAR(255) NOT NULL COMMENT '检验项目名称',
    type TINYINT NOT NULL COMMENT '检验类型（10 IQC 来料检验 20 IPQC 过程检验 30 OQC 出货检验）',
    method TINYINT NOT NULL COMMENT '检验方法（10 外观 20 尺寸 30 功能 40 理化）',
    standard VARCHAR(500) COMMENT '检验标准',
    target VARCHAR(100) COMMENT '目标值',
    upper_limit DECIMAL(20,4) COMMENT '上限',
    lower_limit DECIMAL(20,4) COMMENT '下限',
    unit VARCHAR(32) COMMENT '单位',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 0 COMMENT '状态（0 启用 1 停用）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS 检验项目表';

-- ----------------------------
-- 检验单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_inspection_order (
    id BIGINT PRIMARY KEY COMMENT '主键',
    order_no VARCHAR(64) NOT NULL COMMENT '检验单号',
    type TINYINT NOT NULL COMMENT '检验类型（10 IQC 来料检验 20 IPQC 过程检验 30 OQC 出货检验）',
    supplier_id BIGINT COMMENT '供应商 ID',
    batch_no VARCHAR(64) COMMENT '批次号',
    work_order_id BIGINT COMMENT '工单 ID',
    product_id BIGINT COMMENT '产品 ID',
    inspector VARCHAR(64) COMMENT '检验员',
    inspect_time DATETIME COMMENT '检验时间',
    status TINYINT DEFAULT 10 COMMENT '状态（10 待检验 20 检验中 30 检验通过 40 检验不通过）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS 检验单表';

-- ----------------------------
-- 检验记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_inspection_record (
    id BIGINT PRIMARY KEY COMMENT '主键',
    order_id BIGINT NOT NULL COMMENT '检验单 ID',
    item_id BIGINT NOT NULL COMMENT '检验项目 ID',
    measured_value VARCHAR(100) COMMENT '实测值',
    result TINYINT NOT NULL COMMENT '检验结果（10 合格 20 不合格 30 不适用）',
    inspector VARCHAR(64) COMMENT '检验员',
    inspect_time DATETIME COMMENT '检验时间',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS 检验记录表';

-- ----------------------------
-- CAPA 文档表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_capa_document (
    id BIGINT PRIMARY KEY COMMENT '主键',
    capa_no VARCHAR(64) NOT NULL COMMENT 'CAPA 单号',
    source TINYINT NOT NULL COMMENT '来源（10 内部 20 外部 30 客户投诉 40 审核）',
    problem TEXT NOT NULL COMMENT '问题描述',
    cause TEXT COMMENT '原因',
    root_cause_analysis TEXT COMMENT '根本原因分析',
    corrective_action TEXT COMMENT '纠正措施',
    preventive_action TEXT COMMENT '预防措施',
    responsible_person VARCHAR(64) COMMENT '责任人',
    due_date DATETIME COMMENT '截止日期',
    close_date DATETIME COMMENT '关闭日期',
    status TINYINT DEFAULT 10 COMMENT '状态（10 待处理 20 处理中 30 已关闭）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS CAPA 纠正预防措施文档表';

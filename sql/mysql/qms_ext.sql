-- ======================== QMS 扩展模块建表脚本 ========================
-- 作者：yudao
-- 说明：覆盖 NCR（不合格品处理）/FMEA（失效模式分析）/MSA（测量系统分析）/电子签名
-- 依赖：qms.sql 基础表（qms_inspection_order 等）

-- ----------------------------
-- 不合格品报告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_ncr_document (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ncr_no VARCHAR(64) NOT NULL COMMENT 'NCR 单号',
    source TINYINT NOT NULL COMMENT '来源（10 IQC 来料检验 20 IPQC 过程检验 30 FQC 成品检验 40 OQC 出货检验 50 客户投诉）',
    inspection_order_id BIGINT COMMENT '检验单 ID',
    product_id BIGINT COMMENT '产品 ID',
    supplier_id BIGINT COMMENT '供应商 ID',
    work_order_id BIGINT COMMENT '工单 ID',
    defect_description TEXT NOT NULL COMMENT '缺陷描述',
    defect_level TINYINT COMMENT '缺陷等级（10 致命 20 严重 30 轻微）',
    quantity DECIMAL(20,4) COMMENT '不合格数量',
    disposition TINYINT COMMENT '处置方式（10 返工 20 返修 30 降级 40 报废 50 让步接收 60 退货）',
    status TINYINT DEFAULT 10 COMMENT '状态（10 待处理 20 MRB 评审中 30 已处置 40 已关闭）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS 不合格品报告表';

-- ----------------------------
-- MRB 物料评审委员会记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_ncr_mrb_record (
    id BIGINT PRIMARY KEY COMMENT '主键',
    ncr_id BIGINT NOT NULL COMMENT 'NCR 报告 ID',
    mrb_date DATETIME COMMENT '评审日期',
    mrb_members VARCHAR(500) COMMENT '评审成员',
    decision TINYINT COMMENT '决议（10 同意返工 20 同意返修 30 同意降级 40 报废 50 让步接收 60 退货）',
    condition_terms VARCHAR(1000) COMMENT '附加条件',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS MRB 物料评审委员会记录表';

-- ----------------------------
-- FMEA 文档表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_fmea_document (
    id BIGINT PRIMARY KEY COMMENT '主键',
    fmea_no VARCHAR(64) NOT NULL COMMENT 'FMEA 单号',
    fmea_type TINYINT NOT NULL COMMENT 'FMEA 类型（10 设计 DFMEA 20 过程 PFMEA）',
    product_id BIGINT COMMENT '产品 ID',
    process_id BIGINT COMMENT '工序 ID',
    version VARCHAR(32) COMMENT '版本',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 已评审 30 已批准）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS FMEA 失效模式分析文档表';

-- ----------------------------
-- FMEA 条目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_fmea_item (
    id BIGINT PRIMARY KEY COMMENT '主键',
    fmea_id BIGINT NOT NULL COMMENT 'FMEA 文档 ID',
    function VARCHAR(500) COMMENT '功能',
    failure_mode VARCHAR(500) NOT NULL COMMENT '失效模式',
    failure_effect VARCHAR(1000) COMMENT '失效后果',
    severity TINYINT NOT NULL COMMENT '严重度 S（1-10）',
    potential_cause VARCHAR(1000) COMMENT '潜在失效原因',
    occurrence TINYINT NOT NULL COMMENT '频度 O（1-10）',
    current_controls VARCHAR(1000) COMMENT '现行控制措施',
    detection TINYINT NOT NULL COMMENT '探测度 D（1-10）',
    rpn INT COMMENT '风险优先数 RPN = S * O * D（1-1000，自动计算）',
    action_recommended VARCHAR(1000) COMMENT '建议措施',
    action_taken VARCHAR(1000) COMMENT '已采取措施',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS FMEA 失效模式条目表';

-- ----------------------------
-- MSA 研究记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_msa_study (
    id BIGINT PRIMARY KEY COMMENT '主键',
    study_no VARCHAR(64) NOT NULL COMMENT '研究编号',
    study_type TINYINT NOT NULL COMMENT '研究类型（10 Gage R&R 类型 I 和 II 20 属性型）',
    characteristic_name VARCHAR(255) COMMENT '特性名称',
    equipment_id BIGINT COMMENT '测量设备 ID',
    appraiser_count INT COMMENT '评价人数量',
    trial_count INT COMMENT '试验次数',
    part_count INT COMMENT '零件数量',
    status TINYINT DEFAULT 10 COMMENT '状态（10 草稿 20 进行中 30 已完成）',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS MSA 测量系统分析研究表';

-- ----------------------------
-- MSA 测量数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_msa_measurement (
    id BIGINT PRIMARY KEY COMMENT '主键',
    study_id BIGINT NOT NULL COMMENT '研究 ID',
    part_id BIGINT COMMENT '零件 ID',
    appraiser_id BIGINT COMMENT '评价人 ID',
    trial_no INT COMMENT '试验序号',
    measurement_value DECIMAL(20,4) COMMENT '测量值',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS MSA 测量数据表';

-- ----------------------------
-- 电子签名记录表（21 CFR Part 11）
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_electronic_signature_log (
    id BIGINT PRIMARY KEY COMMENT '主键',
    user_id BIGINT COMMENT '用户 ID',
    signature_meaning VARCHAR(255) COMMENT '签名含义',
    operation_type VARCHAR(255) COMMENT '操作类型',
    operation_content VARCHAR(1000) COMMENT '操作内容',
    signature_time DATETIME COMMENT '签名时间',
    ip_address VARCHAR(64) COMMENT 'IP 地址',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS 电子签名记录表（21 CFR Part 11）';

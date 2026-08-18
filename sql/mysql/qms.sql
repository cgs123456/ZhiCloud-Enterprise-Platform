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
    priority TINYINT COMMENT '优先级（10 高 20 中 30 低）',
    stage TINYINT DEFAULT 10 COMMENT '当前阶段（10 已创建 20 根本原因分析 30 纠正措施 40 预防措施 50 有效性验证 60 已关闭）',
    problem TEXT NOT NULL COMMENT '问题描述',
    cause TEXT COMMENT '原因',
    root_cause_analysis TEXT COMMENT '根本原因分析',
    corrective_action TEXT COMMENT '纠正措施',
    preventive_action TEXT COMMENT '预防措施',
    responsible_person VARCHAR(64) COMMENT '责任人',
    due_date DATETIME COMMENT '截止日期',
    close_date DATETIME COMMENT '关闭日期',
    status TINYINT DEFAULT 10 COMMENT '状态（10 待处理 20 处理中 30 已关闭）',
    verification_result TINYINT COMMENT '有效性验证结果（10 待验证 20 通过 30 不通过）',
    verification_comment TEXT COMMENT '有效性验证意见',
    verified_by VARCHAR(64) COMMENT '验证人',
    verified_time DATETIME COMMENT '验证时间',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS CAPA 纠正预防措施文档表';

-- ----------------------------
-- 8D 报告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_eight_d_report (
    id BIGINT PRIMARY KEY COMMENT '主键',
    report_no VARCHAR(64) NOT NULL COMMENT '8D 报告编号',
    title VARCHAR(255) COMMENT '标题',
    ncr_id BIGINT COMMENT '关联 NCR 不合格品报告 ID',
    capa_id BIGINT COMMENT '关联 CAPA 纠正预防措施 ID',
    status TINYINT DEFAULT 0 COMMENT '状态（0 草稿 10 D1 成立团队 20 D2 描述问题 30 D3 临时遏制 40 D4 根本原因 50 D5 永久纠正 60 D6 实施验证 70 D7 预防再发 80 D8 关闭）',
    d1_team_members TEXT COMMENT 'D1 团队成员',
    d2_problem_description TEXT COMMENT 'D2 问题描述',
    d3_interim_action TEXT COMMENT 'D3 临时遏制措施',
    d4_root_cause TEXT COMMENT 'D4 根本原因分析',
    d5_permanent_action TEXT COMMENT 'D5 永久纠正措施',
    d6_implementation_result TEXT COMMENT 'D6 实施并验证结果',
    d7_prevention_action TEXT COMMENT 'D7 预防再发生措施',
    d8_team_recognition TEXT COMMENT 'D8 团队表彰',
    close_time DATETIME COMMENT '关闭时间',
    remark VARCHAR(500) COMMENT '备注',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT='QMS 8D 报告表';

-- ======================== QMS 检验单 AQL 判定增强（与 V60 迁移保持一致） ========================
ALTER TABLE qms_inspection_record
    ADD COLUMN severity TINYINT NOT NULL DEFAULT 30 COMMENT '缺陷严重度：10-致命(CRITICAL)/20-严重(MAJOR)/30-轻微(MINOR)';

ALTER TABLE qms_inspection_order
    ADD COLUMN acceptance_quantity INT DEFAULT NULL COMMENT 'AQL 接收数 Ac（缺陷数 <= Ac 判合格）',
    ADD COLUMN reject_quantity INT DEFAULT NULL COMMENT 'AQL 拒收数 Re（缺陷数 >= Re 判不合格）',
    ADD COLUMN biz_type VARCHAR(32) DEFAULT NULL COMMENT '业务类型（PURCHASE_IN/PRODUCTION_OUT/STOCK_COUNT/OTHER）',
    ADD COLUMN biz_id BIGINT DEFAULT NULL COMMENT '业务单据 ID';

CREATE INDEX idx_qms_inspection_order_biz ON qms_inspection_order (biz_type, biz_id);

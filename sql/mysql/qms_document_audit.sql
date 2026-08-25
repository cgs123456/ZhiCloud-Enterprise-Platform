-- ======================== QMS 文档控制 + 审核管理建表脚本 ========================
-- 作者：zhicloud
-- 说明：覆盖受控文档管理（document/distribute/change_request）+ 审核管理（plan/auditor/report/nonconformity）
-- 依赖：qms.sql 基础表
-- 规范：InnoDB / utf8mb4 / utf8mb4_unicode_ci；主键 BIGINT（应用层雪花ID）；
--      统一含 tenant_id/creator/create_time/updater/update_time/deleted 审计字段。

-- ----------------------------
-- 受控文档主表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_document (
    id BIGINT PRIMARY KEY COMMENT '主键',
    doc_no VARCHAR(64) NOT NULL COMMENT '文件编号',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    doc_type TINYINT NOT NULL COMMENT '文件类型（10 质量手册 20 程序文件 30 作业指导书 40 质量记录 50 外来文件）',
    version VARCHAR(32) NOT NULL DEFAULT '1.0' COMMENT '版本号',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 待审 30 已发布 40 已作废 50 已回收）',
    effective_date DATE COMMENT '生效日期',
    expiry_date DATE COMMENT '失效日期',
    approver_id BIGINT COMMENT '审批人 ID',
    approve_date DATETIME COMMENT '审批日期',
    owner_dept_id BIGINT COMMENT '归属部门 ID',
    file_url VARCHAR(500) COMMENT '文件 URL',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_doc_no (doc_no),
    KEY idx_doc_type (doc_type),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 受控文档主表';

-- ----------------------------
-- 文档分发记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_document_distribute (
    id BIGINT PRIMARY KEY COMMENT '主键',
    document_id BIGINT NOT NULL COMMENT '受控文档 ID',
    distribute_to VARCHAR(255) NOT NULL COMMENT '分发对象',
    distribute_qty INT NOT NULL DEFAULT 1 COMMENT '分发份数',
    distribute_date DATE NOT NULL COMMENT '分发日期',
    received_by VARCHAR(64) COMMENT '签收人',
    received_date DATE COMMENT '签收日期',
    returned_qty INT DEFAULT 0 COMMENT '回收份数',
    returned_date DATE COMMENT '回收日期',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_document_id (document_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 文档分发记录表';

-- ----------------------------
-- 文件变更申请表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_document_change_request (
    id BIGINT PRIMARY KEY COMMENT '主键',
    document_id BIGINT NOT NULL COMMENT '受控文档 ID',
    change_type TINYINT NOT NULL COMMENT '变更类型（10 新增 20 修订 30 作废）',
    change_reason VARCHAR(1000) NOT NULL COMMENT '变更原因',
    change_content TEXT COMMENT '变更内容',
    applicant_id BIGINT COMMENT '申请人 ID',
    apply_date DATE COMMENT '申请日期',
    approver_id BIGINT COMMENT '审批人 ID',
    approve_date DATETIME COMMENT '审批日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待审 20 已审 30 已驳回）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_document_id (document_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 文件变更申请表';

-- ----------------------------
-- 审核计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_audit_plan (
    id BIGINT PRIMARY KEY COMMENT '主键',
    plan_no VARCHAR(64) NOT NULL COMMENT '计划编号',
    audit_type TINYINT NOT NULL COMMENT '审核类型（10 内审 20 外审 30 第二方审核 40 体系审核 50 过程审核 60 产品审核）',
    title VARCHAR(255) NOT NULL COMMENT '审核标题',
    audit_standard VARCHAR(255) COMMENT '审核依据（ISO 9001 / IATF 16949 / ISO 14001 等）',
    audit_scope VARCHAR(1000) COMMENT '审核范围',
    audit_purpose VARCHAR(1000) COMMENT '审核目的',
    lead_auditor_id BIGINT COMMENT '主审 ID',
    audit_start_date DATE COMMENT '审核开始日期',
    audit_end_date DATE COMMENT '审核结束日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 已计划 20 已执行 30 已完成 40 已取消）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_plan_no (plan_no),
    KEY idx_audit_type (audit_type),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 审核计划表';

-- ----------------------------
-- 审核组成员表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_audit_plan_auditor (
    id BIGINT PRIMARY KEY COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '审核计划 ID',
    auditor_id BIGINT NOT NULL COMMENT '审核员 ID',
    role TINYINT NOT NULL DEFAULT 20 COMMENT '角色（10 主审 20 组员）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    KEY idx_plan_id (plan_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 审核组成员表';

-- ----------------------------
-- 审核报告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_audit_report (
    id BIGINT PRIMARY KEY COMMENT '主键',
    plan_id BIGINT NOT NULL COMMENT '审核计划 ID',
    report_no VARCHAR(64) NOT NULL COMMENT '报告编号',
    audit_summary TEXT COMMENT '审核总结',
    conclusion TINYINT COMMENT '审核结论（10 符合 20 基本符合 30 不符合）',
    issue_count INT DEFAULT 0 COMMENT '发现的不符合项数',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_report_no (report_no),
    KEY idx_plan_id (plan_id),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 审核报告表';

-- ----------------------------
-- 审核不符合项表
-- ----------------------------
CREATE TABLE IF NOT EXISTS qms_audit_nonconformity (
    id BIGINT PRIMARY KEY COMMENT '主键',
    report_id BIGINT NOT NULL COMMENT '审核报告 ID',
    nc_no VARCHAR(64) NOT NULL COMMENT '不符合项编号',
    severity TINYINT NOT NULL COMMENT '严重程度（10 严重 20 一般 30 观察）',
    description TEXT NOT NULL COMMENT '不符合描述',
    clause VARCHAR(255) COMMENT '不符合条款（如 ISO 9001 8.2.1）',
    responsible_dept_id BIGINT COMMENT '责任部门 ID',
    corrective_action_deadline DATE COMMENT '整改截止日期',
    status TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 待整改 20 整改中 30 已整改 40 已验证 50 已关闭）',
    remark VARCHAR(500) COMMENT '备注',
    sort INT DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID',
    UNIQUE KEY uk_nc_no (nc_no),
    KEY idx_report_id (report_id),
    KEY idx_status (status),
    KEY idx_tenant_id (tenant_id)
) COMMENT='QMS 审核不符合项表';

-- ----------------------------
-- 字典初始化
-- ----------------------------

-- 受控文档类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 受控文档类型', 'qms_doc_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 受控文档类型字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '质量手册',   '10', 'qms_doc_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '程序文件',   '20', 'qms_doc_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '作业指导书', '30', 'qms_doc_type', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '质量记录',   '40', 'qms_doc_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '外来文件',   '50', 'qms_doc_type', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 受控文档状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 受控文档状态', 'qms_doc_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 受控文档状态字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '草稿',   '10', 'qms_doc_status', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '待审',   '20', 'qms_doc_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '已发布', '30', 'qms_doc_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '已作废', '40', 'qms_doc_status', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '已回收', '50', 'qms_doc_status', 0, '',        '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 文件变更类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 文件变更类型', 'qms_doc_change_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 文件变更类型字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '新增', '10', 'qms_doc_change_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '修订', '20', 'qms_doc_change_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '作废', '30', 'qms_doc_change_type', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 变更申请状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 变更申请状态', 'qms_change_request_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 变更申请状态字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '待审',   '10', 'qms_change_request_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '已审',   '20', 'qms_change_request_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '已驳回', '30', 'qms_change_request_status', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 审核类型字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 审核类型', 'qms_audit_type', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 审核类型字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '内审',       '10', 'qms_audit_type', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '外审',       '20', 'qms_audit_type', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '第二方审核', '30', 'qms_audit_type', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '体系审核',   '40', 'qms_audit_type', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '过程审核',   '50', 'qms_audit_type', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(6, '产品审核',   '60', 'qms_audit_type', 0, '',        '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 审核计划状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 审核计划状态', 'qms_audit_plan_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 审核计划状态字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '已计划', '10', 'qms_audit_plan_status', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '已执行', '20', 'qms_audit_plan_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '已完成', '30', 'qms_audit_plan_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '已取消', '40', 'qms_audit_plan_status', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 审核员角色字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 审核员角色', 'qms_auditor_role', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 审核员角色字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '主审', '10', 'qms_auditor_role', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '组员', '20', 'qms_auditor_role', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 审核结论字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 审核结论', 'qms_audit_conclusion', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 审核结论字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '符合',     '10', 'qms_audit_conclusion', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '基本符合', '20', 'qms_audit_conclusion', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '不符合',   '30', 'qms_audit_conclusion', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 不符合项严重程度字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 不符合项严重程度', 'qms_nc_severity', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 不符合项严重程度字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '严重', '10', 'qms_nc_severity', 0, 'danger',  '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '一般', '20', 'qms_nc_severity', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '观察', '30', 'qms_nc_severity', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

-- 不符合项状态字典
INSERT INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, tenant_id, remark)
VALUES ('QMS 不符合项状态', 'qms_nc_status', 0, 'admin', NOW(), 'admin', NOW(), 0, 0, 'QMS 不符合项状态字典');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
(1, '待整改',   '10', 'qms_nc_status', 0, 'info',    '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(2, '整改中',   '20', 'qms_nc_status', 0, 'warning', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(3, '已整改',   '30', 'qms_nc_status', 0, 'primary', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(4, '已验证',   '40', 'qms_nc_status', 0, 'success', '', '', 'admin', NOW(), 'admin', NOW(), 0, 0),
(5, '已关闭',   '50', 'qms_nc_status', 0, '',        '', '', 'admin', NOW(), 'admin', NOW(), 0, 0);

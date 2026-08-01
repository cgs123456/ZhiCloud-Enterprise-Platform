-- ========== 任务1：合同附件 ==========
ALTER TABLE crm_contract ADD COLUMN IF NOT EXISTS file_urls VARCHAR(2048) DEFAULT NULL COMMENT '合同附件 URL 列表';

-- ========== 任务2：开票管理 ==========
CREATE TABLE IF NOT EXISTS crm_invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    no VARCHAR(32) NOT NULL COMMENT '开票单号',
    contract_id BIGINT DEFAULT NULL COMMENT '合同编号',
    customer_id BIGINT DEFAULT NULL COMMENT '客户编号',
    contact_id BIGINT DEFAULT NULL COMMENT '联系人编号',
    invoice_type TINYINT DEFAULT NULL COMMENT '发票类型',
    invoice_no VARCHAR(64) DEFAULT NULL COMMENT '发票号码',
    buyer_name VARCHAR(128) DEFAULT NULL COMMENT '购方名称',
    buyer_tax_no VARCHAR(64) DEFAULT NULL COMMENT '购方税号',
    amount_without_tax DECIMAL(20,2) DEFAULT NULL COMMENT '不含税金额',
    tax_amount DECIMAL(20,2) DEFAULT NULL COMMENT '税额',
    amount_with_tax DECIMAL(20,2) DEFAULT NULL COMMENT '含税金额',
    invoice_date DATE DEFAULT NULL COMMENT '开票日期',
    audit_status TINYINT DEFAULT NULL COMMENT '审批状态',
    process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '工作流编号',
    owner_user_id BIGINT DEFAULT NULL COMMENT '负责人的用户编号',
    file_urls VARCHAR(2048) DEFAULT NULL COMMENT '发票附件 URL 列表',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 开票表';

CREATE TABLE IF NOT EXISTS crm_invoice_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    invoice_id BIGINT NOT NULL COMMENT '发票编号',
    product_name VARCHAR(128) NOT NULL COMMENT '产品名称',
    quantity DECIMAL(20,2) NOT NULL COMMENT '数量',
    unit_price DECIMAL(20,2) NOT NULL COMMENT '单价',
    amount_without_tax DECIMAL(20,2) DEFAULT NULL COMMENT '不含税金额',
    tax_rate DECIMAL(10,2) DEFAULT NULL COMMENT '税率',
    tax_amount DECIMAL(20,2) DEFAULT NULL COMMENT '税额',
    amount_with_tax DECIMAL(20,2) DEFAULT NULL COMMENT '含税金额',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 开票明细表';

-- ========== 任务3：线索池 ==========
CREATE TABLE IF NOT EXISTS crm_clue_pool_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    enabled TINYINT(1) NOT NULL COMMENT '是否启用线索公海',
    contact_expire_days INT DEFAULT NULL COMMENT '未跟进放入公海天数',
    notify_enabled TINYINT(1) DEFAULT NULL COMMENT '是否开启提前提醒',
    notify_days INT DEFAULT NULL COMMENT '提前提醒天数',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 线索公海配置表';

ALTER TABLE crm_clue ADD COLUMN IF NOT EXISTS receive_count INT DEFAULT 0 COMMENT '领取次数';

-- ========== 任务4：拜访签到 ==========
CREATE TABLE IF NOT EXISTS crm_visit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    customer_id BIGINT DEFAULT NULL COMMENT '客户编号',
    contact_id BIGINT DEFAULT NULL COMMENT '联系人编号',
    owner_user_id BIGINT DEFAULT NULL COMMENT '负责人的用户编号',
    visit_time DATETIME DEFAULT NULL COMMENT '拜访时间',
    sign_in_time DATETIME DEFAULT NULL COMMENT '签到时间',
    sign_in_latitude DECIMAL(10,7) DEFAULT NULL COMMENT '签到纬度',
    sign_in_longitude DECIMAL(10,7) DEFAULT NULL COMMENT '签到经度',
    sign_in_address VARCHAR(255) DEFAULT NULL COMMENT '签到地址',
    visit_type TINYINT DEFAULT NULL COMMENT '拜访类型',
    content VARCHAR(1024) DEFAULT NULL COMMENT '拜访内容',
    pic_urls VARCHAR(2048) DEFAULT NULL COMMENT '图片 URL 列表',
    file_urls VARCHAR(2048) DEFAULT NULL COMMENT '附件 URL 列表',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 拜访签到记录表';

-- ========== 任务5：客户标签 ==========
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS tag_ids VARCHAR(255) DEFAULT NULL COMMENT '客户标签 ID 列表';

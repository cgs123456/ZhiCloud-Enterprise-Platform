-- ========== 任务1：销售订单 ==========
CREATE TABLE IF NOT EXISTS crm_sale_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    no VARCHAR(32) NOT NULL COMMENT '订单编号',
    contract_id BIGINT DEFAULT NULL COMMENT '关联合同编号',
    customer_id BIGINT DEFAULT NULL COMMENT '客户编号',
    business_id BIGINT DEFAULT NULL COMMENT '商机编号',
    contact_id BIGINT DEFAULT NULL COMMENT '联系人编号',
    order_date DATETIME DEFAULT NULL COMMENT '下单日期',
    delivery_date DATETIME DEFAULT NULL COMMENT '交货日期',
    total_amount DECIMAL(20,2) DEFAULT NULL COMMENT '总金额',
    discount_amount DECIMAL(20,2) DEFAULT NULL COMMENT '折扣金额',
    final_amount DECIMAL(20,2) DEFAULT NULL COMMENT '最终金额',
    payment_status TINYINT DEFAULT 10 COMMENT '付款状态：10未付款 20部分付款 30已付款',
    delivery_status TINYINT DEFAULT 10 COMMENT '发货状态：10未发货 20部分发货 30已发货 40已签收',
    status TINYINT DEFAULT 10 COMMENT '订单状态：10草稿 20已确认 30已发货 40已完成 50已取消',
    owner_user_id BIGINT DEFAULT NULL COMMENT '负责人的用户编号',
    process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '工作流编号',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 销售订单表';

CREATE TABLE IF NOT EXISTS crm_sale_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    order_id BIGINT NOT NULL COMMENT '订单编号',
    product_id BIGINT DEFAULT NULL COMMENT '产品编号',
    product_name VARCHAR(128) DEFAULT NULL COMMENT '产品名称',
    quantity DECIMAL(20,2) NOT NULL COMMENT '数量',
    unit_price DECIMAL(20,2) NOT NULL COMMENT '单价',
    discount DECIMAL(20,2) DEFAULT NULL COMMENT '折扣',
    amount DECIMAL(20,2) DEFAULT NULL COMMENT '金额',
    tax_rate DECIMAL(10,4) DEFAULT NULL COMMENT '税率',
    tax_amount DECIMAL(20,2) DEFAULT NULL COMMENT '税额',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 销售订单明细表';

-- ========== 任务2：多渠道线索归集 ==========
CREATE TABLE IF NOT EXISTS crm_clue_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    channel_name VARCHAR(128) NOT NULL COMMENT '渠道名称',
    channel_type TINYINT DEFAULT NULL COMMENT '渠道类型：10官网 20广告 30企微 40小程序 50API 60线下',
    channel_code VARCHAR(64) NOT NULL COMMENT '渠道编码',
    api_url VARCHAR(512) DEFAULT NULL COMMENT 'API 地址',
    api_key VARCHAR(255) DEFAULT NULL COMMENT 'API 密钥',
    auto_assign_user_id BIGINT DEFAULT NULL COMMENT '自动分配人',
    status TINYINT DEFAULT 10 COMMENT '状态：10启用 20停用',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 线索渠道表';

-- ========== 任务3：合同电子签 ==========
ALTER TABLE crm_contract ADD COLUMN IF NOT EXISTS esign_task_id VARCHAR(64) DEFAULT NULL COMMENT '电子签任务ID';

-- ========== 任务4：售后工单 ==========
CREATE TABLE IF NOT EXISTS crm_work_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    no VARCHAR(32) NOT NULL COMMENT '工单编号',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    customer_id BIGINT DEFAULT NULL COMMENT '客户编号',
    contact_id BIGINT DEFAULT NULL COMMENT '联系人编号',
    product_id BIGINT DEFAULT NULL COMMENT '产品编号',
    work_order_type TINYINT DEFAULT NULL COMMENT '工单类型：10安装 20维修 30投诉 40咨询 50退换货',
    priority TINYINT DEFAULT NULL COMMENT '优先级：10低 20中 30高 40紧急',
    description TEXT DEFAULT NULL COMMENT '问题描述',
    status TINYINT DEFAULT 10 COMMENT '状态：10待分配 20已分配 30处理中 40已解决 50已关闭',
    assignee_user_id BIGINT DEFAULT NULL COMMENT '处理人',
    resolution TEXT DEFAULT NULL COMMENT '解决方案',
    respond_time DATETIME DEFAULT NULL COMMENT '响应时间',
    resolve_time DATETIME DEFAULT NULL COMMENT '解决时间',
    sla_deadline DATETIME DEFAULT NULL COMMENT 'SLA 截止时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户编号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 售后工单表';

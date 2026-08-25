-- ============================================================
-- V34: QMS 客户投诉管理
--
-- 覆盖投诉登记、调查、处理措施，并支持关联 8D 报告。
-- ============================================================

CREATE TABLE IF NOT EXISTS `qms_customer_complaint` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `complaint_no` VARCHAR(64) NOT NULL COMMENT '投诉编号',
    `customer_id` BIGINT DEFAULT NULL COMMENT '客户 ID',
    `customer_name` VARCHAR(255) DEFAULT NULL COMMENT '客户名称',
    `product_id` BIGINT DEFAULT NULL COMMENT '产品 ID',
    `product_name` VARCHAR(255) DEFAULT NULL COMMENT '产品名称',
    `complaint_content` TEXT COMMENT '投诉内容',
    `complaint_date` DATE DEFAULT NULL COMMENT '投诉日期',
    `root_cause` TEXT COMMENT '调查根因',
    `impact_scope` VARCHAR(1000) DEFAULT NULL COMMENT '影响范围',
    `handle_type` TINYINT DEFAULT NULL COMMENT '处理方式（10 退货 20 换货 30 赔偿 40 纠正）',
    `handle_action` TEXT COMMENT '处理措施描述',
    `eight_d_id` BIGINT DEFAULT NULL COMMENT '关联 8D 报告 ID',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 已登记 20 调查中 30 处理中 40 已关闭）',
    `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_complaint_no` (`complaint_no`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`),
    KEY `idx_eight_d_id` (`eight_d_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 客户投诉表';
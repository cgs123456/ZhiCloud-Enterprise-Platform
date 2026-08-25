-- =====================================================
-- P0-2 客户信用控制字段（erp_customer）
-- =====================================================
ALTER TABLE erp_customer ADD COLUMN credit_limit DECIMAL(20,4) NULL DEFAULT 0 COMMENT '信用额度';
ALTER TABLE erp_customer ADD COLUMN used_credit DECIMAL(20,4) NULL DEFAULT 0 COMMENT '已用信用额度（冗余，审核销售订单时锁定，反审核时释放）';
ALTER TABLE erp_customer ADD COLUMN credit_period INT NULL DEFAULT 0 COMMENT '信用期（天）';
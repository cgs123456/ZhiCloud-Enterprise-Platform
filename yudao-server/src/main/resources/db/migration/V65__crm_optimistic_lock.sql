-- CRM 乐观锁版本号字段
-- 为 CRM 5 个核心实体添加 version 字段，配合 MyBatis-Plus OptimisticLockerInnerInterceptor 使用
-- 幂等语法：使用 IF NOT EXISTS 避免重复执行报错

-- 客户
ALTER TABLE crm_customer ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_customer SET version = 0 WHERE version IS NULL;

-- 商机
ALTER TABLE crm_business ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_business SET version = 0 WHERE version IS NULL;

-- 联系人
ALTER TABLE crm_contact ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_contact SET version = 0 WHERE version IS NULL;

-- 合同
ALTER TABLE crm_contract ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_contract SET version = 0 WHERE version IS NULL;

-- 线索
ALTER TABLE crm_clue ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_clue SET version = 0 WHERE version IS NULL;

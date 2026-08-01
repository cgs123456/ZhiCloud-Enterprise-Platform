-- CRM 回款乐观锁版本号字段
-- 补全 CrmReceivableDO 的 version 字段，对齐 CRM 其他核心实体（customer/business/contact/contract/clue）的乐观锁机制
-- 幂等语法：使用 IF NOT EXISTS 避免重复执行报错

-- 回款
ALTER TABLE crm_receivable ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_receivable SET version = 0 WHERE version IS NULL;

-- 回款计划（同模块补全）
ALTER TABLE crm_receivable_plan ADD COLUMN IF NOT EXISTS version INT DEFAULT 0 COMMENT '乐观锁版本号';
UPDATE crm_receivable_plan SET version = 0 WHERE version IS NULL;

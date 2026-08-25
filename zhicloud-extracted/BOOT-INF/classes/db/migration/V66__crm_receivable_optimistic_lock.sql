-- CRM 回款乐观锁版本号字段
-- 补全 CrmReceivableDO 的 version 字段，对齐 CRM 其他核心实体（customer/business/contact/contract/clue）的乐观锁机制
-- 幂等语法：使用 IF NOT EXISTS 避免重复执行报错

-- 回款
-- 幂等新增列：crm_receivable.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_receivable' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_receivable` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_receivable SET version = 0 WHERE version IS NULL;

-- 回款计划（同模块补全）
-- 幂等新增列：crm_receivable_plan.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_receivable_plan' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_receivable_plan` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_receivable_plan SET version = 0 WHERE version IS NULL;

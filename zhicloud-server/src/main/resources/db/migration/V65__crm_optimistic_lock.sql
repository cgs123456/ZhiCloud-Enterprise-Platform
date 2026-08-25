-- CRM 乐观锁版本号字段
-- 为 CRM 5 个核心实体添加 version 字段，配合 MyBatis-Plus OptimisticLockerInnerInterceptor 使用
-- 幂等语法：使用 IF NOT EXISTS 避免重复执行报错

-- 客户
-- 幂等新增列：crm_customer.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_customer' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_customer` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_customer SET version = 0 WHERE version IS NULL;

-- 商机
-- 幂等新增列：crm_business.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_business' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_business` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_business SET version = 0 WHERE version IS NULL;

-- 联系人
-- 幂等新增列：crm_contact.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contact' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_contact` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_contact SET version = 0 WHERE version IS NULL;

-- 合同
-- 幂等新增列：crm_contract.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_contract' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_contract` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_contract SET version = 0 WHERE version IS NULL;

-- 线索
-- 幂等新增列：crm_clue.version
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_clue' AND COLUMN_NAME = 'version'),
                  'DO 0',
                  'ALTER TABLE `crm_clue` ADD COLUMN `version` INT DEFAULT 0 COMMENT ''乐观锁版本号''');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;
UPDATE crm_clue SET version = 0 WHERE version IS NULL;

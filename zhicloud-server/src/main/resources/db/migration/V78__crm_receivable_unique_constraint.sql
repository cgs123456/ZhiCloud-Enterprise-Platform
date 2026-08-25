-- CRM 回款唯一约束（防止并发超收）
-- 为 crm_receivable 添加 (contract_id, no) 唯一索引，确保同一合同不会有两笔相同编号的回款

-- 幂等新增唯一索引
SET @zm_sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'crm_receivable' 
                         AND INDEX_NAME = 'uk_contract_no'),
                  'DO 0',
                  'ALTER TABLE `crm_receivable` ADD UNIQUE INDEX `uk_contract_no` (`contract_id`, `no`)');
PREPARE zm_stmt FROM @zm_sql;
EXECUTE zm_stmt;
DEALLOCATE PREPARE zm_stmt;

-- 同步更新 zhicloud_platform.sql 中的表定义

-- ============================================================
-- V84: HR 薪资唯一索引（薪资生成幂等兜底；依赖 V83 创建的 hr_salary 表）
--
-- 为 hr_salary 添加 (employee_id, salary_month) 唯一索引，
-- 确保同一员工同一月份只有一条薪资记录，防止并发重复生成。
--
-- 注意：若存量数据在 (employee_id, salary_month) 上存在重复，
--       本迁移会失败，需先清理重复数据后再执行。
--
-- 幂等：查 information_schema.statistics 判断索引是否已存在，
--       仅在缺失时执行 ALTER（存量库/全新库均安全）。
-- ============================================================

SET @zm_sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hr_salary'
                         AND INDEX_NAME = 'uk_hr_salary_employee_month'),
                  'DO 0',
                  'ALTER TABLE `hr_salary` ADD UNIQUE INDEX `uk_hr_salary_employee_month` (`employee_id`, `salary_month`)');
PREPARE zm_stmt FROM @zm_sql;
EXECUTE zm_stmt;
DEALLOCATE PREPARE zm_stmt;

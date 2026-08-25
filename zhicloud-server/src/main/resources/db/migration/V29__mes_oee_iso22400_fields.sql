-- OEE ISO 22400-2 完整指标持久化
-- 新增时间稼动率 TUR (Time Utilization Rate) 和机械效率 ME (Mechanical Efficiency) 字段
-- TUR = RunTime / PlannedProductionTime（与 Availability 同义）
-- ME = (IdealCycleTime × GoodProduced) / RunTime（区别于 Performance 使用 GoodProduced）

-- 幂等性检查 + 添加字段
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'mes_dv_oee_record' AND column_name = 'time_utilization_rate');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE mes_dv_oee_record ADD COLUMN time_utilization_rate DECIMAL(10,4) DEFAULT NULL COMMENT ''ISO 22400-2 时间稼动率 TUR (Time Utilization Rate) = RunTime / PlannedProductionTime''',
    'SELECT ''Column time_utilization_rate already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'mes_dv_oee_record' AND column_name = 'mechanical_efficiency');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE mes_dv_oee_record ADD COLUMN mechanical_efficiency DECIMAL(10,4) DEFAULT NULL COMMENT ''ISO 22400-2 机械效率 ME (Mechanical Efficiency) = (IdealCycleTime × GoodProduced) / RunTime''',
    'SELECT ''Column mechanical_efficiency already exists'' AS msg');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

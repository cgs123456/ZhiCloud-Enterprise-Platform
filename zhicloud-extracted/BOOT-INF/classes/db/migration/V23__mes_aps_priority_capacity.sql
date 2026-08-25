-- ============================================================
-- V23: MES APS 排产算法改造字段（P0-12）
--
-- 为 mes_pro_work_order 添加：
--   priority   INT  工单优先级（1=高/2=中/3=低），APS 排产排序用，默认 2（中）
--
-- 为 mes_md_workstation 添加：
--   capacity    DECIMAL(20,4) 单位时间产能（件/小时）
--   efficiency  DECIMAL(10,4) 效率系数（默认 1.0）
--
-- 设计说明：
--   1) priority 用于 APS 排产时按"高→中→低 + 需求日期"排序，替代旧的"统一中优先级"简化逻辑
--   2) capacity + efficiency 用于精确估算工序时长：
--      durationHours = ceil(quantity * unitHours / capacity / efficiency)
--      unitHours 由 mes_pro_route_product.productionTime 按时间单位换算为小时
--      若 capacity/efficiency 为空，回退到旧的"1 件 = 1 小时"简化估算
-- ============================================================

DROP PROCEDURE IF EXISTS p_mes_aps_add_columns;
DELIMITER $$
CREATE PROCEDURE p_mes_aps_add_columns(IN tableName VARCHAR(128), IN columnName VARCHAR(64),
                                        IN columnDef TEXT, IN columnComment VARCHAR(255))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = tableName)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND column_name = columnName) THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDef,
                         ' COMMENT ''', columnComment, '''');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- mes_pro_work_order 新增 priority
CALL p_mes_aps_add_columns('mes_pro_work_order', 'priority', 'INT NULL DEFAULT 2', '工单优先级（APS 排产排序用）：1=高/2=中/3=低');

-- mes_md_workstation 新增 capacity / efficiency
CALL p_mes_aps_add_columns('mes_md_workstation', 'capacity', 'DECIMAL(20,4) NULL DEFAULT 1.0000', '单位时间产能（件/小时，APS 排产时长估算用）');
CALL p_mes_aps_add_columns('mes_md_workstation', 'efficiency', 'DECIMAL(10,4) NULL DEFAULT 1.0000', '效率系数（1.0=标准，APS 排产时长按此系数折算）');

DROP PROCEDURE IF EXISTS p_mes_aps_add_columns;

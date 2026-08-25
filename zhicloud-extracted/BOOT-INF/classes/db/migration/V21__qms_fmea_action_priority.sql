-- ============================================================
-- V21: QMS FMEA 条目表补充 AIAG-VDA 行动优先级字段（P0-10）
--
-- 为 qms_fmea_item 表添加 action_priority 列：
--   action_priority VARCHAR(16) 行动优先级 HIGH/MEDIUM/LOW
--
-- 数据迁移：根据已有 severity/occurrence/detection 三列回填 action_priority
-- ============================================================

-- 1. 添加列
DROP PROCEDURE IF EXISTS p_qms_fmea_add_action_priority;
DELIMITER $$
CREATE PROCEDURE p_qms_fmea_add_action_priority()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = 'qms_fmea_item') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = 'qms_fmea_item'
                         AND column_name = 'action_priority') THEN
            ALTER TABLE `qms_fmea_item`
                ADD COLUMN `action_priority` VARCHAR(16) NULL COMMENT '行动优先级（AIAG-VDA 2019）：HIGH/MEDIUM/LOW';
        END IF;
    END IF;
END$$
DELIMITER ;
CALL p_qms_fmea_add_action_priority();
DROP PROCEDURE IF EXISTS p_qms_fmea_add_action_priority;

-- 2. 历史数据回填（按 S/O/D 组合查表，与 FmeaActionPriorityCalculator 一致）
--    S 高（9-10）：O >= 4 → H；O 2-3 且 D<=3 → H；O 2-3 且 D>=4 → M；O 1 → M
--    S 中高（7-8）：O >= 6 → H；O 2-5 → M；O 1 → L
--    S 中低（4-6）：O >= 8 → H；O 4-7 → M；O 1-3 → L
--    S 低（1-3）：O >= 6 → M；O 1-5 → L
UPDATE `qms_fmea_item` SET `action_priority` = CASE
    WHEN severity >= 9 AND occurrence >= 4 THEN 'HIGH'
    WHEN severity >= 9 AND occurrence >= 2 AND detection <= 3 THEN 'HIGH'
    WHEN severity >= 9 AND occurrence >= 2 THEN 'MEDIUM'
    WHEN severity >= 9 THEN 'MEDIUM'
    WHEN severity >= 7 AND occurrence >= 6 THEN 'HIGH'
    WHEN severity >= 7 AND occurrence >= 2 THEN 'MEDIUM'
    WHEN severity >= 7 THEN 'LOW'
    WHEN severity >= 4 AND occurrence >= 8 THEN 'HIGH'
    WHEN severity >= 4 AND occurrence >= 4 THEN 'MEDIUM'
    WHEN severity >= 4 THEN 'LOW'
    WHEN severity >= 1 AND occurrence >= 6 THEN 'MEDIUM'
    ELSE 'LOW'
END
WHERE `action_priority` IS NULL
  AND severity IS NOT NULL
  AND occurrence IS NOT NULL
  AND detection IS NOT NULL;

-- V180: HRM 员工自助端（portal）——考核申诉附件列 + 菜单权限种子
-- ============================================================
-- 一、加列：hr_performance_assessment.appeal_file_urls（申诉附件地址数组 → json）
--     来源：api/hrm/portal/performance/assessment submit-appeal 的 appealFileUrls 契约。
--     纯加性（不改名不删列），与 V161 同口径。
-- 二、菜单/权限（挂 9100 /hrm 目录下，id 9251-9260 段）：
--     来源：views/hrm/portal/** 与后端 @PreAuthorize 交集。portal 三权限：
--       hrm:portal:employee:update（档案自助编辑）、hrm:portal:attendance:leave（请假申请/撤销）、
--       hrm:portal:performance:action（考核填写/评分/确认/申诉等动作）。
--     portal 页面为员工自助入口（首页聚合，非管理菜单），三权限以按钮形式挂 /hrm 便于授权。
-- 幂等：加列前查 information_schema；菜单按 (permission,type) 预检。
-- MySQL 8.4。

SET @col_exists = (SELECT COUNT(*) FROM `information_schema`.`columns`
                   WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_performance_assessment'
                     AND `column_name` = 'appeal_file_urls');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `hr_performance_assessment` ADD COLUMN `appeal_file_urls` json DEFAULT NULL COMMENT ''申诉附件地址列表'' AFTER `appeal_reason`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO `system_menu` (`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
SELECT t.* FROM (
  SELECT 9251 AS `id`, '档案自助编辑' AS `name`, 'hrm:portal:employee:update' AS `permission`, 3 AS `type`, 1 AS `sort`, 9100 AS `parent_id`, '' AS `path`, '' AS `icon`, '' AS `component`, NULL AS `component_name`, 0 AS `status`, b'1' AS `visible`, b'1' AS `keep_alive`, b'1' AS `always_show`, 'system' AS `creator`, NOW() AS `create_time`, 'system' AS `updater`, NOW() AS `update_time`, b'0' AS `deleted`
  UNION ALL SELECT 9252, '请假申请', 'hrm:portal:attendance:leave', 3, 2, 9100, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
  UNION ALL SELECT 9253, '考核自助处理', 'hrm:portal:performance:action', 3, 3, 9100, '', '', '', NULL, 0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
) t
WHERE NOT EXISTS (
  SELECT 1 FROM `system_menu` m
  WHERE m.`deleted` = b'0'
    AND ( (m.`id` = t.`id`)
       OR (t.`type` = 3 AND t.`permission` <> '' AND m.`permission` = t.`permission` AND m.`type` = 3) )
);

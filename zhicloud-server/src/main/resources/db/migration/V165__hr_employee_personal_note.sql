-- V165: HR/HRM 融合——员工个人备忘表 + field_config 种子去重
-- ============================================================
-- 一、hr_employee_field_config 去重：V163 种子的 kind=1 段 companyAge 字面行重复（id 31/32），
--     且该表 uk (kind,entry_status,name) 对 NULL 不去重，物理约束拦不住。按同键保留最小 id，
--     重复行物理删除（种子数据无引用，可直接删）；幂等：重跑无 count>1 分组即无操作。
--     修复后 V163_ARCHIVE_FIELDS 断言回归 34。
DELETE c FROM `hr_employee_field_config` c
JOIN (
  SELECT `kind`, `entry_status`, `name`, MIN(`id`) AS keep_id
    FROM `hr_employee_field_config`
   WHERE `deleted` = b'0'
   GROUP BY `kind`, `entry_status`, `name`
  HAVING COUNT(*) > 1
) d ON c.`kind` = d.`kind`
   AND c.`name` = d.`name`
   AND ((c.`entry_status` IS NULL AND d.`entry_status` IS NULL) OR c.`entry_status` = d.`entry_status`)
 WHERE c.`id` > d.`keep_id`;

-- 二、员工个人备忘表（前端 home 日历消费；api/hrm/employee/personal-note 只有 content + reminderTime）
-- 备忘按登录用户隔离（user_id），租户表（TenantBaseDO）。
CREATE TABLE IF NOT EXISTS `hr_employee_personal_note` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '备忘编号',
  `user_id` bigint NOT NULL COMMENT '所属后台用户编号（system_users.id，备忘按人隔离）',
  `content` varchar(255) NOT NULL COMMENT '备忘内容',
  `reminder_time` datetime DEFAULT NULL COMMENT '提醒时间',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_emp_note_user` (`user_id`, `reminder_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR 员工个人备忘表';

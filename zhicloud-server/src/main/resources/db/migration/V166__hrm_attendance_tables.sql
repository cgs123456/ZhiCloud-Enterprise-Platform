-- V166: HRM 考勤域建表——打卡/考勤组/节假日/请假/月度汇总（前端 hrm/attendance 契约逐字段生成）
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/attendance/{clock,group,holiday,leave,statistics}
-- 全部租户表（TenantBaseDO）；日期时间统一 datetime（epoch 毫秒序列化）。
-- 设计要点：
--   * hr_attendance_group：班次/地点/WiFi/特殊日期/扣款规则均为嵌套数组 → json 列（契约形状整体存，避免 7 张碎片表）。
--   * hr_attendance_group_employee：员工维度关联表（deptIds 走「适用部门」语义，员工数少时优先员工关联）。
--   * hr_attendance_month_record：月度汇总按 (employee, year, month) 唯一——一键汇总的幂等落点。
--   * hr_attendance_leave：请假单由员工自助发起并走 BPM（process_instance_id 回填），审批状态与 bpm 实例对齐。
--   * hr_attendance_clock：日打卡记录，含应打卡时间（attendance_time）与阶段（stage）。
-- 幂等：CREATE TABLE IF NOT EXISTS。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_attendance_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤组编号',
  `name` varchar(64) NOT NULL COMMENT '考勤组名称',
  `open_wifi_card` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用 WiFi 打卡',
  `open_point_card` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否启用定位打卡',
  `rest` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否法定节假日休息',
  `default_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认考勤组',
  `special_dates` json DEFAULT NULL COMMENT '特殊日期配置数组 [{type,date}]',
  `dept_ids` json DEFAULT NULL COMMENT '适用部门编号数组',
  `employee_ids` json DEFAULT NULL COMMENT '适用员工编号数组',
  `shifts` json DEFAULT NULL COMMENT '班次配置数组 [{weeks,startTime,...}]',
  `points` json DEFAULT NULL COMMENT '打卡地点数组',
  `wifis` json DEFAULT NULL COMMENT '打卡 WiFi 数组',
  `deduct_rule` json DEFAULT NULL COMMENT '扣款规则对象',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_att_group_default` (`default_status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 考勤组表';

CREATE TABLE IF NOT EXISTS `hr_attendance_group_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `group_id` bigint NOT NULL COMMENT '考勤组编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_group_employee` (`group_id`, `employee_id`),
  KEY `idx_hr_att_ge_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 考勤组员工关联表';

CREATE TABLE IF NOT EXISTS `hr_attendance_clock` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '打卡记录编号',
  `employee_id` bigint NOT NULL COMMENT '打卡员工编号',
  `clock_time` datetime DEFAULT NULL COMMENT '打卡时间',
  `type` tinyint NOT NULL COMMENT '打卡类型：1上班 2下班',
  `attendance_time` datetime DEFAULT NULL COMMENT '应打卡日期（当天）',
  `source_type` tinyint DEFAULT NULL COMMENT '打卡来源：1手机端 2手工录入',
  `status` tinyint DEFAULT NULL COMMENT '打卡状态：1正常 2迟到 3早退 4缺卡 5旷工（hrm_attendance_clock_status 字典）',
  `stage` tinyint DEFAULT NULL COMMENT '打卡阶段',
  `address` varchar(255) DEFAULT NULL COMMENT '打卡地址',
  `longitude` decimal(10, 6) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10, 6) DEFAULT NULL COMMENT '纬度',
  `ssid` varchar(64) DEFAULT NULL COMMENT 'WiFi 名称',
  `mac` varchar(64) DEFAULT NULL COMMENT 'WiFi MAC 地址',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_att_clock_emp_time` (`employee_id`, `attendance_time`),
  KEY `idx_hr_att_clock_time` (`clock_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 考勤打卡记录表';

CREATE TABLE IF NOT EXISTS `hr_attendance_holiday` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '节假日编号',
  `date` date NOT NULL COMMENT '日期',
  `type` tinyint NOT NULL COMMENT '日期类型：1上班 2休息',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_holiday_date` (`date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 考勤节假日表';

CREATE TABLE IF NOT EXISTS `hr_attendance_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '请假记录编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `type` varchar(32) NOT NULL COMMENT '请假类型（hrm_attendance_leave_type 字典 value）',
  `start_time` datetime DEFAULT NULL COMMENT '请假开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '请假结束时间',
  `day` decimal(6, 2) NOT NULL DEFAULT 0 COMMENT '请假天数',
  `reason` varchar(255) DEFAULT NULL COMMENT '请假事由',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `approval_status` tinyint DEFAULT NULL COMMENT '审批状态（对齐 BPM 流程实例状态）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT '流程实例编号',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_reason` varchar(255) DEFAULT NULL COMMENT '审批意见',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_att_leave_emp_time` (`employee_id`, `start_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 考勤请假表';

CREATE TABLE IF NOT EXISTS `hr_attendance_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '月度考勤汇总编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `year` int NOT NULL COMMENT '年份',
  `month` int NOT NULL COMMENT '月份',
  `attend_days` decimal(6, 2) NOT NULL DEFAULT 0 COMMENT '应出勤天数',
  `actual_days` decimal(6, 2) NOT NULL DEFAULT 0 COMMENT '实际出勤天数',
  `late_minute` int NOT NULL DEFAULT 0 COMMENT '迟到分钟数',
  `late_count` int NOT NULL DEFAULT 0 COMMENT '迟到次数',
  `early_minute` int NOT NULL DEFAULT 0 COMMENT '早退分钟数',
  `early_count` int NOT NULL DEFAULT 0 COMMENT '早退次数',
  `misscard_count` int NOT NULL DEFAULT 0 COMMENT '缺卡次数',
  `absenteeism_days` decimal(6, 2) NOT NULL DEFAULT 0 COMMENT '旷工天数',
  `absenteeism_minutes` int NOT NULL DEFAULT 0 COMMENT '旷工分钟数',
  `leave_days` decimal(6, 2) NOT NULL DEFAULT 0 COMMENT '请假天数',
  `leave_minutes` int NOT NULL DEFAULT 0 COMMENT '请假分钟数',
  `late_deduct_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '迟到扣款',
  `early_deduct_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '早退扣款',
  `misscard_deduct_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '缺卡扣款',
  `absenteeism_deduct_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '旷工扣款',
  `attendance_deduct_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '考勤扣款合计',
  `full_attendance` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否全勤',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_att_month_emp` (`employee_id`, `year`, `month`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 月度考勤汇总表';

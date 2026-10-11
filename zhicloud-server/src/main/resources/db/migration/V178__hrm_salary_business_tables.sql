-- V178: HRM 薪资域业务建表——员工薪资档案/调薪记录/月度工资表/员工月度工资/工资条模板/发放记录/工资条
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/salary/{employee-info,change-record,month-record,month-record/employee,slip,slip/template,slip/send-record} 契约。
-- 设计（薪资域第二刀；第一刀配置域见 V176）：
--   * hr_salary_employee_info：员工薪资档案（试用期/转正工资 + 两组工资项 json）
--   * hr_salary_employee_change_record：调薪记录（前后总额 + 两组工资项 json，可取消）
--   * hr_salary_month_record：月度工资表头（计薪人数/应发/社保/公积金/个税/实发/公司承担 + 表头工资项 json）
--   * hr_salary_month_employee_record：员工月度工资（出勤天数/应发/应税/个税/实发/绩效系数 + 工资项值 json）
--   * hr_salary_slip_template：工资条模板（hideEmpty + 模板工资项 json）
--   * hr_salary_slip_send_record：工资条发放记录（发放人数/已查看数 + 发放模板项 json）
--   * hr_salary_slip：员工工资条（关联发放记录与月度员工记录，readStatus 阅读状态）
-- 全部租户表（TenantBaseDO）；幂等：CREATE TABLE IF NOT EXISTS。
-- 与旧 /hr 薪资表（hr_salary）并存，旧表待旧页面退役后清理。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_salary_employee_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工薪资信息编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `change_reason` tinyint DEFAULT NULL COMMENT '调薪原因',
  `effect_time` datetime DEFAULT NULL COMMENT '调薪生效时间',
  `change_type` tinyint DEFAULT NULL COMMENT '调薪类型',
  `probation_salary` decimal(12, 2) DEFAULT NULL COMMENT '试用期工资',
  `regular_salary` decimal(12, 2) DEFAULT NULL COMMENT '转正工资',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `salary_options` json DEFAULT NULL COMMENT '转正工资项 [{code,name,value}]',
  `probation_salary_options` json DEFAULT NULL COMMENT '试用期工资项 [{code,name,value}]',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_emp_info_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工薪资档案表';

CREATE TABLE IF NOT EXISTS `hr_salary_employee_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调薪记录编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `record_type` tinyint DEFAULT NULL COMMENT '记录类型',
  `change_reason` tinyint DEFAULT NULL COMMENT '调薪原因',
  `effect_time` datetime DEFAULT NULL COMMENT '生效时间',
  `before_total` decimal(12, 2) DEFAULT NULL COMMENT '调整前转正工资',
  `after_total` decimal(12, 2) DEFAULT NULL COMMENT '调整后转正工资',
  `probation_before_total` decimal(12, 2) DEFAULT NULL COMMENT '调整前试用期工资',
  `probation_after_total` decimal(12, 2) DEFAULT NULL COMMENT '调整后试用期工资',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `salary_options` json DEFAULT NULL COMMENT '转正工资项',
  `probation_salary_options` json DEFAULT NULL COMMENT '试用期工资项',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_change_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工调薪记录表';

CREATE TABLE IF NOT EXISTS `hr_salary_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '月度工资表编号',
  `title` varchar(128) DEFAULT NULL COMMENT '标题',
  `year` int NOT NULL COMMENT '年份',
  `month` tinyint NOT NULL COMMENT '月份',
  `employee_count` int NOT NULL DEFAULT 0 COMMENT '计薪人数',
  `start_time` datetime DEFAULT NULL COMMENT '计薪开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '计薪结束时间',
  `expected_pay_salary` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '应发工资',
  `personal_insurance_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人社保',
  `personal_provident_fund_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人公积金',
  `personal_tax` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人所得税',
  `real_pay_salary` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '实发工资',
  `corporate_insurance_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '公司社保',
  `corporate_provident_fund_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '公司公积金',
  `status` tinyint NOT NULL DEFAULT 5 COMMENT '工资表状态：5未核算 10已归档 11已核算',
  `option_headers` json DEFAULT NULL COMMENT '工资项表头',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_month_year_month` (`year`, `month`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 月度工资表';

CREATE TABLE IF NOT EXISTS `hr_salary_month_employee_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工月度工资记录编号',
  `month_record_id` bigint NOT NULL COMMENT '月度工资表编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `year` int NOT NULL COMMENT '年份',
  `month` tinyint NOT NULL COMMENT '月份',
  `actual_work_day` decimal(6, 2) DEFAULT NULL COMMENT '实际出勤天数',
  `need_work_day` decimal(6, 2) DEFAULT NULL COMMENT '应出勤天数',
  `expected_pay_salary` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '应发工资',
  `taxable_salary` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '应税工资',
  `personal_tax` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人所得税',
  `real_pay_salary` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '实发工资',
  `performance_coefficient` decimal(6, 2) DEFAULT NULL COMMENT '绩效系数',
  `option_values` json DEFAULT NULL COMMENT '工资项值',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_month_emp_record` (`month_record_id`, `employee_id`),
  KEY `idx_hr_sal_month_emp_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工月度工资表';

CREATE TABLE IF NOT EXISTS `hr_salary_slip_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工资条模板编号',
  `name` varchar(64) NOT NULL COMMENT '模板名称',
  `hide_empty` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否隐藏空工资项',
  `default_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认模板',
  `options` json DEFAULT NULL COMMENT '模板工资项 [{code,name,hidden}]',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 工资条模板表';

CREATE TABLE IF NOT EXISTS `hr_salary_slip_send_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工资条发放记录编号',
  `month_record_id` bigint NOT NULL COMMENT '月度工资表编号',
  `employee_count` int NOT NULL DEFAULT 0 COMMENT '工资条数量',
  `send_employee_count` int NOT NULL DEFAULT 0 COMMENT '发放数量',
  `read_count` int NOT NULL DEFAULT 0 COMMENT '已查看数量',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` tinyint DEFAULT NULL COMMENT '月份',
  `options` json DEFAULT NULL COMMENT '本次发放的工资条模板项',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_slip_send_month` (`month_record_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 工资条发放记录表';

CREATE TABLE IF NOT EXISTS `hr_salary_slip` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '工资条编号',
  `send_record_id` bigint NOT NULL COMMENT '工资条发放记录编号',
  `month_employee_record_id` bigint NOT NULL COMMENT '员工月度工资记录编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `year` int DEFAULT NULL COMMENT '年份',
  `month` tinyint DEFAULT NULL COMMENT '月份',
  `read_status` tinyint NOT NULL DEFAULT 0 COMMENT '查看状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_sal_slip_send` (`send_record_id`),
  KEY `idx_hr_sal_slip_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工工资条表';

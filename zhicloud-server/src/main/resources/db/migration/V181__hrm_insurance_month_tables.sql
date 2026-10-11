-- V181: HRM 保险域月度社保表建表（月度社保表头 + 员工月度社保记录）
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/insurance/month-record 与 month-record/employee 契约。
-- 设计（保险域第三刀；前两刀见 V168-170）：
--   * hr_insurance_month_record：月度社保表头（参保/停止参保人数 + 四项金额汇总）
--   * hr_insurance_month_employee_record：员工月度社保记录（社保/公积金两组项目 json + 四项金额 + 参保状态）
-- 全部租户表（TenantBaseDO）；幂等：CREATE TABLE IF NOT EXISTS。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_insurance_month_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '月度社保表编号',
  `title` varchar(128) DEFAULT NULL COMMENT '标题',
  `year` int NOT NULL COMMENT '年份',
  `month` tinyint NOT NULL COMMENT '月份',
  `insured_employee_count` int NOT NULL DEFAULT 0 COMMENT '参保人数',
  `stopped_employee_count` int NOT NULL DEFAULT 0 COMMENT '停止参保人数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `personal_insurance_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '公司公积金金额',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_ins_month_year_month` (`year`, `month`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 月度社保表';

CREATE TABLE IF NOT EXISTS `hr_insurance_month_employee_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工月度社保记录编号',
  `month_record_id` bigint NOT NULL COMMENT '月度社保表编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `scheme_id` bigint DEFAULT NULL COMMENT '社保方案编号',
  `year` int NOT NULL COMMENT '年份',
  `month` tinyint NOT NULL COMMENT '月份',
  `personal_insurance_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人社保金额',
  `personal_provident_fund_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '个人公积金金额',
  `corporate_insurance_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '公司社保金额',
  `corporate_provident_fund_amount` decimal(14, 2) NOT NULL DEFAULT 0 COMMENT '公司公积金金额',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '参保状态：0停止参保 1正常参保',
  `social_security_projects` json DEFAULT NULL COMMENT '社保项目 [{schemeProjectId,type,name,baseAmount,corporateRate,personalRate,corporateAmount,personalAmount}]',
  `provident_fund_projects` json DEFAULT NULL COMMENT '公积金项目',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_ins_month_emp_record` (`month_record_id`, `employee_id`),
  KEY `idx_hr_ins_month_emp_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工月度社保记录表';

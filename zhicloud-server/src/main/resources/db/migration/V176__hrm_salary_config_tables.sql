-- V176: HRM 薪资域配置建表——薪资组/薪资项/计税规则/调薪模板/薪资配置
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/salary/config/{group,option,tax-rule,change-template,config} 契约。
-- 设计（本刀为薪资域第一刀，只做配置；员工薪资/月度/工资条留次刀）：
--   * hr_salary_group：薪资组（适用部门/员工数组 → json；tax_rule_id 关联计税规则）
--   * hr_salary_option：薪资项（树形：parent_code 自引用；系统默认项 system_flag 不可删；计税/显示/计算/启用四开关）
--   * hr_salary_tax_rule：计税规则（类型/起征阈值/小数位/周期）
--   * hr_salary_change_template：调薪模板（options 调薪项数组 → json）
--   * hr_salary_config：薪资配置（单条：计薪周期开始/结束日、社保月份类型、工资开始年月）
-- 全部租户表（TenantBaseDO）；幂等：CREATE TABLE IF NOT EXISTS。
-- 与旧 /hr 薪资表（hr_salary）并存，旧表待旧页面退役后清理。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_salary_group` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '薪资组编号',
  `name` varchar(64) NOT NULL COMMENT '薪资组名称',
  `salary_standard` tinyint DEFAULT NULL COMMENT '月计薪标准',
  `change_rule` varchar(64) DEFAULT NULL COMMENT '转正、调薪月规则',
  `tax_rule_id` bigint DEFAULT NULL COMMENT '计税规则编号',
  `dept_ids` json DEFAULT NULL COMMENT '适用部门编号列表',
  `employee_ids` json DEFAULT NULL COMMENT '适用员工编号列表',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 薪资组表';

CREATE TABLE IF NOT EXISTS `hr_salary_option` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '薪资项编号',
  `code` int NOT NULL COMMENT '薪资项编码',
  `parent_code` int DEFAULT NULL COMMENT '父薪资项编码（0=根）',
  `name` varchar(64) NOT NULL COMMENT '薪资项名称',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `system_flag` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否系统默认项（不可删）',
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '薪资项类型',
  `tax_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否计税',
  `visible` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否显示',
  `calculate_enabled` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否参与计算',
  `enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `template_id` bigint DEFAULT NULL COMMENT '标准薪资项目录编号',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_salary_option_code` (`code`),
  KEY `idx_hr_salary_option_parent` (`parent_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 薪资项表';

CREATE TABLE IF NOT EXISTS `hr_salary_tax_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '计税规则编号',
  `name` varchar(64) NOT NULL COMMENT '计税规则名称',
  `type` tinyint DEFAULT NULL COMMENT '计税类型',
  `tax_enabled` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否计税',
  `threshold` decimal(12, 2) DEFAULT NULL COMMENT '起征阈值',
  `decimal_scale` tinyint DEFAULT NULL COMMENT '小数位数',
  `cycle_type` tinyint DEFAULT NULL COMMENT '计税周期类型',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 计税规则表';

CREATE TABLE IF NOT EXISTS `hr_salary_change_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调薪模板编号',
  `name` varchar(64) NOT NULL COMMENT '模板名称',
  `default_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认模板',
  `options` json DEFAULT NULL COMMENT '调薪项配置 [{code,name}]',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 调薪模板表';

CREATE TABLE IF NOT EXISTS `hr_salary_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置编号',
  `cycle_start_day` tinyint DEFAULT NULL COMMENT '计薪周期开始日',
  `cycle_end_day` tinyint DEFAULT NULL COMMENT '计薪周期结束日',
  `social_security_month_type` tinyint DEFAULT NULL COMMENT '社保对应月份类型',
  `start_year` int DEFAULT NULL COMMENT '工资开始年份',
  `start_month` tinyint DEFAULT NULL COMMENT '工资开始月份',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 薪资配置表';

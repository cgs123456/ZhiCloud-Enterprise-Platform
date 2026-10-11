-- V168: HRM 保险域建表——标准参保/参保方案/员工参保信息
-- ============================================================
-- 来源：zhicloud-ui-admin-vue3/src/api/hrm/insurance/{standard,scheme,employee-info} 契约逐字段生成。
-- 设计要点：
--   * hr_insurance_standard_type/project：按地区预置的标准参保参考数据（种子在 tenant 1，各租户读取走
--     平台级参考口径——标准 uk 不含 tenant，服务层 executeIgnore 读取，与 hrm 字段配置懒惰供给不同）。
--   * hr_insurance_scheme：方案头（area_id 为地区编号，areaName 不入库由 AreaUtils 现算）。
--   * hr_insurance_scheme_project：方案项目行（前端 flat projectList 整组提交，社保/公积金按 type 分组：
--     1-5 社保、10 公积金、6-11 自定义归组见 HrmInsuranceProjectType）。
--   * hr_insurance_employee_info：员工参保信息（一员工一条）。社保/公积金账号镜像 hr_employee 既有两列
--     （V161 social_security_number/accumulation_fund_number，单一真相；save 双写），本表只存参保特有字段。
-- 全部租户表（TenantBaseDO）；幂等：CREATE TABLE IF NOT EXISTS。
-- MySQL 8.4。

CREATE TABLE IF NOT EXISTS `hr_insurance_standard_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参保类型编号',
  `area_id` bigint NOT NULL COMMENT '参保地区编号（system_area.id）',
  `code` varchar(32) NOT NULL COMMENT '参保方案编码（如 URBAN, FLEXIBLE, RURAL）',
  `name` varchar(64) NOT NULL COMMENT '参保方案名称（如 城镇职工）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_ins_std_type_area` (`area_id`, `code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 标准参保类型表';

CREATE TABLE IF NOT EXISTS `hr_insurance_standard_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '参保项目编号',
  `type_id` bigint NOT NULL COMMENT '参保类型编号',
  `type_code` varchar(32) NOT NULL COMMENT '参保类型编码（冗余，便于不关联 type 表查询）',
  `area_id` bigint NOT NULL COMMENT '参保地区编号',
  `type` tinyint NOT NULL COMMENT '项目类型（1养老 2医疗 3失业 4工伤 5生育 6补充大病 7补充养老 8残保金 9社保自定义 10公积金 11公积金自定义，hrm_insurance_project_type 字典）',
  `name` varchar(64) NOT NULL COMMENT '项目名称（如 养老保险）',
  `base_amount` decimal(12, 2) DEFAULT NULL COMMENT '缴纳基数（元）',
  `corporate_rate` decimal(6, 4) DEFAULT NULL COMMENT '公司缴纳比例（小数）',
  `personal_rate` decimal(6, 4) DEFAULT NULL COMMENT '个人缴纳比例（小数）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_ins_std_proj_type` (`type_id`),
  KEY `idx_hr_ins_std_proj_area_type` (`area_id`, `type_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 标准参保项目表';

CREATE TABLE IF NOT EXISTS `hr_insurance_scheme` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '社保方案编号',
  `name` varchar(64) NOT NULL COMMENT '方案名称',
  `area_id` bigint DEFAULT NULL COMMENT '参保地区编号（system_area.id，areaName 现算不入库）',
  `household_type` varchar(32) DEFAULT NULL COMMENT '户籍类型（标准参保类型 code）',
  `type` tinyint NOT NULL DEFAULT 1 COMMENT '方案类型：1比例 2固定金额（hrm_insurance_scheme_type 字典）',
  `personal_insurance_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '个人社保金额（汇总）',
  `corporate_insurance_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '公司社保金额（汇总）',
  `personal_provident_fund_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '个人公积金金额（汇总）',
  `corporate_provident_fund_amount` decimal(12, 2) NOT NULL DEFAULT 0 COMMENT '公司公积金金额（汇总）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_ins_scheme_name` (`tenant_id`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 社保方案表';

CREATE TABLE IF NOT EXISTS `hr_insurance_scheme_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '社保方案项目编号',
  `scheme_id` bigint NOT NULL COMMENT '社保方案编号',
  `type` tinyint NOT NULL COMMENT '项目类型（1养老 2医疗 3失业 4工伤 5生育 6补充大病 7补充养老 8残保金 9社保自定义 10公积金 11公积金自定义）',
  `name` varchar(64) DEFAULT NULL COMMENT '项目名称（自定义项目可改）',
  `base_amount` decimal(12, 2) DEFAULT NULL COMMENT '缴纳基数（元）',
  `corporate_rate` decimal(6, 4) DEFAULT NULL COMMENT '公司缴纳比例（小数）',
  `personal_rate` decimal(6, 4) DEFAULT NULL COMMENT '个人缴纳比例（小数）',
  `corporate_amount` decimal(12, 2) DEFAULT NULL COMMENT '公司缴纳金额（元）',
  `personal_amount` decimal(12, 2) DEFAULT NULL COMMENT '个人缴纳金额（元）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_hr_ins_scheme_project_scheme` (`scheme_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 社保方案项目表';

CREATE TABLE IF NOT EXISTS `hr_insurance_employee_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工参保信息编号',
  `employee_id` bigint NOT NULL COMMENT '员工编号',
  `first_social_security` bit(1) DEFAULT b'0' COMMENT '是否本地首次缴纳社保',
  `first_accumulation_fund` bit(1) DEFAULT b'0' COMMENT '是否本地首次缴纳公积金',
  `social_security_start_month` datetime DEFAULT NULL COMMENT '社保起缴月份（epoch 毫秒口径）',
  `scheme_id` bigint DEFAULT NULL COMMENT '社保方案编号',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hr_ins_emp_info_employee` (`employee_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HRM 员工参保信息表';

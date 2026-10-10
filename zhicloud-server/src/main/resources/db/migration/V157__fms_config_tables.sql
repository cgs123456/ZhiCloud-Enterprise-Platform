-- V157: FMS 财务管理系统 config 域 11 张 fms_* 表
-- 来源: zhicloud-ui-admin-vue3/src/api/fms/config 前端契约 + zhicloud-module-fms DO 字段（同一 spec 生成）。
-- 口径: 全部为租户表（TenantBaseDO：creator/create_time/updater/update_time/deleted/tenant_id）；
--       金额/汇率 decimal(24,6)；数组/对象字段用 json 列。
-- 本轮为 config 域首刀：账套/成员/参数 + 8 类主数据。subject（科目树）与 initial-balance（初始余额）
-- 含真实业务逻辑，留待下一增量；本迁移不建它们的表。
-- 幂等: CREATE TABLE IF NOT EXISTS。

CREATE TABLE IF NOT EXISTS `fms_account_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `company_code` varchar(32) NOT NULL COMMENT '公司编码',
  `company_name` varchar(64) NOT NULL COMMENT '公司名称',
  `company_profile` varchar(512) DEFAULT NULL COMMENT '公司简介',
  `industry` varchar(32) DEFAULT NULL COMMENT '所在行业',
  `location` varchar(32) DEFAULT NULL COMMENT '所在地',
  `legal_representative` varchar(32) DEFAULT NULL COMMENT '法人代表',
  `legal_representative_id_number` varchar(32) DEFAULT NULL COMMENT '法人身份证号',
  `business_license_number` varchar(32) DEFAULT NULL COMMENT '营业执照号',
  `organization_code` varchar(32) DEFAULT NULL COMMENT '组织机构代码',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `contact_name` varchar(32) DEFAULT NULL COMMENT '联系人',
  `office_telephone` varchar(32) DEFAULT NULL COMMENT '办公电话',
  `mobile` varchar(16) DEFAULT NULL COMMENT '手机号码',
  `fax_number` varchar(32) DEFAULT NULL COMMENT '传真号码',
  `qq_number` varchar(32) DEFAULT NULL COMMENT 'QQ 号码',
  `email` varchar(64) DEFAULT NULL COMMENT '邮箱',
  `other_contact` varchar(64) DEFAULT NULL COMMENT '其他联系方式',
  `address` varchar(128) DEFAULT NULL COMMENT '详细地址',
  `currency_id` bigint DEFAULT NULL COMMENT '本位币编号',
  `start_time` int DEFAULT NULL COMMENT '启用期间 yyyyMM',
  `standard` int DEFAULT NULL COMMENT '会计制度',
  `initialized` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否已初始化',
  `default_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认账套',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_account_set` (`company_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 账套表';

CREATE TABLE IF NOT EXISTS `fms_account_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `user_id` bigint NOT NULL COMMENT '后台用户编号',
  `level` int NOT NULL DEFAULT 0 COMMENT '成员权限级别：1=主管 2=查看者 3=会计',
  `default_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认账套',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_account_user` (`account_set_id`, `user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 账套成员表';

CREATE TABLE IF NOT EXISTS `fms_finance_parameter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `standard` int DEFAULT NULL COMMENT '会计制度',
  `level` int NOT NULL DEFAULT 4 COMMENT '科目层级',
  `subject_code_rule` varchar(32) NOT NULL DEFAULT '4-2-2-2' COMMENT '科目编码规则',
  `ledger_balance_mode` int NOT NULL DEFAULT 0 COMMENT '账簿余额方向模式',
  `voucher_review_required` bit(1) NOT NULL DEFAULT b'1' COMMENT '结账前是否要求凭证审核',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_finance_parameter` (`account_set_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 账套参数表';

CREATE TABLE IF NOT EXISTS `fms_currency` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `code` varchar(16) NOT NULL COMMENT '币别编码',
  `name` varchar(32) NOT NULL COMMENT '币别名称',
  `exchange_rate` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '汇率',
  `standard` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否本位币',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_currency` (`account_set_id`, `code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 币别表';

CREATE TABLE IF NOT EXISTS `fms_voucher_word` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `name` varchar(8) NOT NULL COMMENT '凭证字',
  `print_title` varchar(32) DEFAULT NULL COMMENT '打印标题',
  `default_status` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认凭证字',
  `sort` int NOT NULL DEFAULT 0 COMMENT '显示顺序',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_voucher_word` (`account_set_id`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 凭证字表';

CREATE TABLE IF NOT EXISTS `fms_digest` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `content` varchar(255) NOT NULL COMMENT '摘要内容',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 常用摘要表';

CREATE TABLE IF NOT EXISTS `fms_auxiliary_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `name` varchar(32) NOT NULL COMMENT '类别名称',
  `type` int DEFAULT NULL COMMENT '辅助核算类型',
  `system_preset` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否系统预置',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_auxiliary_type` (`account_set_id`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 辅助核算类别表';

CREATE TABLE IF NOT EXISTS `fms_auxiliary_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `auxiliary_type_id` bigint NOT NULL COMMENT '辅助核算类别编号',
  `code` varchar(32) NOT NULL COMMENT '项目编码',
  `name` varchar(64) NOT NULL COMMENT '项目名称',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `specification` varchar(64) DEFAULT NULL COMMENT '规格',
  `unit` varchar(16) DEFAULT NULL COMMENT '单位',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_auxiliary_item` (`account_set_id`, `auxiliary_type_id`, `code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 辅助核算项目表';

CREATE TABLE IF NOT EXISTS `fms_voucher_template_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `name` varchar(32) NOT NULL COMMENT '分类名称',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_voucher_template_category` (`account_set_id`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 凭证模板分类表';

CREATE TABLE IF NOT EXISTS `fms_voucher_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `name` varchar(64) NOT NULL COMMENT '模板名称',
  `category_id` bigint NOT NULL COMMENT '分类编号',
  `entries` json DEFAULT NULL COMMENT '凭证模板分录数组',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_voucher_template` (`account_set_id`, `name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 凭证模板表';

CREATE TABLE IF NOT EXISTS `fms_finance_indicator` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `account_set_id` bigint NOT NULL COMMENT '账套编号',
  `name` varchar(64) NOT NULL COMMENT '指标名称',
  `code` varchar(32) NOT NULL COMMENT '指标编码',
  `type` int NOT NULL COMMENT '取数报表类型',
  `formula` varchar(1024) NOT NULL COMMENT '指标公式',
  `sort` int NOT NULL DEFAULT 0 COMMENT '展示顺序',
  `status` int NOT NULL DEFAULT 0 COMMENT '状态',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fms_finance_indicator` (`account_set_id`, `code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'FMS 财务指标表';

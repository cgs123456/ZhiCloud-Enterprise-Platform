-- ============================================================
-- V51.1: CRM 模块核心表补齐
--
-- 问题：zhicloud-module-crm 有 29 个 DO，但生产 schema 里只有 8 张 CRM 表
--       （由 V52 / V63 创建）。crm_customer / crm_clue / crm_contact /
--       crm_business / crm_contract / crm_receivable / crm_receivable_plan
--       等 21 张核心表从未被 zhicloud_platform.sql 或任何迁移创建，
--       仅存在于 H2 测试 schema 中。
--
-- 直接后果：V52 / V63 / V65 / V66 / V78 这批"给 CRM 表打补丁"的迁移
--       全部因 ERROR 1146 (Table doesn't exist) 失败，迁移链断裂。
--
-- 修复：在 V52 之前补齐这 21 张表（字段清单取自 CRM DO / H2 测试 schema）。
--       版本号沿用本项目既有的小版本约定（参考 V60.1 / V61.1 / V62.1）。
--
-- 说明：
--   1) 已由 V52 / V63 创建的 8 张表（crm_invoice / crm_invoice_line /
--      crm_clue_pool_config / crm_visit_record / crm_clue_channel /
--      crm_sale_order / crm_sale_order_item / crm_work_order）此处不重复定义，
--      避免先建出字段较少的版本导致后续 CREATE TABLE IF NOT EXISTS 静默跳过。
--   2) 统一补 tenant_id：TenantDatabaseInterceptor 对未标注 @TenantIgnore
--      的表会自动拼接 tenant_id 条件，缺列会在运行时 ERROR 1054。
--   3) crm_receivable 的 uk_contract_no 唯一索引仍由 V78 负责，保持职责单一。
--   4) 全部使用 CREATE TABLE IF NOT EXISTS，存量库可安全执行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `crm_business` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `customer_id` bigint,
    `follow_up_status` bit(1),
    `contact_last_time` datetime,
    `contact_next_time` datetime,
    `owner_user_id` bigint,
    `status_type_id` bigint,
    `status_id` bigint,
    `end_status` int,
    `end_remark` varchar(255),
    `deal_time` datetime,
    `total_product_price` decimal(20,2),
    `discount_percent` decimal(20,2),
    `total_price` decimal(20,2),
    `remark` varchar(255),
    `version` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_business_product` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `business_id` bigint,
    `product_id` bigint,
    `product_price` decimal(20,2),
    `business_price` decimal(20,2),
    `count` decimal(20,2),
    `total_price` decimal(20,2),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_business_status` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `type_id` bigint,
    `name` varchar(255),
    `percent` int,
    `sort` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_business_status_type` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `dept_ids` varchar(1024),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_clue` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `follow_up_status` bit(1),
    `contact_last_time` datetime,
    `contact_last_content` varchar(255),
    `contact_next_time` datetime,
    `owner_user_id` bigint,
    `receive_count` int,
    `transform_status` bit(1),
    `customer_id` bigint,
    `mobile` varchar(255),
    `telephone` varchar(255),
    `qq` varchar(255),
    `wechat` varchar(255),
    `email` varchar(255),
    `area_id` int,
    `detail_address` varchar(255),
    `industry_id` int,
    `level` int,
    `source` int,
    `remark` varchar(255),
    `version` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_contact` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `customer_id` bigint,
    `contact_last_time` datetime,
    `contact_last_content` varchar(255),
    `contact_next_time` datetime,
    `owner_user_id` bigint,
    `mobile` varchar(255),
    `telephone` varchar(255),
    `email` varchar(255),
    `qq` bigint,
    `wechat` varchar(255),
    `area_id` int,
    `detail_address` varchar(255),
    `sex` int,
    `master` bit(1),
    `post` varchar(255),
    `parent_id` bigint,
    `remark` varchar(255),
    `version` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_contact_business` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `contact_id` bigint,
    `business_id` bigint,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_contract` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `no` varchar(255),
    `customer_id` bigint,
    `business_id` bigint,
    `contact_last_time` datetime,
    `owner_user_id` bigint,
    `process_instance_id` varchar(255),
    `audit_status` int,
    `order_date` datetime,
    `start_time` datetime,
    `end_time` datetime,
    `total_product_price` decimal(20,2),
    `discount_percent` decimal(20,2),
    `total_price` decimal(20,2),
    `sign_contact_id` bigint,
    `sign_user_id` bigint,
    `remark` varchar(255),
    `file_urls` varchar(1024),
    `esign_task_id` varchar(255),
    `version` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_contract_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `notify_enabled` bit(1),
    `notify_days` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_contract_product` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `contract_id` bigint,
    `product_id` bigint,
    `product_price` decimal(20,2),
    `contract_price` decimal(20,2),
    `count` decimal(20,2),
    `total_price` decimal(20,2),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_customer` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `follow_up_status` bit(1),
    `contact_last_time` datetime,
    `contact_last_content` varchar(255),
    `contact_next_time` datetime,
    `owner_user_id` bigint,
    `owner_time` datetime,
    `lock_status` bit(1),
    `deal_status` bit(1),
    `mobile` varchar(255),
    `telephone` varchar(255),
    `qq` varchar(255),
    `wechat` varchar(255),
    `email` varchar(255),
    `area_id` int,
    `detail_address` varchar(255),
    `industry_id` int,
    `level` int,
    `source` int,
    `remark` varchar(255),
    `tag_ids` varchar(1024),
    `version` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_customer_limit_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `type` int,
    `user_ids` varchar(1024),
    `dept_ids` varchar(1024),
    `max_count` int,
    `deal_count_enabled` bit(1),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_customer_pool_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `enabled` bit(1),
    `contact_expire_days` int,
    `deal_expire_days` int,
    `notify_enabled` bit(1),
    `notify_days` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_follow_up_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `biz_type` int,
    `biz_id` bigint,
    `type` int,
    `content` varchar(255),
    `next_time` datetime,
    `pic_urls` varchar(1024),
    `file_urls` varchar(1024),
    `business_ids` varchar(1024),
    `contact_ids` varchar(1024),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_owner_record` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `biz_type` int,
    `biz_id` bigint,
    `pre_owner_user_id` bigint,
    `post_owner_user_id` bigint,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_performance_config` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `biz_type` int,
    `object_id` bigint,
    `object_type` int,
    `year` int,
    `year_target_price` decimal(20,2),
    `january_target_price` decimal(20,2),
    `february_target_price` decimal(20,2),
    `march_target_price` decimal(20,2),
    `april_target_price` decimal(20,2),
    `may_target_price` decimal(20,2),
    `june_target_price` decimal(20,2),
    `july_target_price` decimal(20,2),
    `august_target_price` decimal(20,2),
    `september_target_price` decimal(20,2),
    `october_target_price` decimal(20,2),
    `november_target_price` decimal(20,2),
    `december_target_price` decimal(20,2),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `biz_type` int,
    `biz_id` bigint,
    `user_id` bigint,
    `level` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_product` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `no` varchar(255),
    `unit` int,
    `price` decimal(20,2),
    `status` int,
    `category_id` bigint,
    `description` varchar(255),
    `owner_user_id` bigint,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_product_category` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `name` varchar(255),
    `parent_id` bigint,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_receivable` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `no` varchar(255),
    `plan_id` bigint,
    `customer_id` bigint,
    `contract_id` bigint,
    `owner_user_id` bigint,
    `return_time` datetime,
    `return_type` int,
    `price` decimal(20,2),
    `remark` varchar(255),
    `process_instance_id` varchar(255),
    `audit_status` int,
    `version` int,
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `crm_receivable_plan` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `period` int,
    `customer_id` bigint,
    `contract_id` bigint,
    `owner_user_id` bigint,
    `return_time` datetime,
    `return_type` int,
    `price` decimal(20,2),
    `receivable_id` bigint,
    `remind_days` int,
    `remind_time` datetime,
    `remark` varchar(255),
    `creator` varchar(64) DEFAULT '',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updater` varchar(64) DEFAULT '',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` bit(1) NOT NULL DEFAULT b'0',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

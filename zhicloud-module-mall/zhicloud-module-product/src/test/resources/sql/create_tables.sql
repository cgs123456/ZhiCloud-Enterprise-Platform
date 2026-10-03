CREATE TABLE IF NOT EXISTS `product_brand` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `name` VARCHAR(128),
  `pic_url` VARCHAR(512),
  `sort` INT,
  `description` VARCHAR(512),
  `status` INT,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `parent_id` BIGINT,
  `name` VARCHAR(128),
  `pic_url` VARCHAR(512),
  `sort` INT,
  `status` INT,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `user_id` BIGINT,
  `user_nickname` VARCHAR(255),
  `user_avatar` VARCHAR(512),
  `anonymous` BOOLEAN,
  `order_id` BIGINT,
  `order_item_id` BIGINT,
  `spu_id` BIGINT,
  `spu_name` VARCHAR(255),
  `sku_id` BIGINT,
  `sku_pic_url` VARCHAR(512),
  `sku_properties` VARCHAR(8192),
  `visible` BOOLEAN,
  `scores` INT,
  `description_scores` INT,
  `benefit_scores` INT,
  `content` VARCHAR(8192),
  `pic_urls` VARCHAR(8192),
  `reply_status` BOOLEAN,
  `reply_user_id` BIGINT,
  `reply_content` VARCHAR(8192),
  `reply_time` TIMESTAMP,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `user_id` BIGINT,
  `spu_id` BIGINT,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_browse_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `spu_id` BIGINT,
  `user_id` BIGINT,
  `user_deleted` BOOLEAN,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_property` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `name` VARCHAR(128),
  `remark` VARCHAR(512),
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_property_value` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `property_id` BIGINT,
  `name` VARCHAR(128),
  `remark` VARCHAR(512),
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_sku` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `spu_id` BIGINT,
  `properties` VARCHAR(8192),
  `price` INT,
  `market_price` INT,
  `cost_price` INT,
  `bar_code` VARCHAR(64),
  `pic_url` VARCHAR(512),
  `stock` INT,
  `weight` DOUBLE,
  `volume` DOUBLE,
  `first_brokerage_price` INT,
  `second_brokerage_price` INT,
  `sales_count` INT,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE IF NOT EXISTS `product_spu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  `name` VARCHAR(128),
  `keyword` VARCHAR(255),
  `introduction` VARCHAR(255),
  `description` VARCHAR(512),
  `category_id` BIGINT,
  `brand_id` BIGINT,
  `pic_url` VARCHAR(512),
  `slider_pic_urls` VARCHAR(8192),
  `sort` INT,
  `status` INT,
  `spec_type` BOOLEAN,
  `price` INT,
  `market_price` INT,
  `cost_price` INT,
  `stock` INT,
  `delivery_types` VARCHAR(8192),
  `delivery_template_id` BIGINT,
  `give_integral` INT,
  `sub_commission_type` BOOLEAN,
  `sales_count` INT,
  `virtual_sales_count` INT,
  `browse_count` INT,
  `creator` VARCHAR(64) DEFAULT '',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) DEFAULT '',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` BOOLEAN NOT NULL DEFAULT FALSE,
  `tenant_id` BIGINT NOT NULL DEFAULT 0
);


-- P1-4 共享库存 Starter：统一库存条目表（单一真值源的承载表）
-- 幂等：通过 information_schema 判表存在，避免 Flyway 重复执行报错

CREATE TABLE IF NOT EXISTS `inventory_item`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `version`       BIGINT       NOT NULL DEFAULT 0                       COMMENT '乐观锁版本号',
    `item_id`       BIGINT       NOT NULL                                 COMMENT '物品编号（统一标识）',
    `warehouse_id`  BIGINT       NOT NULL                                 COMMENT '仓库编号',
    `location_id`   BIGINT       NOT NULL DEFAULT 0                       COMMENT '库位编号（未指定填 0）',
    `area_id`       BIGINT       NOT NULL DEFAULT 0                       COMMENT '库区编号（未指定填 0）',
    `batch_id`      BIGINT       NOT NULL DEFAULT 0                       COMMENT '批次编号（未指定填 0）',
    `batch_code`    VARCHAR(64)  DEFAULT NULL                             COMMENT '批次编码',
    `quantity`      DECIMAL(20, 2) NOT NULL DEFAULT 0                     COMMENT '库存数量',
    `locked_count`  DECIMAL(20, 2) NOT NULL DEFAULT 0                     COMMENT '锁定数量',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0,
    `creator`       VARCHAR(64)  DEFAULT ''                              COMMENT '创建者',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    `updater`       VARCHAR(64)  DEFAULT ''                              COMMENT '更新者',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_item_warehouse_location_area_batch` (`item_id`, `warehouse_id`, `location_id`, `area_id`, `batch_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '共享库存条目（P1-4 单一真值源承载表）';

-- P3-1 MES 能源消耗记录表
-- 对应 DO: MesEnergyConsumptionDO
CREATE TABLE IF NOT EXISTS `mes_energy_consumption` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '编号',
    `workshop_id`     BIGINT       NOT NULL                COMMENT '车间编号',
    `workstation_id`  BIGINT       DEFAULT NULL            COMMENT '工位编号（空表示车间级统计）',
    `energy_type`     INT          NOT NULL                COMMENT '能源类型（10 电 / 20 水 / 30 天然气 / 40 蒸汽 / 50 压缩空气）',
    `record_date`     DATE         NOT NULL                COMMENT '统计日期',
    `consumption`     DECIMAL(12,3) NOT NULL               COMMENT '消耗量',
    `unit`            VARCHAR(20)  DEFAULT NULL            COMMENT '单位（kWh / 吨 / m³ / ...）',
    `unit_price`      DECIMAL(10,4) DEFAULT NULL           COMMENT '单价',
    `total_amount`    DECIMAL(14,2) DEFAULT NULL           COMMENT '总金额',
    `remark`          VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    `creator`         VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         BIT(1)       NOT NULL DEFAULT b'0'   COMMENT '是否删除',
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0      COMMENT '租户编号',
    PRIMARY KEY (`id`),
    INDEX `idx_workshop_date` (`workshop_id`, `record_date`),
    INDEX `idx_energy_type` (`energy_type`),
    INDEX `idx_workstation` (`workstation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 能源消耗记录表';

CREATE TABLE IF NOT EXISTS `bpm_process_instance_idempotency` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `process_definition_key` varchar(64) NOT NULL COMMENT '流程定义标识',
    `business_key` varchar(128) NOT NULL COMMENT '业务唯一标识',
    `process_instance_id` varchar(255) DEFAULT NULL COMMENT '流程实例编号',
    `claim_token` varchar(64) DEFAULT NULL COMMENT '本次创建 claim 的 ownership token；legacy/blank 行为 NULL',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bpm_process_instance_idempotency` (`tenant_id`, `process_definition_key`, `business_key`),
    KEY `idx_bpm_process_instance_idempotency_instance` (`process_instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'BPM 流程实例幂等登记';

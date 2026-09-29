-- ============================================================
-- V118: BPM 流程实例幂等登记表
--
-- 目标：
--   1. 防止同一业务单据在同一租户内重复创建审批流程；
--   2. process_definition_key + business_key 在租户内永久唯一，软删除仍占用；
--   3. process_instance_id 在流程启动成功后回填，重入请求返回既有实例。
--
-- 边界：
--   * business_key 语义由上游业务负责；CRM/HR/OA 等 API 调用方必须传入稳定主键。
--   * 表只在 MySQL 中创建；其他数据库方言需单独实现。
--   * 创建流程失败时，同事务回滚登记行；已存在的历史重复数据不由本迁移清理。
-- ============================================================

CREATE TABLE IF NOT EXISTS `bpm_process_instance_idempotency` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    `process_definition_key` varchar(64) NOT NULL COMMENT '流程定义标识',
    `business_key` varchar(128) NOT NULL COMMENT '业务唯一标识',
    `process_instance_id` varchar(255) DEFAULT NULL COMMENT '流程实例编号',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bpm_process_instance_idempotency` (`tenant_id`, `process_definition_key`, `business_key`),
    KEY `idx_bpm_process_instance_idempotency_instance` (`process_instance_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'BPM 流程实例幂等登记';

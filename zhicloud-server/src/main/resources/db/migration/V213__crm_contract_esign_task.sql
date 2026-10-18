-- ============================================================
-- V213：CRM 合同电子签任务表（D5，ADR 271；台账 §12.271）
-- ============================================================
-- 依据：§12.221.1「CRM 合同电子签入口」Stub → 真实集成（重建矩阵 D5，Temp/zc_adr_267.md §0
-- / Temp/zc_adr_271.md §2）。本迁移为纯增一表、零存量改动：
--   crm_contract_esign_task（合同电子签任务表）：任务真状态机 10 待签署 / 20 已签署 / 30 已拒签；
--   真值在此表，crm_contract.esign_task_id 仅为「最新任务号」缓存指针；历史 stub 无存量任务、
--   无需回填（登记）。
-- 幂等：CREATE TABLE IF NOT EXISTS（重跑安全）。
-- 存量安全：纯新表，零存量行改动、无键变更。
-- 回滚预案（如需）：删除该表即可（纯新表、无下游外键；合同指针列 crm_contract.esign_task_id 保留）。
-- ============================================================

-- 1. 合同电子签任务表
CREATE TABLE IF NOT EXISTS `crm_contract_esign_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
    `contract_id` bigint NOT NULL COMMENT '合同编号（crm_contract.id）',
    `task_no` varchar(64) NOT NULL COMMENT '电子签任务号（ESIGN+yyyyMMddHHmmssSSS+3 随机位）',
    `status` tinyint NOT NULL DEFAULT 10 COMMENT '签署状态：10 待签署 / 20 已签署 / 30 已拒签',
    `sign_time` datetime DEFAULT NULL COMMENT '签署完成时间（回调 signTime 优先，缺省取回调到达时间）',
    `signed_file_urls` varchar(2048) DEFAULT NULL COMMENT '签署后文件 URL 列表（JSON，回调携带）',
    `remark` varchar(512) DEFAULT NULL COMMENT '备注',
    `creator` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_crm_contract_esign_task_task_no` (`tenant_id`, `task_no`),
    KEY `idx_crm_contract_esign_task_contract` (`tenant_id`, `contract_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 合同电子签任务表（D5：电子签任务真状态机）';

-- 2. 自检 1：任务表在
SELECT CASE WHEN COUNT(*) = 1 THEN 'V213_ESIGN_TASK_TABLE_PASS'
            ELSE CONCAT('V213_ESIGN_TASK_TABLE_FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'crm_contract_esign_task';

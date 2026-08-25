-- ============================================================
-- V32: QMS 8D 报告管理
--
-- 8D（Eight Disciplines）问题解决法，覆盖 D1-D8 八个阶段，
-- 可关联 NCR（不合格品报告）或 CAPA（纠正预防措施）。
-- ============================================================

CREATE TABLE IF NOT EXISTS `qms_eight_d_report` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `report_no` VARCHAR(64) NOT NULL COMMENT '8D 报告编号',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `ncr_id` BIGINT DEFAULT NULL COMMENT '关联 NCR 编号 ID',
    `capa_id` BIGINT DEFAULT NULL COMMENT '关联 CAPA 编号 ID',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 草稿 10 D1团队 20 D2问题 30 D3遏制 40 D4根因 50 D5纠正 60 D6实施 70 D7预防 80 D8关闭）',
    `d1_team_members` VARCHAR(1000) DEFAULT NULL COMMENT 'D1 团队成员',
    `d2_problem_description` TEXT COMMENT 'D2 问题描述',
    `d3_interim_action` TEXT COMMENT 'D3 临时遏制措施',
    `d4_root_cause` TEXT COMMENT 'D4 根本原因分析',
    `d5_permanent_action` TEXT COMMENT 'D5 永久纠正措施',
    `d6_implementation_result` TEXT COMMENT 'D6 实施并验证结果',
    `d7_prevention_action` TEXT COMMENT 'D7 预防再发生措施',
    `d8_team_recognition` VARCHAR(1000) DEFAULT NULL COMMENT 'D8 团队表彰',
    `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_report_no` (`report_no`),
    KEY `idx_status` (`status`),
    KEY `idx_ncr_id` (`ncr_id`),
    KEY `idx_capa_id` (`capa_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 8D 报告表';
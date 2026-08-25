-- P3-2 OA 日程管理表
-- 对应 DO: OaScheduleDO
CREATE TABLE IF NOT EXISTS `oa_schedule` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '编号',
    `user_id`        BIGINT       NOT NULL                COMMENT '用户编号（日程归属人）',
    `title`          VARCHAR(200) NOT NULL                COMMENT '标题',
    `description`    VARCHAR(1000) DEFAULT NULL           COMMENT '描述',
    `type`           INT          DEFAULT 10              COMMENT '日程类型（10 日程 / 20 任务 / 30 纪念日 / 40 会议）',
    `start_time`     DATETIME     NOT NULL                COMMENT '开始时间',
    `end_time`       DATETIME     DEFAULT NULL            COMMENT '结束时间',
    `all_day`        BIT(1)       NOT NULL DEFAULT b'0'   COMMENT '全天事件',
    `location`       VARCHAR(200) DEFAULT NULL            COMMENT '地点',
    `remind_minutes` INT          DEFAULT NULL            COMMENT '提前提醒分钟数',
    `reminded`       BIT(1)       NOT NULL DEFAULT b'0'   COMMENT '是否已提醒',
    `repeat_type`    INT          NOT NULL DEFAULT 0      COMMENT '重复类型（0 不重复 / 10 每天 / 20 每周 / 30 每月 / 40 每年）',
    `status`         INT          NOT NULL DEFAULT 0      COMMENT '状态（0 未完成 / 1 已完成 / 2 已取消）',
    `remark`         VARCHAR(500) DEFAULT NULL            COMMENT '备注',
    `creator`        VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`        VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        BIT(1)       NOT NULL DEFAULT b'0'   COMMENT '是否删除',
    `tenant_id`      BIGINT       NOT NULL DEFAULT 0      COMMENT '租户编号',
    PRIMARY KEY (`id`),
    INDEX `idx_user_start` (`user_id`, `start_time`),
    INDEX `idx_status` (`status`),
    INDEX `idx_reminded` (`reminded`, `start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OA 日程表';

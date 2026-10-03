-- ============================================================
-- V105: 死信人工重放审计
--
-- 背景：
--   V100 已提供 event_outbox / event_inbox，但生产运营需要人工重放死信。
--   仅修改事件状态而没有操作者、原因、时间窗和结果审计，会形成不可追溯的
--   高危运维入口，也无法在事故复盘时判断谁在何时恢复了哪些事件。
--
-- 目标：
--   1. 每一次人工重放请求都写入 event_dlq_replay_log。
--   2. 成功记录与事件状态变更在同一事务提交；失败记录使用独立事务提交，
--      避免主事务回滚后丢失失败痕迹。
--   3. 审计按 tenant_id 隔离，查询和取证不得跨租户读取。
--
-- 兼容性：
--   * CREATE TABLE IF NOT EXISTS，只做加法，可重复执行。
--   * 不修改、不删除 event_outbox / event_inbox 的任何列或历史数据。
--   * 不建立外键：审计记录需要在事件保留策略之外长期留存，且不能因清理
--     Outbox/Inbox 而阻断或级联删除。
--
-- 风险与边界：
--   * error_message 仅保存有限长度的失败摘要，完整异常栈仍应进入日志系统。
--   * 本表会持续增长，生产环境必须配置保留期与归档任务；本迁移不自动清理。
--   * 审计写入失败不能伪装成重放成功；应用层需记录高优先级告警。
-- ============================================================

CREATE TABLE IF NOT EXISTS `event_dlq_replay_log`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT                COMMENT '主键编号',
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0                     COMMENT '租户编号',
    `event_id`        VARCHAR(64)  NOT NULL                               COMMENT '被重放的事件编号',
    `operator_id`     BIGINT       NOT NULL                               COMMENT '操作者用户编号',
    `operator_name`   VARCHAR(64)  NOT NULL                               COMMENT '操作者展示名称快照',
    `reason`          VARCHAR(512) NOT NULL                               COMMENT '重放原因',
    `result`          VARCHAR(16)  NOT NULL                               COMMENT '结果：SUCCESS/FAILED',
    `outbox_replayed` BIT          NOT NULL DEFAULT 0                     COMMENT '是否重新排队 Outbox',
    `inbox_replayed`  INT          NOT NULL DEFAULT 0                     COMMENT '恢复的 Inbox 死信数量',
    `error_message`   VARCHAR(2000) DEFAULT NULL                          COMMENT '失败摘要',
    `creator`         VARCHAR(64)  DEFAULT ''                             COMMENT '创建者',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
    `updater`         VARCHAR(64)  DEFAULT ''                             COMMENT '更新者',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         BIT          NOT NULL DEFAULT 0                     COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_dlq_replay_tenant_event` (`tenant_id`, `event_id`),
    KEY `idx_dlq_replay_tenant_create` (`tenant_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '死信人工重放审计（V105）';

-- 回滚：本表为纯审计新增表。若确认不再保留重放记录，可先导出全量审计再 DROP；
-- 不允许在未取证时直接删除。

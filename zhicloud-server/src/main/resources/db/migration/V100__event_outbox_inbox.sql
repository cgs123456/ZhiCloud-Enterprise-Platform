-- ============================================================
-- V100: 可靠集成数据面（event_outbox / event_inbox）
--
-- 背景：
--   03-目标架构与升级方案.md 第 4 节把「可靠集成数据面」列为中台底座，但仓库里
--   没有任何 Outbox/Inbox 持久化实现：跨模块（ERP/MES/WMS/QMS/TMS）的业务协同
--   只能靠直接调用或数据库轮询，既不能保证「业务事务与事件原子提交」，也没有
--   幂等消费、重试、死信与重放能力。02-企业级能力现状核验.md 把「建立最小可用的
--   Outbox/Inbox 或明确暂时禁止跨模块异步写流程」列为 P0 第 5 条。
--
-- 目标：
--   1. event_outbox：与业务表在同一数据库事务内写入，成为「事件已发生」的唯一事实。
--   2. event_inbox：消费侧按 (consumer_name, event_id) 唯一键去重，记录处理结果。
--   3. 为「阶段 1：单库 Outbox，不引入复杂消息拓扑」提供落库结构。
--
-- 语义：
--   * event_outbox.status：NEW 待投递 / PUBLISHING 投递中 / PUBLISHED 已投递 /
--     DEAD 超过重试阈值进入死信。NEW 与 PUBLISHING 都可被再次领取，条件是
--     next_retry_at 为空或已到期，因此重试靠 next_retry_at 而非状态字段区分。
--   * event_outbox.event_id 全局唯一，是消费侧幂等键，也是重放入口。
--   * event_outbox.partition_key 默认 tenant_id:aggregate_id，用于保证同一聚合的顺序，
--     并为将来接入 MQ 时提供分区键；本阶段不接入 MQ，仅落库备用。
--   * event_inbox 的唯一键 (consumer_name, event_id) 是「重复投递必须被去重」的
--     数据库级保证：同一事件被同一消费者收到多次时，第二次插入会被唯一键拒绝，
--     消费侧据此跳过而不是重复执行业务。
--   * payload 用 LONGTEXT 存 JSON 文本而不用 JSON 类型：JSON 类型在 H2 兼容模式与
--     部分历史 MySQL 版本上行为不一致，迁移与单元测试需要同构；代价是失去
--     JSON 函数校验，由应用层契约测试补偿。
--
-- 兼容性：
--   * CREATE TABLE IF NOT EXISTS，可重复执行；不删除、不改名、不删列。
--   * 两张表都是新增表，不存在历史数据回填问题。
--
-- 限界：
--   * 本迁移只建结构，不投递、不消费、不接 MQ；投递与消费由应用层实现。
--   * status 使用 VARCHAR 而非 ENUM：ENUM 变更需要改列，违反「只做加法」的迁移原则。
--   * 没有外键：Outbox/Inbox 不引用业务表，避免删除业务单据时阻断事件清理。
--   * 未建按月分区：阶段 1 数据量有限，先靠 idx_status_next_retry 覆盖扫描；
--     数据量增长后再评估归档与分区，不要提前设计。
-- ============================================================

-- ------------------------------------------------------------
-- 1. 事件发件箱：与业务表同事务写入
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `event_outbox`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `event_id`       VARCHAR(64)  NOT NULL                                COMMENT '事件唯一编号（消费侧幂等键、重放入口）',
    `tenant_id`      BIGINT       NOT NULL DEFAULT 0                      COMMENT '租户编号',
    `aggregate_type` VARCHAR(64)  NOT NULL                                COMMENT '聚合类型，如 erp_sale_out',
    `aggregate_id`   VARCHAR(64)  NOT NULL                                COMMENT '聚合编号',
    `event_type`     VARCHAR(128) NOT NULL                                COMMENT '事件类型，如 erp.sale-out.approved.v1',
    `schema_version` VARCHAR(16)  NOT NULL DEFAULT 'v1'                   COMMENT '事件契约版本',
    `partition_key`  VARCHAR(128) NOT NULL DEFAULT ''                     COMMENT '分区键，默认 tenant_id:aggregate_id',
    `payload`        LONGTEXT     NOT NULL                                COMMENT '事件体（JSON 文本）',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'NEW'                  COMMENT '状态：NEW/PUBLISHING/PUBLISHED/DEAD',
    `retry_count`    INT          NOT NULL DEFAULT 0                      COMMENT '已重试次数',
    `next_retry_at`  DATETIME     DEFAULT NULL                            COMMENT '下次重试时间（NULL 表示立即可领取）',
    `occurred_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '业务发生时间',
    `published_at`   DATETIME     DEFAULT NULL                            COMMENT '投递完成时间',
    `trace_id`       VARCHAR(64)  DEFAULT NULL                            COMMENT '链路追踪编号',
    `last_error`     VARCHAR(2000) DEFAULT NULL                           COMMENT '最后一次失败原因',
    `creator`        VARCHAR(64)  DEFAULT ''                              COMMENT '创建者',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updater`        VARCHAR(64)  DEFAULT ''                              COMMENT '更新者',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        BIT          NOT NULL DEFAULT 0                      COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_event_id` (`event_id`),
    KEY `idx_status_next_retry` (`status`, `next_retry_at`, `id`),
    KEY `idx_tenant_aggregate` (`tenant_id`, `aggregate_type`, `aggregate_id`, `occurred_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '事件发件箱（可靠集成数据面，与业务表同事务写入）';

-- ------------------------------------------------------------
-- 2. 事件收件箱：消费侧幂等与处理结果
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `event_inbox`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0                       COMMENT '租户编号',
    `consumer_name` VARCHAR(64)  NOT NULL                                 COMMENT '消费者名称',
    `event_id`      VARCHAR(64)  NOT NULL                                 COMMENT '事件唯一编号（与 outbox 对应）',
    `event_type`    VARCHAR(128) NOT NULL DEFAULT ''                      COMMENT '事件类型',
    `status`        VARCHAR(16)  NOT NULL DEFAULT 'RECEIVED'              COMMENT '状态：RECEIVED/PROCESSED/FAILED/DEAD',
    `retry_count`   INT          NOT NULL DEFAULT 0                       COMMENT '已重试次数',
    `received_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP       COMMENT '接收时间',
    `processed_at`  DATETIME     DEFAULT NULL                             COMMENT '处理完成时间',
    `error`         VARCHAR(2000) DEFAULT NULL                            COMMENT '最后一次失败原因',
    `creator`       VARCHAR(64)  DEFAULT ''                               COMMENT '创建者',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP       COMMENT '创建时间',
    `updater`       VARCHAR(64)  DEFAULT ''                               COMMENT '更新者',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT          NOT NULL DEFAULT 0                       COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_consumer_event` (`consumer_name`, `event_id`),
    KEY `idx_status_processed` (`status`, `processed_at`),
    KEY `idx_tenant_event` (`tenant_id`, `event_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '事件收件箱（消费侧幂等去重与处理结果）';

-- 回滚：两张表均为新增，未被业务写入前可直接 DROP；一旦已有事件，删除会丢失
-- 「已发生但未投递」的事实，必须先停写并导出事件后再执行。

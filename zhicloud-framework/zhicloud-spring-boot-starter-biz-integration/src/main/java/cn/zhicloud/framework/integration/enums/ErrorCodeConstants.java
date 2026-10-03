package cn.zhicloud.framework.integration.enums;

import cn.zhicloud.framework.common.exception.ErrorCode;

/**
 * 可靠集成数据面错误码（V100）
 *
 * <p>使用 1-099-001 段（framework 级集成 Starter）。1-099-000 段已被共享库存 Starter 占用，
 * 业务模块段为 erp 1-020 / wms 1-030 / mes 1-040 / crm 1-050 / qms 1-070，互不冲突。
 *
 * @author 智云集成治理
 */
public interface ErrorCodeConstants {

    // ========== 事件发件箱 ==========
    ErrorCode EVENT_OUTBOX_ARG_INVALID = new ErrorCode(1_099_001_001, "事件登记参数不完整：eventId / aggregateType / aggregateId / eventType / payload 均不能为空");
    ErrorCode EVENT_OUTBOX_PAYLOAD_INVALID = new ErrorCode(1_099_001_002, "事件体不是合法 JSON 文本");

    // ========== 事件收件箱 ==========
    ErrorCode EVENT_INBOX_DEAD = new ErrorCode(1_099_001_003, "事件已进入死信，拒绝继续消费，请先排查并人工重放");

    // ========== 死信运营 ==========
    ErrorCode EVENT_DLQ_ARG_INVALID = new ErrorCode(1_099_001_004, "死信重放参数不完整：eventId 不能为空");
    ErrorCode EVENT_DLQ_BATCH_LIMIT_EXCEEDED = new ErrorCode(1_099_001_005, "死信批量重放数量不能超过 100 条");
    ErrorCode EVENT_DLQ_NOT_REPLAYABLE = new ErrorCode(1_099_001_006, "事件当前状态不允许重放：仅 DEAD 事件，或存在 DEAD Inbox 的 PUBLISHED/NEW 事件可重放");
    ErrorCode EVENT_DLQ_CONCURRENT_MODIFIED = new ErrorCode(1_099_001_007, "事件状态已被其他操作修改，请刷新后重试");
    ErrorCode EVENT_DLQ_AUDIT_ARG_INVALID = new ErrorCode(1_099_001_008, "死信重放审计参数不完整：操作者、原因和时间窗必须完整且时间窗合法");
    ErrorCode EVENT_DLQ_OUTSIDE_WINDOW = new ErrorCode(1_099_001_009, "事件业务发生时间不在本次重放时间窗内");

    // ========== 租户边界 ==========
    ErrorCode EVENT_TENANT_REQUIRED = new ErrorCode(1_099_001_010, "事件操作缺少租户上下文，已拒绝执行");
    ErrorCode EVENT_TENANT_MISMATCH = new ErrorCode(1_099_001_011, "事件租户与当前操作租户不一致，已拒绝执行");

}

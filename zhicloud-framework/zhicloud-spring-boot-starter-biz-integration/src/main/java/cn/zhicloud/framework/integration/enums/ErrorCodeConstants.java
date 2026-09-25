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

}
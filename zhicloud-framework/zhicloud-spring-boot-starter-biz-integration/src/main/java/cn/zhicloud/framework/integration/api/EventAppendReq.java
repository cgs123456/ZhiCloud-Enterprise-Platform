package cn.zhicloud.framework.integration.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 事件登记请求（V100）
 *
 * <p>{@code eventId} 必须由调用方显式给出且业务侧可推导（如 {@code erp-sale-out-123-approved}）：
 * 它是消费侧幂等键，也是人工重放的入口。用自增主键当幂等键会导致「同一业务动作重复触发时无法去重」。
 *
 * @author 智云集成治理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAppendReq {

    /** 事件唯一编号（必填，业务侧可推导的幂等键） */
    private String eventId;

    /** 租户编号（空则从当前上下文推导；上下文缺失或租户冲突时拒绝） */
    private Long tenantId;

    /** 聚合类型，如 erp_sale_out（必填） */
    private String aggregateType;

    /** 聚合编号（必填） */
    private String aggregateId;

    /** 事件类型，如 erp.sale-out.approved.v1（必填） */
    private String eventType;

    /** 事件契约版本（空则默认 v1） */
    private String schemaVersion;

    /** 分区键（空则默认 tenantId:aggregateId） */
    private String partitionKey;

    /** 事件体 JSON 文本（必填且必须是合法 JSON） */
    private String payload;

    /** 业务发生时间（空则取当前时间） */
    private LocalDateTime occurredAt;

    /** 链路追踪编号 */
    private String traceId;

}
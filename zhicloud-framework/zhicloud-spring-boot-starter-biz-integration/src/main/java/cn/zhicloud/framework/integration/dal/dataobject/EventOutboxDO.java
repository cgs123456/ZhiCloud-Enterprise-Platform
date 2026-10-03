package cn.zhicloud.framework.integration.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 事件发件箱 DO（V100 可靠集成数据面）
 *
 * <p>与业务表在同一个数据库事务内写入，是「事件已发生」的唯一事实来源。业务事务提交则事件存在，
 * 业务事务回滚则事件一并回滚，从根本上消除「业务成功但消息没发出 / 消息发了但业务失败」的双写不一致。
 *
 * <p>字段与 V100__event_outbox_inbox.sql 的 {@code event_outbox} 列一一对应（{@code creator/create_time/
 * updater/update_time/deleted} 由 {@link BaseDO} 提供）。
 *
 * @author 智云集成治理
 */
@TableName("event_outbox")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventOutboxDO extends BaseDO {

    /**
     * 主键编号（DB 自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户内事件唯一编号（消费侧幂等键、重放入口）
     */
    private String eventId;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 聚合类型，如 erp_sale_out
     */
    private String aggregateType;

    /**
     * 聚合编号
     */
    private String aggregateId;

    /**
     * 事件类型，如 erp.sale-out.approved.v1
     */
    private String eventType;

    /**
     * 事件契约版本
     */
    private String schemaVersion;

    /**
     * 分区键，默认 tenant_id:aggregate_id
     */
    private String partitionKey;

    /**
     * 事件体（JSON 文本）
     */
    private String payload;

    /**
     * 状态：NEW / PUBLISHING / PUBLISHED / DEAD
     */
    private String status;

    /**
     * 当前投递 owner（每次领取生成，终态或重放时清空）
     */
    private String processingOwner;

    /**
     * 当前领取租约到期时间；过期后 PUBLISHING 可被其他实例接管
     */
    private LocalDateTime leaseUntil;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 下次重试时间（NULL 表示立即可领取）
     */
    private LocalDateTime nextRetryAt;

    /**
     * 业务发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 投递完成时间
     */
    private LocalDateTime publishedAt;

    /**
     * 链路追踪编号
     */
    private String traceId;

    /**
     * 最后一次失败原因
     */
    private String lastError;

}
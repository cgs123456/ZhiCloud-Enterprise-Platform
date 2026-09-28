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
 * 事件收件箱 DO（V100 可靠集成数据面）
 *
 * <p>按 {@code (tenant_id, consumer_name, event_id)} 唯一键做数据库级去重：同一事件被同一消费者重复投递时，
 * 第二次插入会被唯一键拒绝，消费侧据此直接跳过而不是重复执行业务。这是「至少一次投递 + 消费侧幂等」
 * 的最低保障，不依赖消费者自己实现幂等。
 *
 * @author 智云集成治理
 */
@TableName("event_inbox")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInboxDO extends BaseDO {

    /**
     * 主键编号（DB 自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 消费者名称
     */
    private String consumerName;

    /**
     * 事件唯一编号（与 outbox 对应）
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 状态：RECEIVED / PROCESSED / FAILED / DEAD
     */
    private String status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 接收时间
     */
    private LocalDateTime receivedAt;

    /**
     * 处理完成时间
     */
    private LocalDateTime processedAt;

    /**
     * 最后一次失败原因
     */
    private String error;

}
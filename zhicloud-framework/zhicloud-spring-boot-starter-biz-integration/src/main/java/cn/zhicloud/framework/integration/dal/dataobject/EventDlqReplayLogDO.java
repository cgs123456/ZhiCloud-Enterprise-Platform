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

/**
 * 死信重放审计记录（V105）
 *
 * <p>每一次人工重放请求都落一条审计，无论成功或失败。审计与事件状态变更分事务处理：
 * 成功审计与状态变更同事务提交，失败审计独立提交，避免主事务回滚抹掉失败痕迹。
 *
 * @author 智云集成治理
 */
@TableName("event_dlq_replay_log")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDlqReplayLogDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户编号
     */
    private Long tenantId;

    /**
     * 事件编号
     */
    private String eventId;

    /**
     * 操作者用户编号
     */
    private Long operatorId;

    /**
     * 操作者展示名称
     */
    private String operatorName;

    /**
     * 重放原因
     */
    private String reason;

    /**
     * 结果：SUCCESS / FAILED
     */
    private String result;

    /**
     * 是否重新排队 Outbox
     */
    private Boolean outboxReplayed;

    /**
     * 恢复的 Inbox 死信数量
     */
    private Integer inboxReplayed;

    /**
     * 失败原因
     */
    private String errorMessage;

}

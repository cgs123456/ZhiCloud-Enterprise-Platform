package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.integration.dal.dataobject.EventDlqReplayLogDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 集成死信查询与协调重放 Service（V105）
 *
 * <p>重放必须同时考虑 Outbox 与 Inbox：只恢复 Inbox，事件不会再次投递；只恢复 Outbox，
 * 消费侧会被 DEAD Inbox 拒绝。Service 在这里承担跨两张表的状态协调，管理端只负责权限、
 * 参数校验、审计与展示。
 *
 * @author 智云集成治理
 */
public interface EventDlqService {

    /**
     * 单次批量重放允许的最大事件数，防止管理端误操作拖垮在线事务
     */
    int MAX_BATCH_SIZE = 100;

    /**
     * 分页查询当前租户的 Outbox 死信
     */
    PageResult<EventOutboxDO> getDeadOutboxPage(PageParam pageParam, String eventId,
                                                 String aggregateType, String eventType,
                                                 LocalDateTime fromTime, LocalDateTime toTime);

    /**
     * 分页查询当前租户的 Inbox 死信
     */
    PageResult<EventInboxDO> getDeadInboxPage(PageParam pageParam, String eventId,
                                              String consumerName, String eventType,
                                              LocalDateTime fromTime, LocalDateTime toTime);
    /**
     * 分页查询当前租户的人工重放审计记录
     */
    PageResult<EventDlqReplayLogDO> getReplayLogPage(PageParam pageParam, String eventId,
                                                     Long operatorId, String result,
                                                     LocalDateTime fromTime, LocalDateTime toTime);

    /**
     * 按 eventId 协调重放 Outbox 与 Inbox 死信
     */
    EventDlqReplayResult replay(EventDlqReplayCommand command);

    /**
     * 批量协调重放；每条事件独立事务，单条失败不回滚其他事件
     */
    List<EventDlqReplayResult> replayBatch(EventDlqReplayCommand command);

}

package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventHandler;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;

/**
 * 事件收件箱 Service（V100）
 *
 * <p>提供「至少一次投递 + 消费侧幂等」的最小保障：去重键 {@code (tenant_id, consumer_name, event_id)}
 * 由数据库唯一约束兜底，业务处理器不需要自己实现幂等表。
 *
 * @author 智云集成治理
 */
public interface EventInboxService {

    /**
     * 消费事件（幂等）
     *
     * @param consumerName 消费者名称，参与去重唯一键
     * @param eventId      事件唯一编号
     * @param eventType    事件类型（冗余落库，便于按类型排查）
     * @param tenantId     租户编号
     * @param handler      业务处理器；为空则只做去重登记
     * @return {@link ConsumeResult#PROCESSED} 表示本次执行业务处理器成功；
     *         {@link ConsumeResult#DUPLICATE} 表示此前已成功处理；
     *         {@link ConsumeResult#IN_PROGRESS} 表示其他线程/节点持有未过期租约或发生状态争用，
     *         调用方不得确认 Outbox，应稍后重试
     * @throws Exception 业务处理器抛出的原异常会先落库再原样抛出，由调用方决定重试策略
     */
    ConsumeResult consume(String consumerName, String eventId, String eventType, Long tenantId,
                          EventHandler handler) throws Exception;

    /**
     * 查询去重记录
     */
    EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId, Long tenantId);

    /**
     * 人工重放：把死信记录重置为可再次消费
     */
    boolean replay(String consumerName, String eventId, Long tenantId);

    /**
     * 当前待重试失败存量（FAILED），供监控告警。
     *
     * <p>这是数据库中的当前状态数量，不是时间窗口失败率；真实失败率需要累计 Counter。
     */
    long countFailed();

    /**
     * 当前死信存量（DEAD），供监控告警。
     */
    long countDead();

    /**
     * 最老待消费事件已等待秒数（RECEIVED + FAILED），供监控告警。
     *
     * <p>没有待消费事件时返回 0；统计 RECEIVED + PROCESSING + FAILED，DEAD 已停止自动消费，不计入该口径。
     */
    long oldestPendingSeconds();

}

package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.api.EventHandler;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;

/**
 * 事件收件箱 Service（V100）
 *
 * <p>提供「至少一次投递 + 消费侧幂等」的最小保障：去重键 {@code (consumer_name, event_id)}
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
     * @return 本次是否真正执行了业务处理器
     * @throws Exception 业务处理器抛出的原异常会先落库再原样抛出，由调用方决定重试策略
     */
    ConsumeResult consume(String consumerName, String eventId, String eventType, Long tenantId,
                          EventHandler handler) throws Exception;

    /**
     * 查询去重记录
     */
    EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId);

    /**
     * 人工重放：把死信记录重置为可再次消费
     */
    boolean replay(String consumerName, String eventId);

}
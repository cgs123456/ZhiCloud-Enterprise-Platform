package cn.zhicloud.framework.integration.api;

import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;

/**
 * 事件消费者（V100 阶段 1：单库中继）
 *
 * <p>业务模块实现本接口并注册为 Spring Bean，{@code EventRelayService} 会自动按 {@code supports} 分发。
 * 阶段 1 不引入 MQ，中继是同一个数据库内的轮询；消费者仍然必须幂等，因为「至少一次投递」是本设计的
 * 基本假设（消费侧幂等由 {@code event_inbox} 唯一键兜底）。
 *
 * @author 智云集成治理
 */
public interface EventConsumer {

    /**
     * 消费者名称，会写入 {@code event_inbox.consumer_name} 并参与去重唯一键，必须全局稳定唯一。
     * <p>改名等于换了一个消费者，历史事件会被重新消费一次，务必谨慎。
     */
    String name();

    /**
     * 是否处理该事件类型
     */
    boolean supports(String eventType);

    /**
     * 执行业务处理；抛异常即视为失败，由中继触发重试
     */
    void handle(EventOutboxDO event) throws Exception;

}
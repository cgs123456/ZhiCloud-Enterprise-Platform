package cn.iocoder.yudao.module.erp.domain.sale.event;

import cn.iocoder.yudao.module.erp.domain.sale.aggregate.SaleOrderAggregate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 销售订单领域事件发布器（DDD 试点）
 *
 * <p>封装 Spring {@link ApplicationEventPublisher}，将领域事件通过 Spring 机制发布。
 * 应用服务在聚合根完成状态变更并持久化后，调用本发布器发布对应事件；
 * 监听方通过 {@code @TransactionalEventListener} 在事务提交后异步处理，实现跨模块解耦。
 *
 * @author DDD 试点
 */
@Component
public class SaleOrderEventPublisher {

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    /**
     * 发布销售订单审核确认事件
     *
     * @param aggregate 已确认的聚合根
     */
    public void publishConfirmed(SaleOrderAggregate aggregate) {
        SaleOrderConfirmedEvent event = new SaleOrderConfirmedEvent(
                aggregate.getOrderId(), aggregate.getTenantId(),
                aggregate.getCustomerId(), aggregate.getTotalAmount());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布销售订单出库事件
     *
     * @param aggregate    已出库的聚合根
     * @param outCountMap  出库明细（itemId → 累计出库数量）
     */
    public void publishShipped(SaleOrderAggregate aggregate, Map<Long, BigDecimal> outCountMap) {
        SaleOrderShippedEvent event = new SaleOrderShippedEvent(
                aggregate.getOrderId(), aggregate.getTenantId(),
                outCountMap == null ? new LinkedHashMap<>() : outCountMap);
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布销售订单取消事件
     *
     * @param aggregate 已取消的聚合根
     * @param reason    取消原因
     */
    public void publishCancelled(SaleOrderAggregate aggregate, String reason) {
        SaleOrderCancelledEvent event = new SaleOrderCancelledEvent(
                aggregate.getOrderId(), aggregate.getTenantId(), reason);
        applicationEventPublisher.publishEvent(event);
    }
}

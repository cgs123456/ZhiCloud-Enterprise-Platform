package cn.iocoder.yudao.module.erp.domain.sale.event;

import java.time.LocalDateTime;

/**
 * 销售订单领域事件抽象基类（DDD 试点）
 *
 * <p>所有销售订单领域事件的公共父类，封装事件发生的元数据。
 * 领域事件本身为纯 Java 对象，通过 {@link SaleOrderEventPublisher} 借助 Spring
 * {@code ApplicationEventPublisher} 发布，监听方通过 {@code @TransactionalEventListener} 订阅。
 *
 * <p>领域事件用于跨模块解耦：例如销售订单审核确认后，库存模块可监听
 * {@link SaleOrderConfirmedEvent} 释放 / 预留库存，无需销售模块直接调用库存 Service。
 *
 * @author DDD 试点
 */
public abstract class AbstractSaleOrderEvent {

    /**
     * 订单编号
     */
    private final Long orderId;
    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;
    /**
     * 租户编号
     */
    private final Long tenantId;

    protected AbstractSaleOrderEvent(Long orderId, Long tenantId) {
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public Long getTenantId() {
        return tenantId;
    }
}

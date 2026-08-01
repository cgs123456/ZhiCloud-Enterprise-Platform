package cn.iocoder.yudao.module.erp.domain.saleorder.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 销售订单创建事件（DDD 试点）
 *
 * <p>当销售订单被创建时由聚合根注册。下游模块可监听此事件做初始化操作，
 * 例如生成应收记录、推送通知等。
 *
 * @author DDD 试点
 */
public class SaleOrderCreatedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private final Long orderId;
    /**
     * 销售订单号
     */
    private final String orderNo;
    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    public SaleOrderCreatedEvent(Long orderId, String orderNo) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}

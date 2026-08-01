package cn.iocoder.yudao.module.erp.domain.saleorder.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单出库数量更新事件（DDD 试点）
 *
 * <p>当销售订单的出库数量发生变化时由聚合根注册。库存模块可监听此事件，
 * 根据最新的出库数量扣减实际库存。
 *
 * @author DDD 试点
 */
public class SaleOrderOutCountUpdatedEvent implements Serializable {

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
     * 更新后的出库数量（累计）
     */
    private final BigDecimal newOutCount;
    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    public SaleOrderOutCountUpdatedEvent(Long orderId, String orderNo, BigDecimal newOutCount) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.newOutCount = newOutCount;
        this.occurredOn = LocalDateTime.now();
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getNewOutCount() {
        return newOutCount;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}

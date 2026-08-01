package cn.iocoder.yudao.module.erp.domain.sale.event;

import java.math.BigDecimal;

/**
 * 销售订单审核确认事件（DDD 试点）
 *
 * <p>当销售订单通过审核确认时发布。库存模块可监听此事件，预留 / 锁定对应产品的可用库存。
 *
 * @author DDD 试点
 */
public class SaleOrderConfirmedEvent extends AbstractSaleOrderEvent {

    /**
     * 客户编号
     */
    private final Long customerId;
    /**
     * 订单总金额
     */
    private final BigDecimal totalAmount;

    public SaleOrderConfirmedEvent(Long orderId, Long tenantId, Long customerId, BigDecimal totalAmount) {
        super(orderId, tenantId);
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}

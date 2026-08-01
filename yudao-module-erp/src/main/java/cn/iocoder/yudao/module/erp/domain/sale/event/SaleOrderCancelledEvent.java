package cn.iocoder.yudao.module.erp.domain.sale.event;

/**
 * 销售订单取消事件（DDD 试点）
 *
 * <p>当销售订单被取消时发布。库存模块可监听此事件，释放此前预留的库存。
 *
 * @author DDD 试点
 */
public class SaleOrderCancelledEvent extends AbstractSaleOrderEvent {

    /**
     * 取消原因
     */
    private final String reason;

    public SaleOrderCancelledEvent(Long orderId, Long tenantId, String reason) {
        super(orderId, tenantId);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}

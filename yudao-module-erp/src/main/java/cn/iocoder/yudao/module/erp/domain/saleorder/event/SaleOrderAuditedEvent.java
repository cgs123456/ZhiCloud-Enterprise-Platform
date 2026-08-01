package cn.iocoder.yudao.module.erp.domain.saleorder.event;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 销售订单审批通过事件（DDD 试点）
 *
 * <p>当销售订单审批通过时由聚合根注册。下游模块可监听此事件：
 * <ul>
 *     <li>库存模块：预留 / 锁定可用库存</li>
 *     <li>财务模块：生成应收账款记录</li>
 *     <li>BPM：触发后续审批流程</li>
 * </ul>
 *
 * @author DDD 试点
 */
public class SaleOrderAuditedEvent implements Serializable {

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

    public SaleOrderAuditedEvent(Long orderId, String orderNo) {
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

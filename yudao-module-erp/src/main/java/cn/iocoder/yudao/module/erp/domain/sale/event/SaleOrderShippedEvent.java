package cn.iocoder.yudao.module.erp.domain.sale.event;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 销售订单出库事件（DDD 试点）
 *
 * <p>当销售订单发生出库时发布。库存模块可监听此事件，扣减对应产品的实际库存。
 *
 * @author DDD 试点
 */
public class SaleOrderShippedEvent extends AbstractSaleOrderEvent {

    /**
     * 出库明细：key = 订单明细编号（itemId），value = 该明细累计出库数量
     */
    private final Map<Long, BigDecimal> outCountMap;

    public SaleOrderShippedEvent(Long orderId, Long tenantId, Map<Long, BigDecimal> outCountMap) {
        super(orderId, tenantId);
        this.outCountMap = outCountMap;
    }

    public Map<Long, BigDecimal> getOutCountMap() {
        return outCountMap;
    }
}

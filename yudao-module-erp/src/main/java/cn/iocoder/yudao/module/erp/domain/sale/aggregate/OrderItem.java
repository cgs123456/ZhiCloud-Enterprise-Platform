package cn.iocoder.yudao.module.erp.domain.sale.aggregate;

import java.math.BigDecimal;

/**
 * 销售订单明细值对象（DDD 试点）
 *
 * <p>不可变值对象，描述一条订单明细的核心属性。由 {@link SaleOrderAggregate} 在重建 / 出库时
 * 构造新的实例，保证明细一旦创建即不可变，出库数量等变更通过生成新实例体现。
 *
 * @author DDD 试点
 */
public record OrderItem(
        Long itemId,
        Long productId,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        BigDecimal outCount,
        BigDecimal returnCount
) {

    /**
     * 剩余可出库数量 = 订单数量 - 已出库数量
     *
     * @return 剩余数量，不会为负
     */
    public BigDecimal getRemainingCount() {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal out = outCount == null ? BigDecimal.ZERO : outCount;
        return qty.subtract(out);
    }
}

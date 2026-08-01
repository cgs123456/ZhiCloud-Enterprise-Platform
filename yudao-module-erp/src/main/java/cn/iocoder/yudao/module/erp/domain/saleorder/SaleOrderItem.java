package cn.iocoder.yudao.module.erp.domain.saleorder;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 销售订单明细值对象（DDD 试点）
 *
 * <p>不可变值对象，描述一条订单明细。出库 / 退货数量等变更由聚合根统一管理，
 * 明细本身不暴露修改方法，保证聚合内不变式。
 *
 * @author DDD 试点
 */
@Getter
public class SaleOrderItem {

    /**
     * 明细编号
     */
    private final Long id;
    /**
     * 产品编号
     */
    private final Long productId;
    /**
     * 产品单位编号
     */
    private final Long productUnitId;
    /**
     * 数量
     */
    private final BigDecimal quantity;
    /**
     * 单价
     */
    private final BigDecimal unitPrice;
    /**
     * 税率，百分比
     */
    private final BigDecimal taxRate;
    /**
     * 小计（不含税金额 = 单价 × 数量）
     */
    private final BigDecimal subtotal;
    /**
     * 税额
     */
    private final BigDecimal taxPrice;
    /**
     * 已出库数量
     */
    private final BigDecimal outCount;
    /**
     * 已退货数量
     */
    private final BigDecimal returnCount;
    /**
     * 备注
     */
    private final String remark;

    public SaleOrderItem(Long id, Long productId, Long productUnitId, BigDecimal quantity,
                         BigDecimal unitPrice, BigDecimal taxRate, BigDecimal subtotal,
                         BigDecimal taxPrice, BigDecimal outCount, BigDecimal returnCount,
                         String remark) {
        this.id = id;
        this.productId = productId;
        this.productUnitId = productUnitId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.taxRate = taxRate;
        this.subtotal = subtotal;
        this.taxPrice = taxPrice;
        this.outCount = outCount;
        this.returnCount = returnCount;
        this.remark = remark;
    }

    /**
     * 剩余可出库数量 = 数量 - 已出库数量
     *
     * @return 剩余数量
     */
    public BigDecimal getRemainingOutCount() {
        BigDecimal qty = quantity == null ? BigDecimal.ZERO : quantity;
        BigDecimal out = outCount == null ? BigDecimal.ZERO : outCount;
        return qty.subtract(out);
    }

    /**
     * 可退货数量 = 已出库数量 - 已退货数量
     *
     * @return 可退货数量
     */
    public BigDecimal getReturnableCount() {
        BigDecimal out = outCount == null ? BigDecimal.ZERO : outCount;
        BigDecimal ret = returnCount == null ? BigDecimal.ZERO : returnCount;
        return out.subtract(ret);
    }
}

package cn.iocoder.yudao.module.erp.domain.saleorder.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新销售订单退货数量命令（DDD 试点）
 *
 * <p>由退货单完成时触发，传递给聚合根的 {@code updateReturnCount()} 方法。
 * delta 为本次退货增量（非累计值），聚合根内部累加并校验不超过已出库数量。
 *
 * @author DDD 试点
 */
@Data
public class UpdateSaleOrderReturnCountCommand {

    /**
     * 订单编号
     */
    private Long orderId;
    /**
     * 本次退货增量数量
     */
    private BigDecimal delta;

    public UpdateSaleOrderReturnCountCommand() {
    }

    public UpdateSaleOrderReturnCountCommand(Long orderId, BigDecimal delta) {
        this.orderId = orderId;
        this.delta = delta;
    }
}

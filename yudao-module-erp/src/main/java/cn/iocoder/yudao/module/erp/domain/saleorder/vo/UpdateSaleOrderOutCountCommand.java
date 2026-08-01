package cn.iocoder.yudao.module.erp.domain.saleorder.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新销售订单出库数量命令（DDD 试点）
 *
 * <p>由出库单完成时触发，传递给聚合根的 {@code updateOutCount()} 方法。
 * delta 为本次出库增量（非累计值），聚合根内部累加并校验不超过订单总数量。
 *
 * @author DDD 试点
 */
@Data
public class UpdateSaleOrderOutCountCommand {

    /**
     * 订单编号
     */
    private Long orderId;
    /**
     * 本次出库增量数量
     */
    private BigDecimal delta;

    public UpdateSaleOrderOutCountCommand() {
    }

    public UpdateSaleOrderOutCountCommand(Long orderId, BigDecimal delta) {
        this.orderId = orderId;
        this.delta = delta;
    }
}

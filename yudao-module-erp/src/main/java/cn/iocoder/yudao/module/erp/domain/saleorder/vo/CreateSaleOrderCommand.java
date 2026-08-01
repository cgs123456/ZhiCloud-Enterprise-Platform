package cn.iocoder.yudao.module.erp.domain.saleorder.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 创建销售订单命令（DDD 试点）
 *
 * <p>命令对象，用于驱动聚合根的创建。仅包含业务字段，不含持久层关注点（如 id、createTime）。
 * 命令由应用层接收，传递给聚合根工厂方法 {@code ErpSaleOrderAggregate.create()}。
 *
 * @author DDD 试点
 */
@Data
public class CreateSaleOrderCommand {

    /**
     * 销售订单号
     */
    private String no;
    /**
     * 客户编号
     */
    private Long customerId;
    /**
     * 结算账户编号
     */
    private Long accountId;
    /**
     * 销售员编号
     */
    private Long saleUserId;
    /**
     * 下单时间
     */
    private LocalDateTime orderTime;
    /**
     * 优惠率，百分比
     */
    private BigDecimal discountPercent;
    /**
     * 定金金额
     */
    private BigDecimal depositPrice;
    /**
     * 币种编号
     */
    private Long currencyId;
    /**
     * 汇率
     */
    private BigDecimal exchangeRate;
    /**
     * 附件地址
     */
    private String fileUrl;
    /**
     * 备注
     */
    private String remark;
    /**
     * 订单明细列表
     */
    private List<Item> items;

    /**
     * 明细项
     */
    @Data
    public static class Item {
        /**
         * 产品编号
         */
        private Long productId;
        /**
         * 产品单位编号
         */
        private Long productUnitId;
        /**
         * 单价
         */
        private BigDecimal productPrice;
        /**
         * 数量
         */
        private BigDecimal count;
        /**
         * 税率，百分比
         */
        private BigDecimal taxPercent;
        /**
         * 备注
         */
        private String remark;
    }
}

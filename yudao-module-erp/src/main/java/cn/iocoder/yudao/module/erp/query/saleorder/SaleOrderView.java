package cn.iocoder.yudao.module.erp.query.saleorder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单读模型视图（CQRS 试点）
 *
 * <p>扁平化的只读视图，包含客户名称、销售员名称、产品名称等冗余字段，
 * 专为查询优化，避免前端二次查询。
 *
 * <p>与写模型的 DO 分离，读侧变更不影响写侧契约。
 *
 * @author DDD 试点
 */
@Schema(description = "管理后台 - ERP 销售订单读模型（CQRS 试点）")
@Data
public class SaleOrderView {

    // ========== 订单头 ==========
    @Schema(description = "订单编号", example = "1024")
    private Long id;

    @Schema(description = "销售订单号", example = "XS20240101001")
    private String no;

    @Schema(description = "销售状态", example = "20")
    private Integer status;

    @Schema(description = "客户编号", example = "1724")
    private Long customerId;

    @Schema(description = "客户名称", example = "上海某某公司")
    private String customerName;

    @Schema(description = "结算账户编号", example = "1")
    private Long accountId;

    @Schema(description = "销售员编号", example = "1")
    private Long saleUserId;

    @Schema(description = "销售员名称", example = "张三")
    private String saleUserName;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    // ========== 金额 ==========
    @Schema(description = "合计数量", example = "100")
    private BigDecimal totalCount;

    @Schema(description = "合计产品价格", example = "9000.00")
    private BigDecimal totalProductPrice;

    @Schema(description = "合计税额", example = "900.00")
    private BigDecimal totalTaxPrice;

    @Schema(description = "优惠金额", example = "100.00")
    private BigDecimal discountPrice;

    @Schema(description = "最终合计价格", example = "9800.00")
    private BigDecimal totalPrice;

    @Schema(description = "定金金额", example = "500.00")
    private BigDecimal depositPrice;

    // ========== 多币种 ==========
    @Schema(description = "币种编号", example = "1")
    private Long currencyId;

    @Schema(description = "汇率", example = "1.0000")
    private BigDecimal exchangeRate;

    @Schema(description = "本位币总金额", example = "9800.00")
    private BigDecimal baseCurrencyTotalPrice;

    // ========== 出库 / 退货 ==========
    @Schema(description = "累计出库数量", example = "10")
    private BigDecimal outCount;

    @Schema(description = "累计退货数量", example = "0")
    private BigDecimal returnCount;

    // ========== 其他 ==========
    @Schema(description = "附件地址")
    private String fileUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "订单明细列表")
    private List<Item> items;

    /**
     * 订单明细视图
     */
    @Data
    @Schema(description = "销售订单明细视图")
    public static class Item {

        @Schema(description = "明细编号", example = "2048")
        private Long id;

        @Schema(description = "产品编号", example = "1")
        private Long productId;

        @Schema(description = "产品名称", example = "键盘")
        private String productName;

        @Schema(description = "产品单位编号", example = "1")
        private Long productUnitId;

        @Schema(description = "数量", example = "10")
        private BigDecimal quantity;

        @Schema(description = "单价", example = "99.00")
        private BigDecimal unitPrice;

        @Schema(description = "税率", example = "13")
        private BigDecimal taxRate;

        @Schema(description = "小计", example = "990.00")
        private BigDecimal subtotal;

        @Schema(description = "税额", example = "128.70")
        private BigDecimal taxPrice;

        @Schema(description = "出库数量", example = "5")
        private BigDecimal outCount;

        @Schema(description = "退货数量", example = "0")
        private BigDecimal returnCount;

        @Schema(description = "备注")
        private String remark;
    }
}

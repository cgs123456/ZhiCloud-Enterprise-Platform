package cn.iocoder.yudao.module.erp.query.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 销售订单详情 VO（CQRS 读模型）
 *
 * <p>只读投影，用于详情查询场景，包含订单头 + 明细，冗余客户名称、产品名称。
 *
 * @author DDD 试点
 */
@Schema(description = "管理后台 - ERP 销售订单详情（CQRS 读模型）")
@Data
public class SaleOrderDetailVO {

    @Schema(description = "订单编号", example = "1024")
    private Long id;

    @Schema(description = "销售订单号", example = "XS20240101001")
    private String orderNo;

    @Schema(description = "客户编号", example = "1724")
    private Long customerId;

    @Schema(description = "客户名称", example = "上海某某公司")
    private String customerName;

    @Schema(description = "总金额", example = "9999.00")
    private BigDecimal totalAmount;

    @Schema(description = "销售状态", example = "20")
    private Integer status;

    @Schema(description = "下单时间")
    private LocalDateTime orderDate;

    @Schema(description = "合计数量", example = "100")
    private BigDecimal totalCount;

    @Schema(description = "出库数量", example = "10")
    private BigDecimal outCount;

    @Schema(description = "退货数量", example = "0")
    private BigDecimal returnCount;

    @Schema(description = "备注", example = "加急")
    private String remark;

    @Schema(description = "订单明细列表")
    private List<Item> items;

    /**
     * 订单明细项
     */
    @Data
    @Schema(description = "销售订单明细项")
    public static class Item {

        @Schema(description = "明细编号", example = "2048")
        private Long itemId;

        @Schema(description = "产品编号", example = "1")
        private Long productId;

        @Schema(description = "产品名称", example = "键盘")
        private String productName;

        @Schema(description = "数量", example = "10")
        private BigDecimal quantity;

        @Schema(description = "单价", example = "99.00")
        private BigDecimal unitPrice;

        @Schema(description = "金额", example = "990.00")
        private BigDecimal amount;

        @Schema(description = "出库数量", example = "5")
        private BigDecimal outCount;

        @Schema(description = "退货数量", example = "0")
        private BigDecimal returnCount;

    }

}

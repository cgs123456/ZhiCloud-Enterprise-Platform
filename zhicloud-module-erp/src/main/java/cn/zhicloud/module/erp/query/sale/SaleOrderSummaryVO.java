package cn.zhicloud.module.erp.query.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 销售订单摘要 VO（CQRS 读模型）
 *
 * <p>只读投影，用于列表 / 分页查询场景，字段经过裁剪，不含明细。
 * 冗余客户名称，避免前端二次查询。
 *
 * @author DDD 试点
 */
@Schema(description = "管理后台 - ERP 销售订单摘要（CQRS 读模型）")
@Data
public class SaleOrderSummaryVO {

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

    @Schema(description = "出库数量", example = "10")
    private BigDecimal outCount;

    @Schema(description = "退货数量", example = "0")
    private BigDecimal returnCount;

}

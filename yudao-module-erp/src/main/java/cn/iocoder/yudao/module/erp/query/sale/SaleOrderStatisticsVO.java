package cn.iocoder.yudao.module.erp.query.sale;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售订单统计 VO（CQRS 读模型）
 *
 * <p>只读投影，用于统计报表场景，按状态、客户维度聚合订单数据。
 *
 * @author DDD 试点
 */
@Schema(description = "管理后台 - ERP 销售订单统计（CQRS 读模型）")
@Data
public class SaleOrderStatisticsVO {

    @Schema(description = "订单总数", example = "100")
    private Integer totalOrderCount;

    @Schema(description = "订单总金额", example = "99999.00")
    private BigDecimal totalAmount;

    @Schema(description = "按状态分组统计")
    private List<StatusStat> byStatus;

    @Schema(description = "按客户分组统计")
    private List<CustomerStat> byCustomer;

    /**
     * 按状态聚合统计
     */
    @Data
    @Schema(description = "按状态分组统计项")
    public static class StatusStat {

        @Schema(description = "销售状态", example = "20")
        private Integer status;

        @Schema(description = "订单数", example = "50")
        private Integer count;

        @Schema(description = "金额合计", example = "49999.00")
        private BigDecimal amount;

    }

    /**
     * 按客户聚合统计
     */
    @Data
    @Schema(description = "按客户分组统计项")
    public static class CustomerStat {

        @Schema(description = "客户编号", example = "1724")
        private Long customerId;

        @Schema(description = "客户名称", example = "上海某某公司")
        private String customerName;

        @Schema(description = "订单数", example = "10")
        private Integer count;

        @Schema(description = "金额合计", example = "9999.00")
        private BigDecimal amount;

    }

}

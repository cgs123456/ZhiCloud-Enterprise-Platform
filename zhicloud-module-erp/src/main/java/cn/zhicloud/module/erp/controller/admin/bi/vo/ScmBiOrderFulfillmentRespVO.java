package cn.zhicloud.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 订单履约率 Response VO")
@Data
public class ScmBiOrderFulfillmentRespVO {

    @Schema(description = "销售订单总数", example = "100")
    private Integer totalOrders;

    @Schema(description = "已履约订单数（已全部出库）", example = "85")
    private Integer fulfilledOrders;

    @Schema(description = "订单履约率", example = "0.85")
    private BigDecimal fulfillmentRate;

}

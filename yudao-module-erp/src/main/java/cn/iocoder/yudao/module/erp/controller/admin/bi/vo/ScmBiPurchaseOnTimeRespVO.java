package cn.iocoder.yudao.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 采购到货及时率 Response VO")
@Data
public class ScmBiPurchaseOnTimeRespVO {

    @Schema(description = "采购入库单总数", example = "100")
    private Integer totalOrders;

    @Schema(description = "按时到货单数", example = "90")
    private Integer onTimeOrders;

    @Schema(description = "采购到货及时率", example = "0.90")
    private BigDecimal onTimeRate;

}

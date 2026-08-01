package cn.iocoder.yudao.module.erp.controller.admin.bi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 供应链 BI 价格波动 Response VO")
@Data
public class ScmBiPriceFluctuationRespVO {

    @Schema(description = "产品编号", example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "平均采购价", example = "100.00")
    private BigDecimal avgPrice;

    @Schema(description = "最高采购价", example = "120.00")
    private BigDecimal maxPrice;

    @Schema(description = "最低采购价", example = "80.00")
    private BigDecimal minPrice;

    @Schema(description = "价格波动率", example = "0.50")
    private BigDecimal fluctuationRate;

}

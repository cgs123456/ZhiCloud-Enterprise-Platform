package cn.zhicloud.module.tms.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - TMS 运费计算结果 Response VO")
@Data
public class TmsFreightCalculateRespVO {

    @Schema(description = "计费方式", example = "10")
    private Integer billingMethod;

    @Schema(description = "计费数量", example = "500.0")
    private BigDecimal billingQuantity;

    @Schema(description = "单价", example = "3.00")
    private BigDecimal unitPrice;

    @Schema(description = "基础运费 = 计费数量 × 单价", example = "1500.00")
    private BigDecimal baseAmount;

    @Schema(description = "附加费用", example = "50.00")
    private BigDecimal surcharge;

    @Schema(description = "折扣金额", example = "20.00")
    private BigDecimal discountAmount;

    @Schema(description = "运费总额 = 基础运费 + 附加费用 - 折扣金额", example = "1530.00")
    private BigDecimal totalAmount;

}

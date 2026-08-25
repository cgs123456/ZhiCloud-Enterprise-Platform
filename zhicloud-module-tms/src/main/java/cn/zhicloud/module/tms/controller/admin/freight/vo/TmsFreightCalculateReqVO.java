package cn.zhicloud.module.tms.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - TMS 运费自动计算 Request VO")
@Data
public class TmsFreightCalculateReqVO {

    @Schema(description = "运单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "运单编号不能为空")
    private Long shipmentId;

    @Schema(description = "计费方式（10 按重量 / 20 按体积 / 30 按件数 / 40 整车一口价 / 50 里程计费）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "计费方式不能为空")
    private Integer billingMethod;

    @Schema(description = "单价（按重量/体积/件数时填写）", example = "3.00")
    private BigDecimal unitPrice;

    @Schema(description = "附加费用", example = "50.00")
    private BigDecimal surcharge;

    @Schema(description = "折扣金额", example = "20.00")
    private BigDecimal discountAmount;

}

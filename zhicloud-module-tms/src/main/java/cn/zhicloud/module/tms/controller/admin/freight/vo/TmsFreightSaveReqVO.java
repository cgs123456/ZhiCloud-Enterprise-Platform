package cn.zhicloud.module.tms.controller.admin.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - TMS 运费结算单新增/修改 Request VO")
@Data
public class TmsFreightSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "结算单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FRT20240101001")
    @NotEmpty(message = "结算单号不能为空")
    private String no;

    @Schema(description = "运单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "运单编号不能为空")
    private Long shipmentId;

    @Schema(description = "承运商编号", example = "1024")
    private Long carrierId;

    @Schema(description = "计费方式（10 按重量 / 20 按体积 / 30 按件数 / 40 整车一口价 / 50 里程计费）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "计费方式不能为空")
    private Integer billingMethod;

    @Schema(description = "计费数量", example = "500.0")
    private BigDecimal billingQuantity;

    @Schema(description = "单价", example = "3.00")
    private BigDecimal unitPrice;

    @Schema(description = "附加费用", example = "50.00")
    private BigDecimal surcharge;

    @Schema(description = "折扣金额", example = "20.00")
    private BigDecimal discountAmount;

    @Schema(description = "运费总额", example = "1530.00")
    private BigDecimal totalAmount;

    @Schema(description = "结算状态（10 待审核 / 20 已审核 / 30 已结算 / 40 已驳回）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}

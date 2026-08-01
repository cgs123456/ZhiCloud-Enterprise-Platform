package cn.iocoder.yudao.module.wms.controller.admin.billing.vo.bill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 计费账单明细 Response VO")
@Data
public class WmsBillingBillLineRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "账单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long billId;

    @Schema(description = "计费合同条款编号", example = "2048")
    private Long contractItemId;

    @Schema(description = "费用类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer feeType;

    @Schema(description = "计费方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer feeMode;

    @Schema(description = "数量（天数/次数/件数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private BigDecimal quantity;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.50")
    private BigDecimal unitPrice;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "45.00")
    private BigDecimal amount;

    @Schema(description = "关联单据号", example = "RK202605110001")
    private String referenceOrderNo;

    @Schema(description = "备注", example = "备注")
    private String remark;

}

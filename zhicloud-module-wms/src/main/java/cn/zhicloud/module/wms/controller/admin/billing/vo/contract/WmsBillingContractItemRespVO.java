package cn.zhicloud.module.wms.controller.admin.billing.vo.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 计费合同条款 Response VO")
@Data
public class WmsBillingContractItemRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "计费合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long contractId;

    @Schema(description = "费用类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer feeType;

    @Schema(description = "计费方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer feeMode;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.50")
    private BigDecimal unitPrice;

    @Schema(description = "最低收费", example = "100.00")
    private BigDecimal minCharge;

    @Schema(description = "备注", example = "备注")
    private String remark;

}

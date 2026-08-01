package cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - WMS 计费合同条款保存 Request VO")
@Data
public class WmsBillingContractItemSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "费用类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "费用类型不能为空")
    private Integer feeType;

    @Schema(description = "计费方式", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "计费方式不能为空")
    private Integer feeMode;

    @Schema(description = "单价", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.50")
    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0", message = "单价不能小于 0")
    private BigDecimal unitPrice;

    @Schema(description = "最低收费", example = "100.00")
    @DecimalMin(value = "0", message = "最低收费不能小于 0")
    private BigDecimal minCharge;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}

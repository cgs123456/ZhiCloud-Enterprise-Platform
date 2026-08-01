package cn.iocoder.yudao.module.wms.controller.admin.billing.vo.bill;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 计费账单保存 Request VO")
@Data
public class WmsBillingBillSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZD202605110001")
    @NotBlank(message = "账单号不能为空")
    @Size(max = 64, message = "账单号长度不能超过 64 个字符")
    private String billNo;

    @Schema(description = "货主编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "货主不能为空")
    private Long ownerId;

    @Schema(description = "计费周期开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计费周期开始时间不能为空")
    private LocalDateTime billingPeriodStart;

    @Schema(description = "计费周期结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "计费周期结束时间不能为空")
    private LocalDateTime billingPeriodEnd;

    @Schema(description = "账单状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}

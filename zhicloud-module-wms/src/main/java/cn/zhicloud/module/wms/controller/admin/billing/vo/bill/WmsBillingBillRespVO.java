package cn.zhicloud.module.wms.controller.admin.billing.vo.bill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - WMS 计费账单 Response VO")
@Data
public class WmsBillingBillRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "账单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ZD202605110001")
    private String billNo;

    @Schema(description = "货主编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long ownerId;
    @Schema(description = "货主名称", example = "某某货主")
    private String ownerName;

    @Schema(description = "计费周期开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime billingPeriodStart;

    @Schema(description = "计费周期结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime billingPeriodEnd;

    @Schema(description = "总金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "账单状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "账单明细")
    private List<WmsBillingBillLineRespVO> lines;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "创建者", example = "1")
    private String creator;
    @Schema(description = "创建者名称", example = "智云")
    private String creatorName;

}

package cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - WMS 计费合同 Response VO")
@Data
public class WmsBillingContractRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "合同号", requiredMode = Schema.RequiredMode.REQUIRED, example = "HT202605110001")
    private String contractNo;

    @Schema(description = "货主编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long ownerId;
    @Schema(description = "货主名称", example = "某某货主")
    private String ownerName;

    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某货主仓储合同")
    private String contractName;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    @Schema(description = "失效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate endDate;

    @Schema(description = "合同状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "计费条款")
    private List<WmsBillingContractItemRespVO> items;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "创建者", example = "1")
    private String creator;
    @Schema(description = "创建者名称", example = "芋道")
    private String creatorName;

    @Schema(description = "总金额（仅用于展示）", example = "1000.00")
    private BigDecimal totalAmount;

}

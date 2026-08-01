package cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - WMS 计费合同保存 Request VO")
@Data
public class WmsBillingContractSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "合同号", requiredMode = Schema.RequiredMode.REQUIRED, example = "HT202605110001")
    @NotBlank(message = "合同号不能为空")
    @Size(max = 64, message = "合同号长度不能超过 64 个字符")
    private String contractNo;

    @Schema(description = "货主编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "货主不能为空")
    private Long ownerId;

    @Schema(description = "合同名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某货主仓储合同")
    @NotBlank(message = "合同名称不能为空")
    @Size(max = 255, message = "合同名称长度不能超过 255 个字符")
    private String contractName;

    @Schema(description = "生效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "生效日期不能为空")
    private LocalDate startDate;

    @Schema(description = "失效日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "失效日期不能为空")
    private LocalDate endDate;

    @Schema(description = "合同状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

    @Schema(description = "计费条款")
    @Valid
    private List<WmsBillingContractItemSaveReqVO> items;

}

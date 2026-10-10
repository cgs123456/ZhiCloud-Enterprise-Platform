package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 辅助核算项目 创建/修改 Request VO")
@Data
public class FmsAuxiliaryItemSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "辅助核算类别编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "辅助核算类别编号不能为空")
    private Long auxiliaryTypeId;

    @Schema(description = "项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目编码不能为空")
    private String code;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "项目名称不能为空")
    private String name;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "单位")
    private String unit;

}

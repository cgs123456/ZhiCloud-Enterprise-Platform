package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 辅助核算类别 创建/修改 Request VO")
@Data
public class FmsAuxiliaryTypeSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "类别名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类别名称不能为空")
    private String name;

    @Schema(description = "辅助核算类型")
    private Integer type;

    @Schema(description = "是否系统预置")
    private Boolean systemPreset;

}

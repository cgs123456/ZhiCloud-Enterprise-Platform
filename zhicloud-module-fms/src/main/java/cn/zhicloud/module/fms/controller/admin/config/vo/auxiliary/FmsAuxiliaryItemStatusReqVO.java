package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 辅助核算项目状态修改 Request VO")
@Data
public class FmsAuxiliaryItemStatusReqVO {

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

}

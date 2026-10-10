package cn.zhicloud.module.fms.controller.admin.config.vo.voucherword;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 凭证字 创建/修改 Request VO")
@Data
public class FmsVoucherWordSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "凭证字", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "凭证字不能为空")
    private String name;

    @Schema(description = "打印标题")
    private String printTitle;

    @Schema(description = "是否默认凭证字")
    private Boolean defaultStatus;

    @Schema(description = "显示顺序")
    private Integer sort;

}

package cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 凭证模板分类 创建/修改 Request VO")
@Data
public class FmsVoucherTemplateCategorySaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类名称不能为空")
    private String name;

}

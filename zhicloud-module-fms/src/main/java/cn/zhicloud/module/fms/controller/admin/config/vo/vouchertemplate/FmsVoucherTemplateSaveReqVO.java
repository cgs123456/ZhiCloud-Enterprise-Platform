package cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate;

import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 凭证模板 创建/修改 Request VO")
@Data
public class FmsVoucherTemplateSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @Schema(description = "分类编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分类编号不能为空")
    private Long categoryId;

    @Schema(description = "凭证模板分录数组", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "凭证模板分录数组不能为空")
    private List<Map<String, Object>> entries;

}

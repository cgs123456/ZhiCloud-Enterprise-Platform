package cn.zhicloud.module.fms.controller.admin.config.vo.digest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - FMS 常用摘要 创建/修改 Request VO")
@Data
public class FmsDigestSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账套编号不能为空")
    private Long accountSetId;

    @Schema(description = "摘要内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "摘要内容不能为空")
    private String content;

}

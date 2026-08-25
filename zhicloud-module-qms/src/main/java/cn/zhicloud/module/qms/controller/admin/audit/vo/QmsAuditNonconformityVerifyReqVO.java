package cn.zhicloud.module.qms.controller.admin.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 不符合项验证 Request VO")
@Data
public class QmsAuditNonconformityVerifyReqVO {

    @Schema(description = "不符合项 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "不符合项 ID 不能为空")
    private Long id;

    @Schema(description = "验证结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "整改有效，问题已关闭")
    @NotEmpty(message = "验证结果不能为空")
    private String result;

}

package cn.zhicloud.module.qms.controller.admin.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 不符合项整改 Request VO")
@Data
public class QmsAuditNonconformityRectifyReqVO {

    @Schema(description = "不符合项 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "不符合项 ID 不能为空")
    private Long id;

    @Schema(description = "整改措施", requiredMode = Schema.RequiredMode.REQUIRED, example = "已对相关人员进行培训并修改作业指导书")
    @NotEmpty(message = "整改措施不能为空")
    private String action;

}

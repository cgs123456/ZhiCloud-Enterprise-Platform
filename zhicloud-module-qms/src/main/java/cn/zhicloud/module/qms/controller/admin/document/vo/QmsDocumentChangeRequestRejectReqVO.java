package cn.zhicloud.module.qms.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 文件变更申请驳回 Request VO")
@Data
public class QmsDocumentChangeRequestRejectReqVO {

    @Schema(description = "变更申请 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "变更申请 ID 不能为空")
    private Long id;

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "变更理由不充分")
    @NotNull(message = "驳回原因不能为空")
    private String reason;

}

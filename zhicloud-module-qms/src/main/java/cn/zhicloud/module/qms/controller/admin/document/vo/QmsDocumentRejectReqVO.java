package cn.zhicloud.module.qms.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 受控文档审核驳回 Request VO")
@Data
public class QmsDocumentRejectReqVO {

    @Schema(description = "受控文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "受控文档 ID 不能为空")
    private Long id;

    @Schema(description = "驳回原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "文件内容需补充")
    @NotNull(message = "驳回原因不能为空")
    private String reason;

}

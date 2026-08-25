package cn.zhicloud.module.oa.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - OA 公文附件 VO")
@Data
public class OaDocumentAttachmentVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "公文 ID", example = "2048")
    private Long documentId;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "通知.pdf")
    @NotEmpty(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "文件地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "https://www.example.com/通知.pdf")
    @NotEmpty(message = "文件地址不能为空")
    private String fileUrl;

    @Schema(description = "文件大小（字节）", example = "1024")
    private Long fileSize;

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

}

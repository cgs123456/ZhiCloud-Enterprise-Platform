package cn.zhicloud.module.airag.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI RAG 文档上传 Request VO")
@Data
public class AiragDocumentUploadReqVO {

    @Schema(description = "知识库编号", required = true, example = "1")
    @NotNull(message = "知识库编号不能为空")
    private Long knowledgeId;

    @Schema(description = "文档名称", required = true, example = "Java 开发手册.pdf")
    @NotBlank(message = "文档名称不能为空")
    private String name;

    @Schema(description = "文档类型（pdf/docx/txt/md）", example = "pdf")
    private String type;

    @Schema(description = "文件 URL", required = true, example = "https://doc.zhicloud.cn/pdf/java.pdf")
    @NotBlank(message = "文件 URL 不能为空")
    private String url;

}

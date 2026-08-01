package cn.iocoder.yudao.module.airag.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI RAG 文档 Response VO")
@Data
public class AiragDocumentRespVO {

    @Schema(description = "文档编号", required = true, example = "2048")
    private Long id;

    @Schema(description = "知识库编号", required = true, example = "1")
    private Long knowledgeId;

    @Schema(description = "文档名称", required = true, example = "Java 开发手册")
    private String name;

    @Schema(description = "文档类型", example = "pdf")
    private String type;

    @Schema(description = "文件 URL", required = true, example = "https://doc.iocoder.cn/pdf/java.pdf")
    private String url;

    @Schema(description = "处理状态（0待处理 1处理中 2已完成 3失败）", required = true, example = "2")
    private Integer status;

    @Schema(description = "分块数量", example = "12")
    private Integer chunkCount;

    @Schema(description = "错误信息", example = "")
    private String errorMsg;

    @Schema(description = "创建时间", required = true)
    private LocalDateTime createTime;

}

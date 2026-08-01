package cn.iocoder.yudao.module.airag.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - AI RAG 知识库 Response VO")
@Data
public class AiragKnowledgeRespVO {

    @Schema(description = "知识库编号", required = true, example = "1024")
    private Long id;

    @Schema(description = "知识库名称", required = true, example = "ruoyi-vue-pro 用户指南")
    private String name;

    @Schema(description = "知识库描述", example = "存储 ruoyi-vue-pro 操作文档")
    private String description;

    @Schema(description = "Embedding 模型标识", required = true, example = "bge-base-zh")
    private String embeddingModel;

    @Schema(description = "向量维度", required = true, example = "768")
    private Integer vectorDimension;

    @Schema(description = "状态（0开启 1停用）", required = true, example = "0")
    private Integer status;

    @Schema(description = "创建时间", required = true)
    private LocalDateTime createTime;

}

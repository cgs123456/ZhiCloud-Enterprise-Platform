package cn.zhicloud.module.airag.controller.admin.knowledge.vo;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI RAG 知识库新增/修改 Request VO")
@Data
public class AiragKnowledgeSaveReqVO {

    @Schema(description = "知识库编号", example = "1024")
    private Long id;

    @Schema(description = "知识库名称", required = true, example = "ruoyi-vue-pro 用户指南")
    @NotBlank(message = "知识库名称不能为空")
    private String name;

    @Schema(description = "知识库描述", example = "存储 ruoyi-vue-pro 操作文档")
    private String description;

    @Schema(description = "Embedding 模型标识", required = true, example = "bge-base-zh")
    @NotBlank(message = "Embedding 模型不能为空")
    private String embeddingModel;

    @Schema(description = "向量维度", required = true, example = "768")
    @NotNull(message = "向量维度不能为空")
    private Integer vectorDimension;

    @Schema(description = "状态（0开启 1停用）", required = true, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}

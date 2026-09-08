package cn.zhicloud.module.airag.controller.admin.eval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - RAG 评估数据集创建 Request VO")
@Data
public class RagEvalDatasetSaveReqVO {

    @Schema(description = "数据集名称", required = true, example = "售后问答基线 v1")
    @NotEmpty(message = "数据集名称不能为空")
    private String name;

    @Schema(description = "数据集描述", example = "覆盖退货/保修/安装三类问题")
    private String description;

    @Schema(description = "关联知识库编号", required = true, example = "1")
    @NotNull(message = "关联知识库不能为空")
    private Long knowledgeId;

}

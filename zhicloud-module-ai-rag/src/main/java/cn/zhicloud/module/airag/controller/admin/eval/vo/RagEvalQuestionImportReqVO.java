package cn.zhicloud.module.airag.controller.admin.eval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - RAG 评估问题批量导入 Request VO")
@Data
public class RagEvalQuestionImportReqVO {

    @Schema(description = "数据集编号", required = true, example = "1")
    @NotNull(message = "数据集编号不能为空")
    private Long datasetId;

    @Schema(description = "问题列表", required = true)
    @NotEmpty(message = "问题列表不能为空")
    @Valid
    private List<QuestionItem> questions;

    @Schema(description = "评估问题项")
    @Data
    public static class QuestionItem {

        @Schema(description = "问题", required = true, example = "如何申请退货？")
        @NotEmpty(message = "问题不能为空")
        private String question;

        @Schema(description = "标准答案", required = true, example = "签收 7 天内可申请退货…")
        @NotEmpty(message = "标准答案不能为空")
        private String groundTruth;

        @Schema(description = "期望关键词（逗号分隔）", example = "退货,7天,签收")
        private String expectedKeywords;

        @Schema(description = "难度（easy/medium/hard）", example = "medium")
        private String difficulty;
    }

}

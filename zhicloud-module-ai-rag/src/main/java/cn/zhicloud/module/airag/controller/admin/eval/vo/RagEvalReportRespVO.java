package cn.zhicloud.module.airag.controller.admin.eval.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - RAG 批量评估报告 Response VO")
@Data
public class RagEvalReportRespVO {

    @Schema(description = "编号", required = true, example = "1")
    private Long id;

    @Schema(description = "关联数据集", required = true, example = "1")
    private Long datasetId;

    @Schema(description = "状态（0执行中 1已完成 2失败）", required = true, example = "1")
    private Integer status;

    @Schema(description = "题目总数", required = true, example = "50")
    private Integer totalCount;

    @Schema(description = "已完成数", required = true, example = "50")
    private Integer doneCount;

    @Schema(description = "忠实度均值", example = "0.86")
    private Double avgFaithfulness;

    @Schema(description = "回答相关性均值", example = "0.91")
    private Double avgAnswerRelevancy;

    @Schema(description = "上下文精确率均值", example = "0.78")
    private Double avgContextPrecision;

    @Schema(description = "上下文召回率均值", example = "0.82")
    private Double avgContextRecall;

    @Schema(description = "综合得分", example = "0.84")
    private Double overallScore;

    @Schema(description = "与上次评估的分差", example = "-0.02")
    private Double overallDelta;

    @Schema(description = "退步问题 JSON", example = "[12, 30]")
    private String regressedQuestions;

    @Schema(description = "进步问题 JSON", example = "[7]")
    private String improvedQuestions;

    @Schema(description = "单题结果 JSON", example = "[]")
    private String resultsJson;

    @Schema(description = "失败原因", example = "")
    private String errorMsg;

    @Schema(description = "创建时间", required = true, example = "时间戳格式")
    private LocalDateTime createTime;

}

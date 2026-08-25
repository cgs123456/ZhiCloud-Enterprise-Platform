package cn.zhicloud.module.airag.evaluation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * RAG 评估结果
 *
 * <p>包含 4 项核心 RAG 指标 + 综合得分：
 * <ul>
 *   <li>{@link #faithfulness}：忠实度，回答是否基于检索到的文档（0-1）</li>
 *   <li>{@link #answerRelevancy}：回答相关性，回答是否切题（0-1）</li>
 *   <li>{@link #contextPrecision}：上下文精确率，检索到的文档是否相关（0-1）</li>
 *   <li>{@link #contextRecall}：上下文召回率，是否检索到所有相关文档（0-1）</li>
 *   <li>{@link #overallScore}：综合得分（4 项指标的算术平均，0-1）</li>
 * </ul>
 *
 * @author zhicloud
 */
@Schema(description = "RAG 评估结果")
@Data
@Accessors(chain = true)
public class RagEvaluationResult {

    @Schema(description = "忠实度（0-1），回答是否基于检索到的文档", example = "0.85")
    private Double faithfulness;

    @Schema(description = "回答相关性（0-1），回答是否切题", example = "0.90")
    private Double answerRelevancy;

    @Schema(description = "上下文精确率（0-1），检索到的文档是否相关", example = "0.75")
    private Double contextPrecision;

    @Schema(description = "上下文召回率（0-1），是否检索到所有相关文档", example = "0.80")
    private Double contextRecall;

    @Schema(description = "综合得分（4 项指标的算术平均，0-1）", example = "0.825")
    private Double overallScore;

    @Schema(description = "评估详情（LLM 原始反馈，便于人工 review）")
    private String detail;

}

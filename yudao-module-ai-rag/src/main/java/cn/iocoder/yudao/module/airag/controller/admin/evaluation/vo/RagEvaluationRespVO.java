package cn.iocoder.yudao.module.airag.controller.admin.evaluation.vo;

import cn.iocoder.yudao.module.airag.evaluation.RagEvaluationResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * RAG 评估 Response VO（继承评估结果，附加问题与回答摘要）
 */
@Schema(description = "管理后台 - RAG 评估 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class RagEvaluationRespVO extends RagEvaluationResult {

    @Schema(description = "用户问题", example = "如何配置多租户？")
    private String question;

    @Schema(description = "RAG 生成的回答摘要", example = "多租户通过 tenant_id 字段隔离...")
    private String answerSummary;

}

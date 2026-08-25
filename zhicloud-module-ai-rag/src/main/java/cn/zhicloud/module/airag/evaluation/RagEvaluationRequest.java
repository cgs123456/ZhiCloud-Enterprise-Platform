package cn.zhicloud.module.airag.evaluation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * RAG 评估请求
 *
 * <p>包含一次 RAG 问答所需的全部输入：问题、回答、检索上下文、标准答案（可选）。
 *
 * @author zhicloud
 */
@Schema(description = "RAG 评估请求")
@Data
public class RagEvaluationRequest {

    @Schema(description = "用户问题", required = true, example = "如何配置多租户？")
    @NotBlank(message = "问题不能为空")
    private String question;

    @Schema(description = "RAG 生成的回答", required = true, example = "多租户通过 tenant_id 字段隔离...")
    @NotBlank(message = "回答不能为空")
    private String answer;

    @Schema(description = "检索到的上下文文档列表", required = true)
    private List<String> contexts;

    @Schema(description = "标准答案（ground truth，用于上下文召回率评估，可选）")
    private String groundTruth;

}

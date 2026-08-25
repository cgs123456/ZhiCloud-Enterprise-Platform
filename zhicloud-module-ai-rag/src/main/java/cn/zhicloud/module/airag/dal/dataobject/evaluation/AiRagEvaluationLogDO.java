package cn.zhicloud.module.airag.dal.dataobject.evaluation;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * AI RAG 评估日志 DO
 *
 * <p>记录每次 RAG 评估的输入摘要与 4 项指标得分，用于追踪 RAG 质量变化趋势。
 *
 * @author zhicloud
 */
@TableName(value = "ai_rag_evaluation_log", autoResultMap = true)
@KeySequence("ai_rag_evaluation_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiRagEvaluationLogDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 用户问题
     */
    private String question;
    /**
     * RAG 生成的回答
     */
    private String answer;
    /**
     * 检索到的上下文数量
     */
    private Integer contextCount;
    /**
     * 标准答案是否提供
     */
    private Boolean hasGroundTruth;
    /**
     * 忠实度（0-1）
     */
    private Double faithfulness;
    /**
     * 回答相关性（0-1）
     */
    private Double answerRelevancy;
    /**
     * 上下文精确率（0-1）
     */
    private Double contextPrecision;
    /**
     * 上下文召回率（0-1）
     */
    private Double contextRecall;
    /**
     * 综合得分（0-1）
     */
    private Double overallScore;
    /**
     * 评估详情（LLM 原始反馈摘要）
     */
    private String detail;

}

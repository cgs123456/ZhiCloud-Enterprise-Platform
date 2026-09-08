package cn.zhicloud.module.airag.dal.dataobject.eval;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * RAG 批量评估报告 DO
 *
 * @author zhicloud
 */
@TableName(value = "airag_eval_report", autoResultMap = true)
@KeySequence("airag_eval_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiragEvalReportDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 关联数据集
     */
    private Long datasetId;
    /**
     * 评估时的知识库（冗余）
     */
    private Long knowledgeId;
    /**
     * 状态（0执行中 1已完成 2失败）
     */
    private Integer status;
    /**
     * 题目总数
     */
    private Integer totalCount;
    /**
     * 已完成数（进度轮询用）
     */
    private Integer doneCount;
    /**
     * 忠实度均值
     */
    private Double avgFaithfulness;
    /**
     * 回答相关性均值
     */
    private Double avgAnswerRelevancy;
    /**
     * 上下文精确率均值
     */
    private Double avgContextPrecision;
    /**
     * 上下文召回率均值
     */
    private Double avgContextRecall;
    /**
     * 综合得分（4 指标加权平均）
     */
    private Double overallScore;
    /**
     * 与上次评估的分差
     */
    private Double overallDelta;
    /**
     * 退步问题 JSON（questionId 列表）
     */
    private String regressedQuestions;
    /**
     * 进步问题 JSON（questionId 列表）
     */
    private String improvedQuestions;
    /**
     * 单题结果 JSON（含每题 4 指标）
     */
    private String resultsJson;
    /**
     * 失败原因
     */
    private String errorMsg;

}

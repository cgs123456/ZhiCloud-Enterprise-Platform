package cn.zhicloud.module.airag.dal.dataobject.eval;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * RAG 评估问题 DO
 *
 * @author zhicloud
 */
@TableName(value = "airag_eval_question", autoResultMap = true)
@KeySequence("airag_eval_question_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiragEvalQuestionDO extends BaseDO {

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
     * 问题
     */
    private String question;
    /**
     * 标准答案
     */
    private String groundTruth;
    /**
     * 期望关键词（逗号分隔）
     */
    private String expectedKeywords;
    /**
     * 难度（easy/medium/hard）
     */
    private String difficulty;
    /**
     * 排序
     */
    private Integer sort;

}

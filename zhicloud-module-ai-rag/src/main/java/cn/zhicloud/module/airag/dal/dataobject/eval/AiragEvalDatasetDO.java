package cn.zhicloud.module.airag.dal.dataobject.eval;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * RAG 评估数据集 DO
 *
 * @author zhicloud
 */
@TableName(value = "airag_eval_dataset", autoResultMap = true)
@KeySequence("airag_eval_dataset_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiragEvalDatasetDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 数据集名称
     */
    private String name;
    /**
     * 数据集描述
     */
    private String description;
    /**
     * 关联知识库
     */
    private Long knowledgeId;
    /**
     * 题目数量（冗余）
     */
    private Integer questionCount;
    /**
     * 状态（0编辑中 1已发布 2已归档）
     */
    private Integer status;

}

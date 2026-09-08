package cn.zhicloud.module.airag.dal.mysql.eval;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalQuestionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * RAG 评估问题 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiragEvalQuestionMapper extends BaseMapperX<AiragEvalQuestionDO> {

    /**
     * 按数据集查询题目列表（按 sort 升序）
     *
     * @param datasetId 数据集编号
     * @return 题目列表
     */
    default List<AiragEvalQuestionDO> selectListByDatasetId(Long datasetId) {
        return selectList(new LambdaQueryWrapperX<AiragEvalQuestionDO>()
                .eq(AiragEvalQuestionDO::getDatasetId, datasetId)
                .orderByAsc(AiragEvalQuestionDO::getSort)
                .orderByAsc(AiragEvalQuestionDO::getId));
    }

    /**
     * 统计数据集题目数
     *
     * @param datasetId 数据集编号
     * @return 题目数
     */
    default Long selectCountByDatasetId(Long datasetId) {
        return selectCount(new LambdaQueryWrapperX<AiragEvalQuestionDO>()
                .eq(AiragEvalQuestionDO::getDatasetId, datasetId));
    }

}

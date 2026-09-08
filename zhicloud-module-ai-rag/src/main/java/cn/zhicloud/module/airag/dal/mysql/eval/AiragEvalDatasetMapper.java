package cn.zhicloud.module.airag.dal.mysql.eval;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalDatasetDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * RAG 评估数据集 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiragEvalDatasetMapper extends BaseMapperX<AiragEvalDatasetDO> {

    /**
     * 按知识库查询数据集列表（按创建时间倒序）
     *
     * @param knowledgeId 知识库编号，可为空（查全部）
     * @return 数据集列表
     */
    default List<AiragEvalDatasetDO> selectListByKnowledgeId(Long knowledgeId) {
        LambdaQueryWrapperX<AiragEvalDatasetDO> wrapper = new LambdaQueryWrapperX<AiragEvalDatasetDO>()
                .orderByDesc(AiragEvalDatasetDO::getCreateTime);
        if (knowledgeId != null) {
            wrapper.eq(AiragEvalDatasetDO::getKnowledgeId, knowledgeId);
        }
        return selectList(wrapper);
    }

}

package cn.zhicloud.module.airag.dal.mysql;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.airag.controller.admin.knowledge.vo.AiragKnowledgePageReqVO;
import cn.zhicloud.module.airag.dal.dataobject.AiragKnowledgeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI RAG 知识库 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiragKnowledgeMapper extends BaseMapperX<AiragKnowledgeDO> {

    default PageResult<AiragKnowledgeDO> selectPage(AiragKnowledgePageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<AiragKnowledgeDO>()
                .likeIfPresent(AiragKnowledgeDO::getName, pageReqVO.getName())
                .eqIfPresent(AiragKnowledgeDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(AiragKnowledgeDO::getCreateTime, pageReqVO.getCreateTime())
                .orderByDesc(AiragKnowledgeDO::getId));
    }

    default List<AiragKnowledgeDO> selectListByStatus(Integer status) {
        return selectList(AiragKnowledgeDO::getStatus, status);
    }

}

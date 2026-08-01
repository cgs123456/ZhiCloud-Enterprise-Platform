package cn.iocoder.yudao.module.airag.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.airag.controller.admin.knowledge.vo.AiragKnowledgePageReqVO;
import cn.iocoder.yudao.module.airag.dal.dataobject.AiragKnowledgeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI RAG 知识库 Mapper
 *
 * @author yudao
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

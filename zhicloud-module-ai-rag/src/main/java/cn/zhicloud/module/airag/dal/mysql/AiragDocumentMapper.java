package cn.zhicloud.module.airag.dal.mysql;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.airag.controller.admin.document.vo.AiragDocumentPageReqVO;
import cn.zhicloud.module.airag.dal.dataobject.AiragDocumentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI RAG 文档 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiragDocumentMapper extends BaseMapperX<AiragDocumentDO> {

    default PageResult<AiragDocumentDO> selectPage(AiragDocumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AiragDocumentDO>()
                .eqIfPresent(AiragDocumentDO::getKnowledgeId, reqVO.getKnowledgeId())
                .likeIfPresent(AiragDocumentDO::getName, reqVO.getName())
                .eqIfPresent(AiragDocumentDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AiragDocumentDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AiragDocumentDO::getId));
    }

    default List<AiragDocumentDO> selectListByKnowledgeId(Long knowledgeId) {
        return selectList(AiragDocumentDO::getKnowledgeId, knowledgeId);
    }

}

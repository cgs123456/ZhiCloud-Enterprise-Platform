package cn.iocoder.yudao.module.airag.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.airag.controller.admin.document.vo.AiragDocumentPageReqVO;
import cn.iocoder.yudao.module.airag.dal.dataobject.AiragDocumentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI RAG 文档 Mapper
 *
 * @author yudao
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

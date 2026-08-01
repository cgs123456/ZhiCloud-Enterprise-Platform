package cn.iocoder.yudao.module.oa.dal.mysql.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeCommentPageReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeCommentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OA 知识库评论 Mapper
 *
 * @author yudao
 */
@Mapper
public interface OaKnowledgeCommentMapper extends BaseMapperX<OaKnowledgeCommentDO> {

    default PageResult<OaKnowledgeCommentDO> selectPage(OaKnowledgeCommentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaKnowledgeCommentDO>()
                .eqIfPresent(OaKnowledgeCommentDO::getArticleId, reqVO.getArticleId())
                .eqIfPresent(OaKnowledgeCommentDO::getParentId, reqVO.getParentId())
                .eqIfPresent(OaKnowledgeCommentDO::getStatus, reqVO.getStatus())
                .eqIfPresent(OaKnowledgeCommentDO::getCommentatorUserId, reqVO.getCommentatorUserId())
                .orderByAsc(OaKnowledgeCommentDO::getId));
    }

    default List<OaKnowledgeCommentDO> selectListByArticleId(Long articleId) {
        return selectList(new LambdaQueryWrapperX<OaKnowledgeCommentDO>()
                .eq(OaKnowledgeCommentDO::getArticleId, articleId)
                .orderByAsc(OaKnowledgeCommentDO::getId));
    }

    default Long selectCountByArticleId(Long articleId) {
        return selectCount(OaKnowledgeCommentDO::getArticleId, articleId);
    }

    default void deleteByArticleId(Long articleId) {
        delete(new LambdaQueryWrapperX<OaKnowledgeCommentDO>()
                .eq(OaKnowledgeCommentDO::getArticleId, articleId));
    }

}

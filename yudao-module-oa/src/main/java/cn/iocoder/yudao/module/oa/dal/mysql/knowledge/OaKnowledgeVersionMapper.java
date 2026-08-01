package cn.iocoder.yudao.module.oa.dal.mysql.knowledge;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionPageReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OA 知识库版本 Mapper
 *
 * @author yudao
 */
@Mapper
public interface OaKnowledgeVersionMapper extends BaseMapperX<OaKnowledgeVersionDO> {

    default PageResult<OaKnowledgeVersionDO> selectPage(OaKnowledgeVersionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<OaKnowledgeVersionDO>()
                .eqIfPresent(OaKnowledgeVersionDO::getArticleId, reqVO.getArticleId())
                .eqIfPresent(OaKnowledgeVersionDO::getEditorUserId, reqVO.getEditorUserId())
                .orderByDesc(OaKnowledgeVersionDO::getVersionNo));
    }

    default List<OaKnowledgeVersionDO> selectListByArticleId(Long articleId) {
        return selectList(new LambdaQueryWrapperX<OaKnowledgeVersionDO>()
                .eq(OaKnowledgeVersionDO::getArticleId, articleId)
                .orderByDesc(OaKnowledgeVersionDO::getVersionNo));
    }

    default OaKnowledgeVersionDO selectByArticleIdAndVersionNo(Long articleId, Integer versionNo) {
        return selectOne(new LambdaQueryWrapperX<OaKnowledgeVersionDO>()
                .eq(OaKnowledgeVersionDO::getArticleId, articleId)
                .eq(OaKnowledgeVersionDO::getVersionNo, versionNo));
    }

    default void deleteByArticleId(Long articleId) {
        delete(new LambdaQueryWrapperX<OaKnowledgeVersionDO>()
                .eq(OaKnowledgeVersionDO::getArticleId, articleId));
    }

}

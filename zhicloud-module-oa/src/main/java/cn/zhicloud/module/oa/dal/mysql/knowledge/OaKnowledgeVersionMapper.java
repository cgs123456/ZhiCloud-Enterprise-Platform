package cn.zhicloud.module.oa.dal.mysql.knowledge;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.oa.controller.admin.knowledge.vo.OaKnowledgeVersionPageReqVO;
import cn.zhicloud.module.oa.dal.dataobject.knowledge.OaKnowledgeVersionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * OA 知识库版本 Mapper
 *
 * @author zhicloud
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
